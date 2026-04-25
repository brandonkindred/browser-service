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
import io.browserservice.api.dto.ScreenshotBase64Response;
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
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import java.io.ByteArrayInputStream;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import javax.imageio.ImageIO;
import org.springframework.stereotype.Component;

/**
 * Maps WS command frames to existing service calls. No business logic here — every entry
 * deserializes params, manually validates (mirroring REST {@code @Valid}), and delegates
 * to a service whose contract is already exercised by the REST controllers.
 */
@Component
public class CommandDispatcher {

    static {
        // Eagerly load PNG codec to avoid ServiceLoader edge cases under virtual threads,
        // matching ScreenshotsController.
        ImageIO.scanForPlugins();
    }

    private final SessionService sessionService;
    private final BrowserOperationsService browserOps;
    private final ElementOperationsService elementOps;
    private final AlertService alertService;
    private final CaptureService captureService;
    private final WsSessionOwnership ownership;
    private final Validator validator;
    private final ObjectMapper mapper;

    private final Map<String, CommandHandler> handlers;

    public CommandDispatcher(SessionService sessionService,
                             BrowserOperationsService browserOps,
                             ElementOperationsService elementOps,
                             AlertService alertService,
                             CaptureService captureService,
                             WsSessionOwnership ownership,
                             Validator validator,
                             ObjectMapper mapper) {
        this.sessionService = sessionService;
        this.browserOps = browserOps;
        this.elementOps = elementOps;
        this.alertService = alertService;
        this.captureService = captureService;
        this.ownership = ownership;
        this.validator = validator;
        this.mapper = mapper;
        this.handlers = buildHandlers();
    }

    public Object dispatch(Connection conn, String op, JsonNode params) throws OwnershipMismatchException {
        CommandHandler handler = handlers.get(op);
        if (handler == null) {
            throw new UnknownOpException(op);
        }
        return handler.handle(conn, params == null ? NullNode.getInstance() : params);
    }

