package io.browserservice.api.ws;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.NullNode;
import io.browserservice.api.dto.AlertRespondRequest;
import io.browserservice.api.dto.CaptureRequest;
import io.browserservice.api.dto.CreateSessionRequest;
import io.browserservice.api.dto.DomRemoveRequest;
import io.browserservice.api.dto.ElementActionRequest;
import io.browserservice.api.dto.ElementScreenshotRequest;
import io.browserservice.api.dto.ElementTouchRequest;
import io.browserservice.api.dto.ExecuteRequest;
import io.browserservice.api.dto.FindElementRequest;
import io.browserservice.api.dto.MouseMoveRequest;
import io.browserservice.api.dto.NavigateRequest;
import io.browserservice.api.dto.ScreenshotRequest;
import io.browserservice.api.dto.ScrollRequest;
import io.browserservice.api.dto.SessionResponse;
import io.browserservice.api.error.AlreadyBoundException;
import io.browserservice.api.error.SessionNotBoundException;
import io.browserservice.api.error.UnknownOpException;
import io.browserservice.api.error.ValidationFailedException;
import io.browserservice.api.service.AlertService;
import io.browserservice.api.service.BrowserOperationsService;
import io.browserservice.api.service.CaptureService;
import io.browserservice.api.service.ElementOperationsService;
import io.browserservice.api.service.SessionService;
import io.browserservice.api.ws.push.WatcherCoordinator;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

/**
 * Maps WS command frames to existing service calls. No business logic here — every entry
 * deserializes params, manually validates (mirroring REST {@code @Valid}), and delegates to a
 * service whose contract is already exercised by the REST controllers.
 */
@Component
public class CommandDispatcher {

  private final SessionService sessionService;
  private final BrowserOperationsService browserOps;
  private final ElementOperationsService elementOps;
  private final AlertService alertService;
  private final CaptureService captureService;
  private final WatcherCoordinator watchers;
  private final Validator validator;
  private final ObjectMapper mapper;

  private final Map<String, CommandHandler> handlers;

  public CommandDispatcher(
      SessionService sessionService,
      BrowserOperationsService browserOps,
      ElementOperationsService elementOps,
      AlertService alertService,
      CaptureService captureService,
      WatcherCoordinator watchers,
      Validator validator,
      ObjectMapper mapper) {
    this.sessionService = sessionService;
    this.browserOps = browserOps;
    this.elementOps = elementOps;
    this.alertService = alertService;
    this.captureService = captureService;
    this.watchers = watchers;
    this.validator = validator;
    this.mapper = mapper;
    this.handlers = buildHandlers();
  }

  public DispatchResult dispatch(Connection conn, String op, JsonNode params) {
    CommandHandler handler = handlers.get(op);
    if (handler == null) {
      throw new UnknownOpException(op);
    }
    Object result = handler.handle(conn, params == null ? NullNode.getInstance() : params);
    return result instanceof DispatchResult dr ? dr : new DispatchResult.Json(result);
  }

