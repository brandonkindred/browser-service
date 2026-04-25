package io.browserservice.api.error;

import io.browserservice.api.dto.ErrorDetail;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.UnhandledAlertException;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.remote.UnreachableBrowserException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

public final class ErrorMapper {

    private static final Logger log = LoggerFactory.getLogger(ErrorMapper.class);

    private ErrorMapper() {}

    public record Mapped(HttpStatus status, ErrorDetail body) {}

    public static Mapped map(Throwable t, String requestId) {
        if (t instanceof ApiException ex) {
            return build(ex.getHttpStatus(), ex.getCode(), ex.getMessage(), ex.getDetails(), requestId);
        }
        if (t instanceof MethodArgumentNotValidException ex) {
            Map<String, Object> details = new HashMap<>();
            details.put("fields", ex.getBindingResult().getFieldErrors().stream()
                    .collect(Collectors.toMap(
                            err -> err.getField(),
                            err -> err.getDefaultMessage() == null ? "invalid" : err.getDefaultMessage(),
                            (a, b) -> a)));
            return build(HttpStatus.BAD_REQUEST, "validation_failed", "request validation failed", details, requestId);
        }
        if (t instanceof HttpMessageNotReadableException) {
            return build(HttpStatus.BAD_REQUEST, "validation_failed", "malformed request body", null, requestId);
        }
        if (t instanceof MethodArgumentTypeMismatchException ex) {
            return build(HttpStatus.BAD_REQUEST, "validation_failed",
                    "parameter type mismatch: " + ex.getName(), Map.of("parameter", ex.getName()), requestId);
        }
        if (t instanceof NoSuchElementException) {
            return build(HttpStatus.NOT_FOUND, "element_not_found", safeMessage(t), null, requestId);
        }
        if (t instanceof TimeoutException) {
            return build(HttpStatus.REQUEST_TIMEOUT, "upstream_timeout", safeMessage(t), null, requestId);
        }
        if (t instanceof UnhandledAlertException) {
            return build(HttpStatus.CONFLICT, "unhandled_alert", safeMessage(t), null, requestId);
        }
        if (t instanceof StaleElementReferenceException) {
            return build(HttpStatus.CONFLICT, "stale_element", safeMessage(t), null, requestId);
        }
        if (t instanceof UnreachableBrowserException) {
            return build(HttpStatus.BAD_GATEWAY, "upstream_unavailable", safeMessage(t), null, requestId);
        }
        if (t instanceof WebDriverException) {
            log.warn("webdriver error", t);
            return build(HttpStatus.BAD_GATEWAY, "webdriver_error", safeMessage(t), null, requestId);
        }
        log.error("unexpected error", t);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "internal_error",
                "an unexpected error occurred", null, requestId);
    }

    public static String safeMessage(Throwable ex) {
        String msg = ex.getMessage();
        if (msg == null || msg.isBlank()) {
            return ex.getClass().getSimpleName();
        }
        int newline = msg.indexOf('\n');
        return newline > 0 ? msg.substring(0, newline) : msg;
    }

    private static Mapped build(HttpStatus status, String code, String message,
                                Map<String, Object> details, String requestId) {
        return new Mapped(status, new ErrorDetail(code, message, details, requestId));
    }
}