    private Map<String, CommandHandler> buildHandlers() {
        Map<String, CommandHandler> h = new HashMap<>();

        // Session lifecycle
        h.put("session.create", (conn, params) -> {
            requireUnbound(conn);
            CreateSessionRequest req = parseAndValidate(params, CreateSessionRequest.class);
            SessionResponse resp = sessionService.create(req);
            ownership.claim(resp.sessionId(), conn.caller());
            conn.bind(resp.sessionId());
            return resp;
        });
        h.put("session.attach", (conn, params) -> {
            requireUnbound(conn);
            UUID sessionId = readSessionId(params);
            if (!ownership.isOwnedBy(sessionId, conn.caller())) {
                throw new OwnershipMismatchException(sessionId);
            }
            conn.bind(sessionId);
            return sessionService.describe(sessionId);
        });
        h.put("session.describe", (conn, params) -> sessionService.describe(requireBound(conn)));
        h.put("session.close", (conn, params) -> {
            UUID id = requireBound(conn);
            sessionService.close(id);
            ownership.release(id);
            conn.unbind();
            return null;
        });

        // Navigation
        h.put("navigation.navigate", (conn, params) ->
                browserOps.navigate(requireBound(conn), parseAndValidate(params, NavigateRequest.class)));
        h.put("navigation.source", (conn, params) -> browserOps.getSource(requireBound(conn)));
        h.put("navigation.status", (conn, params) -> browserOps.getStatus(requireBound(conn)));

        // Screenshots — WS-A returns base64-in-JSON; WS-C will replace with binary frames.
        h.put("screenshot.page", (conn, params) -> {
            ScreenshotRequest req = parseAndValidate(params, ScreenshotRequest.class);
            byte[] png = browserOps.pageScreenshot(requireBound(conn), req.strategy());
            return toBase64Response(png);
        });
        h.put("screenshot.element", (conn, params) -> {
            ElementScreenshotRequest req = parseAndValidate(params, ElementScreenshotRequest.class);
            byte[] png = elementOps.elementScreenshot(requireBound(conn), req);
            return toBase64Response(png);
        });

        // Capture (one-shot session under the hood; not bound to the WS connection)
        h.put("capture.run", (conn, params) ->
                captureService.capture(parseAndValidate(params, CaptureRequest.class)));
        h.put("capture.fetchScreenshot", (conn, params) -> {
            UUID captureId = readUuid(params, "captureId");
            byte[] png = captureService.fetchScreenshot(captureId).pngBytes();
            return toBase64Response(png);
        });

        // Alerts
        h.put("alert.state", (conn, params) -> alertService.getAlert(requireBound(conn)));
        h.put("alert.respond", (conn, params) -> {
            alertService.respond(requireBound(conn), parseAndValidate(params, AlertRespondRequest.class));
            return null;
        });

        // Script
        h.put("script.execute", (conn, params) ->
                browserOps.executeScript(requireBound(conn), parseAndValidate(params, ExecuteRequest.class)));

        // Mouse
        h.put("mouse.move", (conn, params) -> {
            browserOps.moveMouse(requireBound(conn), parseAndValidate(params, MouseMoveRequest.class));
            return null;
        });

        // Elements
        h.put("element.find", (conn, params) ->
                elementOps.find(requireBound(conn), parseAndValidate(params, FindElementRequest.class)));
        h.put("element.action", (conn, params) -> {
            elementOps.action(requireBound(conn), parseAndValidate(params, ElementActionRequest.class));
            return null;
        });

        // Touch
        h.put("touch.tap", (conn, params) -> {
            elementOps.touch(requireBound(conn), parseAndValidate(params, ElementTouchRequest.class));
            return null;
        });

        // Scroll
        h.put("scroll.to", (conn, params) ->
                browserOps.scroll(requireBound(conn), parseAndValidate(params, ScrollRequest.class)));

        // Viewport
        h.put("viewport.state", (conn, params) -> browserOps.getViewport(requireBound(conn)));

        // DOM
        h.put("dom.remove", (conn, params) -> {
            browserOps.removeDom(requireBound(conn), parseAndValidate(params, DomRemoveRequest.class));
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
            throw new ValidationFailedException("malformed params: " + ErrorMapperMessage.shortMessage(e));
        }
        if (req == null) {
            throw new ValidationFailedException("params are required");
        }
        Set<ConstraintViolation<T>> violations = validator.validate(req);
        if (!violations.isEmpty()) {
            Map<String, Object> details = Map.of("fields", violations.stream()
                    .collect(Collectors.toMap(
                            v -> v.getPropertyPath().toString(),
                            v -> v.getMessage() == null ? "invalid" : v.getMessage(),
                            (a, b) -> a)));
            throw new ValidationFailedException("request validation failed", details);
        }
        return req;
    }

    private static UUID readSessionId(JsonNode params) {
        return readUuid(params, "sessionId");
    }

    private static UUID readUuid(JsonNode params, String field) {
        JsonNode node = params.get(field);
        if (node == null || node.isNull() || !node.isTextual() || node.asText().isBlank()) {
            throw new ValidationFailedException(field + " is required",
                    Map.of("fields", Map.of(field, "must be a UUID string")));
        }
        try {
            return UUID.fromString(node.asText());
        } catch (IllegalArgumentException e) {
            throw new ValidationFailedException(field + " must be a UUID",
                    Map.of("fields", Map.of(field, "must be a UUID string")));
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

    private static ScreenshotBase64Response toBase64Response(byte[] png) {
        int[] wh = readDimensions(png);
        return new ScreenshotBase64Response(Base64.getEncoder().encodeToString(png), wh[0], wh[1]);
    }

    private static int[] readDimensions(byte[] png) {
        try (var in = new ByteArrayInputStream(png)) {
            var image = ImageIO.read(in);
            if (image == null) {
                return new int[]{0, 0};
            }
            return new int[]{image.getWidth(), image.getHeight()};
        } catch (Exception e) {
            return new int[]{0, 0};
        }
    }

    @FunctionalInterface
    interface CommandHandler {
        Object handle(Connection conn, JsonNode params) throws OwnershipMismatchException;
    }

    /** Signals an attach attempt against a session that this caller does not own. The handler
     *  catches this and closes the connection with code 4403 — does not write a normal error
     *  frame. */
    public static final class OwnershipMismatchException extends Exception {
        private final UUID sessionId;

        public OwnershipMismatchException(UUID sessionId) {
            super("session_forbidden: " + sessionId);
            this.sessionId = sessionId;
        }

        public UUID sessionId() {
            return sessionId;
        }
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
