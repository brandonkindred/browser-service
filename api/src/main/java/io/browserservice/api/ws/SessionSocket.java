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
import io.quarkus.websockets.next.CloseReason;
import io.quarkus.websockets.next.OnClose;
import io.quarkus.websockets.next.OnError;
import io.quarkus.websockets.next.OnOpen;
import io.quarkus.websockets.next.OnTextMessage;
import io.quarkus.websockets.next.OpenConnections;
import io.quarkus.websockets.next.WebSocket;
import io.quarkus.websockets.next.WebSocketConnection;
import io.smallrye.common.annotation.Blocking;
import io.smallrye.jwt.auth.principal.JWTParser;
import io.smallrye.jwt.auth.principal.ParseException;
import io.vertx.core.buffer.Buffer;
import jakarta.enterprise.context.SessionScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
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
import org.eclipse.microprofile.jwt.JsonWebToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

/**
 * Quarkus {@code @WebSocket} endpoint at {@code /v1/ws/sessions}. One bean instance per WS
 * connection ({@link SessionScoped}); state for the connection lives on this bean and inside the
 * {@link WsConnectionState} POJO held by it.
 *
 * <p>Handshake authentication runs in {@link CallerIdUpgradeCheck} (HTTP 401 on missing or invalid
 * JWT in the {@code Sec-WebSocket-Protocol} subprotocol). A token that parses but lacks the
 * required {@code sub} / {@code tenant_id} claims is rejected here in {@link #onOpen} with WS close
 * code 4401 — should be unreachable in practice since the upgrade check enforces both.
 */
@WebSocket(path = "/v1/ws/sessions")
@SessionScoped
public class SessionSocket {

  public static final int CALLER_UNIDENTIFIED = 4401;
  public static final int SESSION_FORBIDDEN_CODE = 4403;
  public static final int IDLE_TIMEOUT_CODE = 4408;

  private static final Logger log = LoggerFactory.getLogger(SessionSocket.class);

  private final CommandDispatcher dispatcher;
  private final ObjectMapper mapper;
  private final ScheduledExecutorService scheduler;
  private final WatcherCoordinator watchers;
  private final EngineProperties.WebSocketProps props;
  private final WebSocketConnection connection;
  private final OpenConnections openConnections;
  private final JWTParser jwtParser;

  private WsConnectionState state;
  private ScheduledFuture<?> watchdog;

  @Inject
  public SessionSocket(
      CommandDispatcher dispatcher,
      ObjectMapper mapper,
      @Named("webSocketScheduler") ScheduledExecutorService webSocketScheduler,
      WatcherCoordinator watchers,
      EngineProperties props,
      WebSocketConnection connection,
      OpenConnections openConnections,
      JWTParser jwtParser) {
    this.dispatcher = dispatcher;
    this.mapper = mapper;
    this.scheduler = webSocketScheduler;
    this.watchers = watchers;
    this.props = props.webSocket();
    this.connection = connection;
    this.openConnections = openConnections;
    this.jwtParser = jwtParser;
  }

  @OnOpen
  public void onOpen() {
    String rawSubprotocols =
        connection.handshakeRequest().header(CallerIdUpgradeCheck.SUBPROTOCOL_HEADER);
    String token = CallerIdUpgradeCheck.extractBearerToken(rawSubprotocols);
    CallerId caller;
    try {
      JsonWebToken jwt = jwtParser.parse(token);
      Object tenantClaim = jwt.getClaim("tenant_id");
      if (!(tenantClaim instanceof String tenant)) {
        throw new IllegalArgumentException("tenant_id claim is not a string");
      }
      caller = CallerId.of(tenant, jwt.getSubject());
    } catch (ParseException | IllegalArgumentException e) {
      // Should be unreachable: CallerIdUpgradeCheck rejected the upgrade if
      // either the JWT was invalid or the required claims were missing. Treat
      // a reachable failure here as defence-in-depth and close with 4401.
      log.debug("rejecting WS handshake post-upgrade: {}", e.toString());
      safeClose(new CloseReason(CALLER_UNIDENTIFIED, "caller_unidentified"));
      return;
    }

    String connectionId = UUID.randomUUID().toString();
    ThreadFactory tf = namedThreadFactory("ws-cmd-" + connectionId);
    this.state =
        new WsConnectionState(
            caller,
            connectionId,
            connection.id(),
            Executors.newSingleThreadExecutor(tf),
            new Semaphore(props.commandQueueDepth()));

    long idleNanos = TimeUnit.SECONDS.toNanos(props.idleCloseSeconds());
    final WsConnectionState localState = this.state;
    this.watchdog =
        scheduler.scheduleAtFixedRate(
            () -> {
              // Any uncaught throw cancels future ticks (scheduleAtFixedRate contract), so
              // swallow transient failures here — safeCloseLive already try/catches close
              // failures, but defend against an unexpected NPE/RuntimeException on the
              // lookup path so idle enforcement keeps retrying on the next tick.
              try {
                if (System.nanoTime() - localState.lastActivityNanos() > idleNanos) {
                  safeCloseLive(localState, new CloseReason(IDLE_TIMEOUT_CODE, "idle_timeout"));
                }
              } catch (Throwable t) {
                log.debug("idle watchdog tick threw: {}", t.toString());
              }
            },
            1,
            1,
            TimeUnit.SECONDS);

    log.debug("ws established connectionId={} caller={}", connectionId, caller);
  }

