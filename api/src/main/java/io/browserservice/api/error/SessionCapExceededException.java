package io.browserservice.api.error;

import java.util.Map;
import org.springframework.http.HttpStatus;

public class SessionCapExceededException extends ApiException {
  public SessionCapExceededException(int cap) {
    super(
        "session_cap_exceeded",
        HttpStatus.TOO_MANY_REQUESTS,
        "concurrent session cap reached (" + cap + ")",
        Map.of("max_concurrent", cap));
  }
}
