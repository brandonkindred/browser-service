package io.browserservice.api.error;

import com.fasterxml.jackson.core.JacksonException;
import io.browserservice.api.dto.ErrorDetail;
import io.quarkus.security.AuthenticationFailedException;
import io.quarkus.security.ForbiddenException;
import io.quarkus.security.UnauthorizedException;
import jakarta.validation.ConstraintViolationException;
import jakarta.ws.rs.NotAcceptableException;
import jakarta.ws.rs.NotAllowedException;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.NotSupportedException;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import org.eclipse.microprofile.faulttolerance.exceptions.BulkheadException;
import org.eclipse.microprofile.faulttolerance.exceptions.CircuitBreakerOpenException;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.UnhandledAlertException;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.remote.UnreachableBrowserException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;

public final class ErrorMapper {

  private static final Logger log = LoggerFactory.getLogger(ErrorMapper.class);

  // Match the @CircuitBreaker delay on SeleniumGuard (30s). GlobalExceptionHandler reads
  // this off ErrorDetail.details and emits a Retry-After header.
  public static final String RETRY_AFTER_KEY = "retry_after_seconds";

  private static final int CIRCUIT_OPEN_RETRY_AFTER_SECONDS = 30;

  private ErrorMapper() {}

  public record Mapped(HttpStatus status, ErrorDetail body) {}

  public static Mapped map(Throwable t, String requestId) {
    if (t instanceof ApiException ex) {
      return build(ex.getHttpStatus(), ex.getCode(), ex.getMessage(), ex.getDetails(), requestId);
    }
    // quarkus-oidc raises AuthenticationFailedException on invalid tokens (bad
    // signature, wrong issuer/audience, expired) and UnauthorizedException on
    // missing credentials. Render both through the standard ErrorResponse shape
    // so clients get a consistent JSON body, not the framework's bare 401.
    if (t instanceof AuthenticationFailedException) {
      return build(
          HttpStatus.UNAUTHORIZED, "unauthenticated", "invalid bearer token", null, requestId);
    }
    if (t instanceof UnauthorizedException) {
      return build(
          HttpStatus.UNAUTHORIZED, "unauthenticated", "authentication required", null, requestId);
    }
    if (t instanceof ForbiddenException) {
      return build(HttpStatus.FORBIDDEN, "forbidden", "access denied", null, requestId);
    }
    if (t instanceof ConstraintViolationException ex) {
      Map<String, Object> details = new HashMap<>();
      details.put(
          "fields",
          ex.getConstraintViolations().stream()
              .collect(
                  Collectors.toMap(
                      v -> {
                        String path = v.getPropertyPath().toString();
                        int dot = path.lastIndexOf('.');
                        return dot >= 0 ? path.substring(dot + 1) : path;
                      },
                      v -> v.getMessage() == null ? "invalid" : v.getMessage(),
                      (a, b) -> a)));
      return build(
          HttpStatus.BAD_REQUEST,
          "validation_failed",
          "request validation failed",
          details,
          requestId);
    }
    if (t instanceof JacksonException) {
      return build(
          HttpStatus.BAD_REQUEST, "validation_failed", "malformed request body", null, requestId);
    }
    if (t instanceof NotFoundException) {
      return build(HttpStatus.NOT_FOUND, "route_not_found", safeMessage(t), null, requestId);
    }
    if (t instanceof NotAllowedException) {
      return build(
          HttpStatus.METHOD_NOT_ALLOWED, "method_not_allowed", safeMessage(t), null, requestId);
    }
    if (t instanceof NotAcceptableException) {
      return build(HttpStatus.NOT_ACCEPTABLE, "not_acceptable", safeMessage(t), null, requestId);
    }
    if (t instanceof NotSupportedException) {
      return build(
          HttpStatus.UNSUPPORTED_MEDIA_TYPE,
          "unsupported_media_type",
          safeMessage(t),
          null,
          requestId);
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
    if (t instanceof CircuitBreakerOpenException) {
      return build(
          HttpStatus.SERVICE_UNAVAILABLE,
          "selenium_circuit_open",
          "upstream selenium is degraded; circuit breaker is open",
          Map.of(RETRY_AFTER_KEY, CIRCUIT_OPEN_RETRY_AFTER_SECONDS),
          requestId);
    }
    if (t instanceof BulkheadException) {
      // No Retry-After: bulkhead holders are bounded only by their per-method @Timeout (some have
      // none), so any specific hint would be misleading. Clients should back off themselves.
      return build(
          HttpStatus.SERVICE_UNAVAILABLE,
          "concurrency_exceeded",
          "too many concurrent webdriver operations on this replica",
          null,
          requestId);
    }
    if (t instanceof org.eclipse.microprofile.faulttolerance.exceptions.TimeoutException) {
      return build(
          HttpStatus.GATEWAY_TIMEOUT, "selenium_call_timeout", safeMessage(t), null, requestId);
    }
    if (t instanceof UnreachableBrowserException) {
      return build(HttpStatus.BAD_GATEWAY, "upstream_unavailable", safeMessage(t), null, requestId);
    }
    if (t instanceof WebDriverException) {
      log.warn("webdriver error", t);
      return build(HttpStatus.BAD_GATEWAY, "webdriver_error", safeMessage(t), null, requestId);
    }
    log.error("unexpected error", t);
    return build(
        HttpStatus.INTERNAL_SERVER_ERROR,
        "internal_error",
        "an unexpected error occurred",
        null,
        requestId);
  }

  public static String safeMessage(Throwable ex) {
    String msg = ex.getMessage();
    if (msg == null || msg.isBlank()) {
      return ex.getClass().getSimpleName();
    }
    int newline = msg.indexOf('\n');
    return newline > 0 ? msg.substring(0, newline) : msg;
  }

  private static Mapped build(
      HttpStatus status,
      String code,
      String message,
      Map<String, Object> details,
      String requestId) {
    return new Mapped(status, new ErrorDetail(code, message, details, requestId));
  }
}
