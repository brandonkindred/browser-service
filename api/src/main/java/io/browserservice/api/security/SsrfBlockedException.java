package io.browserservice.api.security;

import io.browserservice.api.error.ApiException;
import java.util.Map;
import org.springframework.http.HttpStatus;

/**
 * Thrown when {@link UrlSafetyValidator} rejects a user-supplied URL. The response carries a
 * generic message; the specific {@link SsrfBlockReason} ships in {@code details.reason} so callers
 * can disambiguate without leaking the original host or its resolved IPs.
 */
public class SsrfBlockedException extends ApiException {

  private static final long serialVersionUID = 1L;

  /** Builds the 400-mapped exception with {@code reason} surfaced in {@code details}. */
  public SsrfBlockedException(SsrfBlockReason reason) {
    this(reason, null);
  }

  /** Same as above, but preserves a parsing/DNS failure as {@code cause} for forensics. */
  public SsrfBlockedException(SsrfBlockReason reason, Throwable cause) {
    super(
        "ssrf_blocked",
        HttpStatus.BAD_REQUEST,
        "URL blocked by SSRF guard",
        Map.of("reason", reason.wireValue()),
        cause);
  }
}
