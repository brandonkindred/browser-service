package io.browserservice.api.error;

import io.browserservice.api.dto.ErrorDetail;
import io.browserservice.api.dto.ErrorResponse;
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
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ErrorResponse> handleApi(ApiException ex) {
        return build(ex.getHttpStatus(), ex.getCode(), ex.getMessage(), ex.getDetails());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        Map<String, Object> details = new HashMap<>();
        details.put("fields", ex.getBindingResult().getFieldErrors().stream()
                .collect(Collectors.toMap(
                        err -> err.getField(),
                        err -> err.getDefaultMessage() == null ? "invalid" : err.getDefaultMessage(),
                        (a, b) -> a)));
        return build(HttpStatus.BAD_REQUEST, "validation_failed", "request validation failed", details);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleUnreadable(HttpMessageNotReadableException ex) {
        return build(HttpStatus.BAD_REQUEST, "validation_failed", "malformed request body", null);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        return build(HttpStatus.BAD_REQUEST, "validation_failed",
                "parameter type mismatch: " + ex.getName(), Map.of("parameter", ex.getName()));
    }

    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<ErrorResponse> handleNoSuchElement(NoSuchElementException ex) {
        return build(HttpStatus.NOT_FOUND, "element_not_found", safeMessage(ex), null);
    }

    @ExceptionHandler(TimeoutException.class)
    public ResponseEntity<ErrorResponse> handleTimeout(TimeoutException ex) {
        return build(HttpStatus.REQUEST_TIMEOUT, "upstream_timeout", safeMessage(ex), null);
    }

    @ExceptionHandler(UnhandledAlertException.class)
    public ResponseEntity<ErrorResponse> handleAlert(UnhandledAlertException ex) {
        return build(HttpStatus.CONFLICT, "unhandled_alert", safeMessage(ex), null);
    }

    @ExceptionHandler(StaleElementReferenceException.class)
    public ResponseEntity<ErrorResponse> handleStale(StaleElementReferenceException ex) {
        return build(HttpStatus.CONFLICT, "stale_element", safeMessage(ex), null);
    }

    @ExceptionHandler(UnreachableBrowserException.class)
    public ResponseEntity<ErrorResponse> handleUnreachable(UnreachableBrowserException ex) {
        return build(HttpStatus.BAD_GATEWAY, "upstream_unavailable", safeMessage(ex), null);
    }

    @ExceptionHandler(WebDriverException.class)
    public ResponseEntity<ErrorResponse> handleWebDriver(WebDriverException ex) {
        log.warn("webdriver error", ex);
        return build(HttpStatus.BAD_GATEWAY, "webdriver_error", safeMessage(ex), null);
    }

    @ExceptionHandler(Throwable.class)
    public ResponseEntity<ErrorResponse> handleUnknown(Throwable ex) {
        log.error("unexpected error", ex);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "internal_error",
                "an unexpected error occurred", null);
    }

    private ResponseEntity<ErrorResponse> build(HttpStatus status, String code, String message, Map<String, Object> details) {
        ErrorDetail detail = new ErrorDetail(code, message, details, RequestIdFilter.currentRequestId());
        return ResponseEntity.status(status).body(new ErrorResponse(detail));
    }

    private static String safeMessage(Throwable ex) {
        String msg = ex.getMessage();
        if (msg == null || msg.isBlank()) {
            return ex.getClass().getSimpleName();
        }
        int newline = msg.indexOf('\n');
        return newline > 0 ? msg.substring(0, newline) : msg;
    }
}
