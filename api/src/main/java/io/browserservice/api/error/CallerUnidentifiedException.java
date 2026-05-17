package io.browserservice.api.error;

import org.springframework.http.HttpStatus;

/**
 * Raised when a request reaches the API without a usable caller identity — either no bearer token,
 * an unverifiable token (handled upstream by quarkus-oidc and translated by {@code
 * UnauthorizedExceptionMapper}), or a verified token missing the {@code sub} / {@code tenant_id}
 * claims this service requires.
 *
 * <p>Maps to HTTP 401. Error codes intentionally distinguish the two failure modes so observability
 * and client-side error handling can react differently.
 */
public class CallerUnidentifiedException extends ApiException {

  public CallerUnidentifiedException() {
    super("unauthenticated", HttpStatus.UNAUTHORIZED, "authentication required");
  }

  public CallerUnidentifiedException(String reason) {
    super("unauthenticated", HttpStatus.UNAUTHORIZED, reason);
  }

  public CallerUnidentifiedException(String code, String reason) {
    super(code, HttpStatus.UNAUTHORIZED, reason);
  }

  public CallerUnidentifiedException(String code, String reason, Throwable cause) {
    super(code, HttpStatus.UNAUTHORIZED, reason, null, cause);
  }
}
