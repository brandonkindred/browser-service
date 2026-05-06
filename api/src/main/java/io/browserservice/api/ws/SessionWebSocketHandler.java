package io.browserservice.api.ws;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.browserservice.api.config.EngineProperties;
import io.browserservice.api.dto.ErrorDetail;
import io.browserservice.api.error.CommandQueueFullException;
import io.browserservice.api.error.ErrorMapper;
import io.browserservice.api.error.RequestIdFilter;
import io.browserservice.api.error.ScreenshotTooLargeException;
import io.browserservice.api.error.SessionForbiddenException;
import io.browserservice.api.error.UnknownFrameTypeException;
import io.browserservice.api.session.CallerId;
import io.browserservice.api.ws.dto.BinaryHeaderFrame;
import io.browserservice.api.ws.dto.CommandFrame;
import io.browserservice.api.ws.dto.ResponseFrame;
import io.browserservice.api.ws.push.WatcherCoordinator;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.websocket.CloseReason;
import jakarta.websocket.OnClose;
import jakarta.websocket.OnError;
import jakarta.websocket.OnMessage;
import jakarta.websocket.OnOpen;
import jakarta.websocket.Session;
import jakarta.websocket.server.ServerEndpoint;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.Semaphore;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

@ApplicationScoped
@ServerEndpoint(value = "/v1/ws/sessions", configurator = CallerIdHandshakeInterceptor.class)
public class SessionWebSocketHandler {

  public static final CloseReason.CloseCode CALLER_UNIDENTIFIED = () -> 4401;
  public static final CloseReason.CloseCode SESSION_FORBIDDEN_CODE = () -> 4403;
  public static final CloseReason.CloseCode IDLE_TIMEOUT_CODE = () -> 4408;

  private static final Logger log = LoggerFactory.getLogger(SessionWebSocketHandler.class);

  private final CommandDispatcher dispatcher;
  private final ObjectMapper mapper;
  private final ScheduledExecutorService scheduler;
  private final WatcherCoordinator watchers;
  private final EngineProperties.WebSocketProps props;

  @Inject
  public SessionWebSocketHandler(
      CommandDispatcher dispatcher,
      ObjectMapper mapper,
      @Named("webSocketScheduler") ScheduledExecutorService webSocketScheduler,
      WatcherCoordinator watchers,
      EngineProperties props) {
    this.dispatcher = dispatcher;
    this.mapper = mapper;
    this.scheduler = webSocketScheduler;
    this.watchers = watchers;
    this.props = props.webSocket();
  }

  @OnOpen
  public void onOpen(Session session) {
    String rawCaller =
        (String)
            session
                .getUserProperties()
                .get(CallerIdHandshakeInterceptor.CALLER_HEADER_RAW_ATTRIBUTE);
    String connectionId =
        (String)
            session.getUserProperties().get(CallerIdHandshakeInterceptor.CONNECTION_ID_ATTRIBUTE);
    CallerId caller;
    try {
      caller = CallerId.parse(rawCaller);
    } catch (IllegalArgumentException e) {
      log.debug("rejecting WS handshake: {}", e.getMessage());
      safeClose(session, new CloseReason(CALLER_UNIDENTIFIED, "caller_unidentified"));
      return;
    }
    if (connectionId == null) {
      safeClose(session, new CloseReason(CALLER_UNIDENTIFIED, "caller_unidentified"));
      return;
    }
    session.getUserProperties().put(CallerIdHandshakeInterceptor.CALLER_ATTRIBUTE, caller);

    ThreadFactory tf = namedThreadFactory("ws-cmd-" + connectionId);
    Connection conn =
        new Connection(
            caller,
            connectionId,
            session,
            Executors.newSingleThreadExecutor(tf),
            new Semaphore(props.commandQueueDepth()));
    session.getUserProperties().put(Connection.ATTRIBUTE, conn);

    long idleNanos = TimeUnit.SECONDS.toNanos(props.idleCloseSeconds());
    ScheduledFuture<?> watchdog =
        scheduler.scheduleAtFixedRate(
            () -> {
              if (!session.isOpen()) return;
              if (System.nanoTime() - conn.lastActivityNanos() > idleNanos) {
                safeClose(session, new CloseReason(IDLE_TIMEOUT_CODE, "idle_timeout"));
              }
            },
            1,
            1,
            TimeUnit.SECONDS);
    session.getUserProperties().put("ws.watchdog", watchdog);

    log.debug("ws established connectionId={} caller={}", connectionId, caller);
  }

  @OnMessage
  public void onMessage(String payload, Session session) {
    Connection conn = (Connection) session.getUserProperties().get(Connection.ATTRIBUTE);
    if (conn == null) {
      safeClose(session, new CloseReason(CloseReason.CloseCodes.UNEXPECTED_CONDITION, "no_state"));
      return;
    }
    conn.touchActivity();

    if (!conn.queue().tryAcquire()) {
      CommandFrame parsed = tryParse(payload);
      String cmdId = parsed == null ? null : parsed.id();
      String requestId = UUID.randomUUID().toString();
      ErrorMapper.Mapped mapped =
          ErrorMapper.map(new CommandQueueFullException(props.commandQueueDepth()), requestId);
      writeFrame(conn, ResponseFrame.failure(cmdId, mapped.body()));
      return;
    }

    conn.commands()
        .submit(
            () -> {
              try {
                dispatch(conn, payload);
              } finally {
                conn.queue().release();
              }
            });
  }

