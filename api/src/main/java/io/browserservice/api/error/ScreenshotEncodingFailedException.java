package io.browserservice.api.error;

import org.springframework.http.HttpStatus;

/**
 * Raised when a screenshot can't be encoded to PNG bytes locally. Distinct from {@link
 * UpstreamUnavailableException} because the failure is in our own encoding pipeline, not an
 * upstream Selenium issue — operators reading logs / clients parsing the error code should be able
 * to tell them apart.
 */
public class ScreenshotEncodingFailedException extends ApiException {

  private static final long serialVersionUID = 1L;

  /** Constructs a screenshot-encoding-failed exception with the given message. */
  public ScreenshotEncodingFailedException(String message) {
    super("screenshot_encoding_failed", HttpStatus.INTERNAL_SERVER_ERROR, message);
  }

  /** Constructs a screenshot-encoding-failed exception wrapping the given underlying cause. */
  public ScreenshotEncodingFailedException(String message, Throwable cause) {
    super("screenshot_encoding_failed", HttpStatus.INTERNAL_SERVER_ERROR, message, null, cause);
  }
}