  private Map<String, CommandHandler> buildHandlers() {
    Map<String, CommandHandler> h = new HashMap<>();

    // Session lifecycle
    h.put(
        "session.create",
        (conn, params) -> {
          requireUnbound(conn);
          CreateSessionRequest req = parseAndValidate(params, CreateSessionRequest.class);
          SessionResponse resp = sessionService.create(req, conn.caller());
          conn.bind(resp.sessionId());
          watchers.onSessionAttached(resp.sessionId(), conn);
          return resp;
        });
    h.put(
        "session.attach",
        (conn, params) -> {
          requireUnbound(conn);
          UUID sessionId = readSessionId(params);
          // Resolve state first; only bind once we've confirmed the session is still alive
          // and the caller owns it. describe() delegates to requireOwner, so a wrong-owner
          // attempt throws SessionForbiddenException, which the WS handler closes with 4403.
          // Otherwise a SessionNotFoundException would leave the connection bound to a dead
          // id and fail every subsequent session.create / session.attach with already_bound.
          Object state = sessionService.describe(sessionId, conn.caller());
          conn.bind(sessionId);
          watchers.onSessionAttached(sessionId, conn);
          return state;
        });
    h.put(
        "session.describe",
        (conn, params) -> sessionService.describe(requireBound(conn), conn.caller()));
    h.put(
        "session.close",
        (conn, params) -> {
          UUID id = requireBound(conn);
          sessionService.close(id, conn.caller());
          watchers.onSessionDetached(id, conn);
          conn.unbind();
          return null;
        });

    // Navigation
    h.put(
        "navigation.navigate",
        (conn, params) ->
            browserOps.navigate(
                requireBound(conn),
                conn.caller(),
                parseAndValidate(params, NavigateRequest.class)));
    h.put(
        "navigation.source",
        (conn, params) -> browserOps.getSource(requireBound(conn), conn.caller()));
    h.put(
        "navigation.status",
        (conn, params) -> browserOps.getStatus(requireBound(conn), conn.caller()));

    // Screenshots — WS-C: emit a (binary-header, binary-frame) pair instead of base64.
    h.put(
        "screenshot.page",
        (conn, params) -> {
          ScreenshotRequest req = parseAndValidate(params, ScreenshotRequest.class);
          byte[] png = browserOps.pageScreenshot(requireBound(conn), conn.caller(), req.strategy());
          return new DispatchResult.Binary("image/png", png);
        });
    h.put(
        "screenshot.element",
        (conn, params) -> {
          ElementScreenshotRequest req = parseAndValidate(params, ElementScreenshotRequest.class);
          byte[] png = elementOps.elementScreenshot(requireBound(conn), conn.caller(), req);
          return new DispatchResult.Binary("image/png", png);
        });

    // Capture (one-shot session under the hood; not bound to the WS connection)
    h.put(
        "capture.run",
        (conn, params) ->
            captureService.capture(parseAndValidate(params, CaptureRequest.class), conn.caller()));
    h.put(
        "capture.fetchScreenshot",
        (conn, params) -> {
          UUID captureId = readUuid(params, "capture_id");
          byte[] png = captureService.fetchScreenshot(captureId, conn.caller()).pngBytes();
          return new DispatchResult.Binary("image/png", png);
        });

    // Alerts
    h.put(
        "alert.state", (conn, params) -> alertService.getAlert(requireBound(conn), conn.caller()));
    h.put(
        "alert.respond",
        (conn, params) -> {
          alertService.respond(
              requireBound(conn),
              conn.caller(),
              parseAndValidate(params, AlertRespondRequest.class));
          return null;
        });

    // Script
    h.put(
        "script.execute",
        (conn, params) ->
            browserOps.executeScript(
                requireBound(conn), conn.caller(), parseAndValidate(params, ExecuteRequest.class)));

    // Mouse
    h.put(
        "mouse.move",
        (conn, params) -> {
          browserOps.moveMouse(
              requireBound(conn), conn.caller(), parseAndValidate(params, MouseMoveRequest.class));
          return null;
        });

    // Elements
    h.put(
        "element.find",
        (conn, params) ->
            elementOps.find(
                requireBound(conn),
                conn.caller(),
                parseAndValidate(params, FindElementRequest.class)));
    h.put(
        "element.action",
        (conn, params) -> {
          elementOps.action(
              requireBound(conn),
              conn.caller(),
              parseAndValidate(params, ElementActionRequest.class));
          return null;
        });

    // Touch
    h.put(
        "touch.tap",
        (conn, params) -> {
          elementOps.touch(
              requireBound(conn),
              conn.caller(),
              parseAndValidate(params, ElementTouchRequest.class));
          return null;
        });

    // Scroll
    h.put(
        "scroll.to",
        (conn, params) ->
            browserOps.scroll(
                requireBound(conn), conn.caller(), parseAndValidate(params, ScrollRequest.class)));

    // Viewport
    h.put(
        "viewport.state",
        (conn, params) -> browserOps.getViewport(requireBound(conn), conn.caller()));

    // DOM
    h.put(
        "dom.remove",
        (conn, params) -> {
          browserOps.removeDom(
              requireBound(conn), conn.caller(), parseAndValidate(params, DomRemoveRequest.class));
          return null;
        });

    return Map.copyOf(h);
  }

  public Set<String> ops() {
    return handlers.keySet();
  }

  private <T> T parseAndValidate(JsonNode params, Class<T> type) {
    T req;
    try {
      req = mapper.treeToValue(params, type);
    } catch (Exception e) {
      throw new ValidationFailedException(
          "malformed params: " + ErrorMapperMessage.shortMessage(e));
    }
    if (req == null) {
      throw new ValidationFailedException("params are required");
    }
    Set<ConstraintViolation<T>> violations = validator.validate(req);
    if (!violations.isEmpty()) {
      Map<String, Object> details =
          Map.of(
              "fields",
              violations.stream()
                  .collect(
                      Collectors.toMap(
                          v -> v.getPropertyPath().toString(),
                          v -> v.getMessage() == null ? "invalid" : v.getMessage(),
                          (a, b) -> a)));
      throw new ValidationFailedException("request validation failed", details);
    }
    return req;
  }

  private static UUID readSessionId(JsonNode params) {
    return readUuid(params, "session_id");
  }

  private static UUID readUuid(JsonNode params, String field) {
    JsonNode node = params.get(field);
    if (node == null || node.isNull() || !node.isTextual() || node.asText().isBlank()) {
      throw new ValidationFailedException(
          field + " is required", Map.of("fields", Map.of(field, "must be a UUID string")));
    }
    try {
      return UUID.fromString(node.asText());
    } catch (IllegalArgumentException e) {
      throw new ValidationFailedException(
          field + " must be a UUID", Map.of("fields", Map.of(field, "must be a UUID string")));
    }
  }

  private static UUID requireBound(Connection conn) {
    UUID id = conn.boundSessionId();
    if (id == null) {
      throw new SessionNotBoundException();
    }
    return id;
  }

  private static void requireUnbound(Connection conn) {
    UUID id = conn.boundSessionId();
    if (id != null) {
      throw new AlreadyBoundException(id.toString());
    }
  }

  @FunctionalInterface
  interface CommandHandler {
    Object handle(Connection conn, JsonNode params);
  }

  /** Inline helper to keep the dispatcher self-contained. */
  private static final class ErrorMapperMessage {
    static String shortMessage(Throwable t) {
      String m = t.getMessage();
      if (m == null) return t.getClass().getSimpleName();
      int nl = m.indexOf('\n');
      return nl > 0 ? m.substring(0, nl) : m;
    }
  }
}