  private void dispatch(Connection conn, String payload) {
    String requestId = UUID.randomUUID().toString();
    MDC.put(RequestIdFilter.MDC_KEY, requestId);
    CommandFrame frame = null;
    try {
      try {
        frame = mapper.readValue(payload, CommandFrame.class);
      } catch (Exception e) {
        ErrorDetail err =
            new ErrorDetail(
                "validation_failed",
                "malformed command frame: " + ErrorMapper.safeMessage(e),
                null,
                requestId);
        writeFrame(conn, ResponseFrame.failure(null, err));
        return;
      }
      if (frame.type() != null && !CommandFrame.TYPE.equals(frame.type())) {
        ErrorMapper.Mapped m =
            ErrorMapper.map(new UnknownFrameTypeException(frame.type()), requestId);
        writeFrame(conn, ResponseFrame.failure(frame.id(), m.body()));
        return;
      }
      DispatchResult result = dispatcher.dispatch(conn, frame.op(), frame.params());
      switch (result) {
        case DispatchResult.Json json ->
            writeFrame(conn, ResponseFrame.success(frame.id(), json.value()));
        case DispatchResult.Binary bin -> writeBinaryPair(conn, frame.id(), bin, requestId);
      }
    } catch (SessionForbiddenException forbidden) {
      log.info(
          "ws ownership mismatch caller={} sessionId={}", conn.caller(), forbidden.sessionId());
      safeClose(conn.out(), new CloseReason(SESSION_FORBIDDEN_CODE, "session_forbidden"));
    } catch (Throwable t) {
      ErrorMapper.Mapped m = ErrorMapper.map(t, requestId);
      String cmdId = frame == null ? null : frame.id();
      writeFrame(conn, ResponseFrame.failure(cmdId, m.body()));
    } finally {
      MDC.remove(RequestIdFilter.MDC_KEY);
    }
  }

  @OnClose
  public void onClose(Session session, CloseReason reason) {
    Object watchdog = session.getUserProperties().remove("ws.watchdog");
    if (watchdog instanceof ScheduledFuture<?> sf) {
      sf.cancel(false);
    }
    Connection conn = (Connection) session.getUserProperties().remove(Connection.ATTRIBUTE);
    if (conn != null) {
      UUID bound = conn.boundSessionId();
      if (bound != null) {
        watchers.onSessionDetached(bound, conn);
      }
      conn.commands().shutdownNow();
      log.debug("ws closed connectionId={} reason={}", conn.connectionId(), reason);
    }
  }

  @OnError
  public void onError(Session session, Throwable exception) {
    log.warn("ws transport error: {}", exception.toString());
  }

  private CommandFrame tryParse(String payload) {
    try {
      return mapper.readValue(payload, CommandFrame.class);
    } catch (Exception e) {
      return null;
    }
  }

  private void writeFrame(Connection conn, ResponseFrame frame) {
    try {
      String json = mapper.writeValueAsString(frame);
      synchronized (conn.writeLock()) {
        conn.out().getBasicRemote().sendText(json);
      }
    } catch (IOException e) {
      log.warn("ws write failed connectionId={}: {}", conn.connectionId(), e.toString());
    }
  }

  private void writeBinaryPair(
      Connection conn, String commandId, DispatchResult.Binary bin, String requestId) {
    byte[] bytes = bin.bytes();
    int limit = props.maxBinaryFrameBytes();
    if (bytes.length > limit) {
      ErrorMapper.Mapped m =
          ErrorMapper.map(new ScreenshotTooLargeException(bytes.length, limit), requestId);
      writeFrame(conn, ResponseFrame.failure(commandId, m.body()));
      return;
    }
    BinaryHeaderFrame header =
        BinaryHeaderFrame.of(commandId, bin.mime(), bytes.length, sha256Hex(bytes));
    String headerJson;
    try {
      headerJson = mapper.writeValueAsString(header);
    } catch (Exception e) {
      log.warn(
          "ws binary header serialize failed connectionId={}: {}",
          conn.connectionId(),
          e.toString());
      return;
    }
    // Atomic pair: header text frame + binary frame must arrive adjacent on the wire,
    // never interleaved with watcher events or another command response.
    synchronized (conn.writeLock()) {
      try {
        conn.out().getBasicRemote().sendText(headerJson);
        conn.out().getBasicRemote().sendBinary(ByteBuffer.wrap(bytes));
      } catch (IOException e) {
        log.warn("ws binary write failed connectionId={}: {}", conn.connectionId(), e.toString());
      }
    }
  }

  private static String sha256Hex(byte[] bytes) {
    try {
      byte[] digest = MessageDigest.getInstance("SHA-256").digest(bytes);
      StringBuilder sb = new StringBuilder(digest.length * 2);
      for (byte b : digest) {
        sb.append(Character.forDigit((b >> 4) & 0xF, 16));
        sb.append(Character.forDigit(b & 0xF, 16));
      }
      return sb.toString();
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException("SHA-256 not available", e);
    }
  }

  private static void safeClose(Session session, CloseReason reason) {
    try {
      if (session.isOpen()) {
        session.close(reason);
      }
    } catch (IOException e) {
      log.debug("ws close failed: {}", e.toString());
    }
  }

  private static ThreadFactory namedThreadFactory(String name) {
    AtomicInteger counter = new AtomicInteger();
    return r -> {
      Thread t = new Thread(r, name + "-" + counter.incrementAndGet());
      t.setDaemon(true);
      return t;
    };
  }
}
