package io.browserservice.api.error;

import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;

public class CaptureExpiredException extends ApiException {
    public CaptureExpiredException(UUID captureId) {
        super("capture_expired", HttpStatus.GONE,
                "capture expired: " + captureId,
                Map.of("capture_id", captureId.toString()));
    }
}
