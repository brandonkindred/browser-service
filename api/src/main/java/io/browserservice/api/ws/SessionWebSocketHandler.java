package io.browserservice.api.ws;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.browserservice.api.config.EngineProperties;
import io.browserservice.api.dto.ErrorDetail;
import io.browserservice.api.error.CommandQueueFullException;
import io.browserservice.api.error.ErrorMapper;
import io.browserservice.api.error.RequestIdFilter;
import io.browserservice.api.error.ScreenshotTooLargeException;
import io.browserservice.api.error.UnknownFrameTypeException;
import io.browserservice.api.ws.dto.BinaryHeaderFrame;
import io.browserservice.api.ws.dto.CommandFrame;
import io.browserservice.api.ws.dto.ResponseFrame;
import io.browserservice.api.ws.push.WatcherCoordinator;
import java.io.IOException;
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
import org.springframework.stereotype.Component;
import org.springframework.web.socket.BinaryMessage;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.ConcurrentWebSocketSessionDecorator;
import org.springframework.web.socket.handler.TextWebSocketHandler;

@Component
public class SessionWebSocketHandler extends TextWebSocketHandler {

  public static final CloseStatus IDLE_TIMEOUT = new CloseStatus(4408, "idle_timeout");
  public static final CloseStatus SESSION_FORBIDDEN = new CloseStatus(4403, "session_forbidden");

  private static final Logger log = LoggerFactory.getLogger(SessionWebSocketHandler.class);

  private final CommandDispatcher dispatcher;
  private final ObjectMapper mapper;
  private final ScheduledExecutorService scheduler;
  private final WatcherCoordinator watchers;
  private final EngineProperties.WebSocketProps props;

  public SessionWebSocketHandler(
      CommandDispatcher dispatcher,
      ObjectMapper mapper,
      ScheduledExecutorService webSocketScheduler,
      WatcherCoordinator watchers,
      EngineProperties props) {
    this.dispatcher = dispatcher;
    this.mapper = mapper;
    this.scheduler = webSocketScheduler;
    this.watchers = watchers;
    this.props = props.webSocket();
  }

  @Override
  public void afterConnectionEstablished(WebSocketSession session) {
    CallerId caller =
        (CallerId) session.getAttributes().get(CallerIdHandshakeInterceptor.CALLER_ATTRIBUTE);
    String connectionId =
        (String) session.getAttributes().get(CallerIdHandshakeInterceptor.CONNECTION_ID_ATTRIBUTE);
    if (caller == null || connectionId == null) {
      safeClose(session, new CloseStatus(4401, "caller_unidentified"));
      return;
    }
    ConcurrentWebSocketSessionDecorator out =
        new ConcurrentWebSocketSessionDecorator(
            session, props.sendTimeLimitMs(), props.outboundBufferKiB() * 1024);
    ThreadFactory tf = namedThreadFactory("ws-cmd-" + connectionId);
    Connection conn =
        new Connection(
            caller,
            connectionId,
            out,
            Executors.newSingleThreadExecutor(tf),
            new Semaphore(props.commandQueueDepth()));
    session.getAttributes().put(Connection.ATTRIBUTE, conn);

    long idleNanos = TimeUnit.SECONDS.toNanos(props.idleCloseSeconds());
    ScheduledFuture<?> watchdog =
        scheduler.scheduleAtFixedRate(
            () -> {
              if (!session.isOpen()) return;
              if (System.nanoTime() - conn.lastActivityNanos() > idleNanos) {
                safeClose(session, IDLE_TIMEOUT);
              }
            },
            1,
            1,
            TimeUnit.SECONDS);
    session.getAttributes().put("ws.watchdog", watchdog);

    log.debug("ws established connectionId={} caller={}", connectionId, caller);
  }

  @Override
  protected void handleTextMessage(WebSocketSession session, TextMessage message) {
    Connection conn = (Connection) session.getAttributes().get(Connection.ATTRIBUTE);
    if (conn == null) {
      safeClose(session, CloseStatus.SERVER_ERROR);
      return;
    }
    conn.touchActivity();

    if (!conn.queue().tryAcquire()) {
      CommandFrame parsed = tryParse(message.getPayload());
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
                dispatch(conn, message.getPayload());
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
    } catch (CommandDispatcher.OwnershipMismatchException ownership) {
      log.info(
          "ws ownership mismatch caller={} sessionId={}", conn.caller(), ownership.sessionId());
      safeClose(conn.out(), SESSION_FORBIDDEN);
    } catch (Throwable t) {
      ErrorMapper.Mapped m = ErrorMapper.map(t, requestId);
      String cmdId = frame == null ? null : frame.id();
      writeFrame(conn, ResponseFrame.failure(cmdId, m.body()));
    } finally {
      MDC.remove(RequestIdFilter.MDC_KEY);
    }
  }

  @Override
  public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
    Object watchdog = session.getAttributes().remove("ws.watchdog");
    if (watchdog instanceof ScheduledFuture<?> sf) {
      sf.cancel(false);
    }
    Connection conn = (Connection) session.getAttributes().remove(Connection.ATTRIBUTE);
    if (conn != null) {
      UUID bound = conn.boundSessionId();
      if (bound != null) {
        watchers.onSessionDetached(bound, conn);
      }
      conn.commands().shutdownNow();
      log.debug("ws closed connectionId={} status={}", conn.connectionId(), status);
    }
  }

  @Override
  public void handleTransportError(WebSocketSession session, Throwable exception) {
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
        conn.out().sendMessage(new TextMessage(json));
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
        conn.out().sendMessage(new TextMessage(headerJson));
        conn.out().sendMessage(new BinaryMessage(bytes));
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

  private static void safeClose(WebSocketSession session, CloseStatus status) {
    try {
      if (session.isOpen()) {
        session.close(status);
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
