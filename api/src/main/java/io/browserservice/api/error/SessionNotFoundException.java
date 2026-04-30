package io.browserservice.api.error;

import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;

public class SessionNotFoundException extends ApiException {
    public SessionNotFoundException(UUID id) {
        super("session_not_found", HttpStatus.NOT_FOUND,
                "session not found: " + id,
                Map.of("session_id", id.toString()));
    }
}
