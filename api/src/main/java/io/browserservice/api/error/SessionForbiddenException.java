package io.browserservice.api.error;

import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;

public class SessionForbiddenException extends ApiException {

  private final UUID id;

  public SessionForbiddenException(UUID sessionId) {
    super(
        "session_forbidden",
        HttpStatus.FORBIDDEN,
        "session is owned by a different caller: " + sessionId,
        Map.of("session_id", sessionId.toString()));
    this.id = sessionId;
  }

  public UUID sessionId() {
    return id;
  }
}
