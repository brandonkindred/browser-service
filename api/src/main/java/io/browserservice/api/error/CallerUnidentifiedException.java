package io.browserservice.api.error;

import org.springframework.http.HttpStatus;

public class CallerUnidentifiedException extends ApiException {

  public CallerUnidentifiedException() {
    super("caller_unidentified", HttpStatus.BAD_REQUEST, "X-Caller-Id header is required");
  }

  public CallerUnidentifiedException(String reason, Throwable cause) {
    super(
        "caller_unidentified",
        HttpStatus.BAD_REQUEST,
        "X-Caller-Id header is invalid: " + reason,
        null,
        cause);
  }
}
