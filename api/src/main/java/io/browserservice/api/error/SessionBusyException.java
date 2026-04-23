package io.browserservice.api.error;

import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;

public class SessionBusyException extends ApiException {
    public SessionBusyException(UUID id) {
        super("session_busy", HttpStatus.CONFLICT,
                "session is busy serving another request: " + id,
                Map.of("session_id", id.toString()));
    }
}