  @OnTextMessage
  @Blocking
  public void onTextMessage(String payload) {
    WsConnectionState conn = this.state;
    if (conn == null) {
      safeClose(new CloseReason(1011, "no_state"));
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

  private void dispatch(WsConnectionState conn, String payload) {
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
      safeCloseLive(conn, new CloseReason(SESSION_FORBIDDEN_CODE, "session_forbidden"));
    } catch (Throwable t) {
      ErrorMapper.Mapped m = ErrorMapper.map(t, requestId);
      String cmdId = frame == null ? null : frame.id();
      writeFrame(conn, ResponseFrame.failure(cmdId, m.body()));
    } finally {
      MDC.remove(RequestIdFilter.MDC_KEY);
    }
  }

  @OnClose
  public void onClose() {
    if (watchdog != null) {
      watchdog.cancel(false);
      watchdog = null;
    }
    WsConnectionState conn = this.state;
    this.state = null;
    if (conn != null) {
      UUID bound = conn.boundSessionId();
      if (bound != null) {
        watchers.onSessionDetached(bound, conn);
      }
      conn.commands().shutdownNow();
      log.debug("ws closed connectionId={}", conn.connectionId());
    }
  }

  @OnError
  public void onError(Throwable exception) {
    log.warn("ws transport error: {}", exception.toString());
  }

  private CommandFrame tryParse(String payload) {
    try {
      return mapper.readValue(payload, CommandFrame.class);
    } catch (Exception e) {
      return null;
    }
  }

  private void writeFrame(WsConnectionState conn, ResponseFrame frame) {
    WebSocketConnection live =
        openConnections.findByConnectionId(conn.wsConnectionId()).orElse(null);
    if (live == null) {
      return;
    }
    try {
      String json = mapper.writeValueAsString(frame);
      synchronized (conn.writeLock()) {
        live.sendTextAndAwait(json);
      }
    } catch (Exception e) {
      log.warn("ws write failed connectionId={}: {}", conn.connectionId(), e.toString());
    }
  }

  private void writeBinaryPair(
      WsConnectionState conn, String commandId, DispatchResult.Binary bin, String requestId) {
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
    WebSocketConnection live =
        openConnections.findByConnectionId(conn.wsConnectionId()).orElse(null);
    if (live == null) {
      return;
    }
    // Atomic pair: header text frame + binary frame must arrive adjacent on the wire,
    // never interleaved with watcher events or another command response.
    synchronized (conn.writeLock()) {
      try {
        live.sendTextAndAwait(headerJson);
        live.sendBinaryAndAwait(Buffer.buffer(bytes));
      } catch (Exception e) {
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

  private void safeClose(CloseReason reason) {
    try {
      if (connection.isOpen()) {
        connection.closeAndAwait(reason);
      }
    } catch (Exception e) {
      log.debug("ws close failed: {}", e.toString());
    }
  }

  /** Close path that runs off the @OnOpen/@OnTextMessage callback thread. */
  private void safeCloseLive(WsConnectionState conn, CloseReason reason) {
    WebSocketConnection live =
        openConnections.findByConnectionId(conn.wsConnectionId()).orElse(null);
    if (live == null) {
      return;
    }
    try {
      live.closeAndAwait(reason);
    } catch (Exception e) {
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
