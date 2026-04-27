package io.browserservice.api.error;

import java.util.Map;
import org.springframework.http.HttpStatus;

public class AlreadyBoundException extends ApiException {
  public AlreadyBoundException(String boundSessionId) {
    super(
        "already_bound",
        HttpStatus.CONFLICT,
        "this connection is already bound to a session",
        Map.of("boundSessionId", boundSessionId));
  }
}
