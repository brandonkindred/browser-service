package io.browserservice.api.error;

import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;

public class CaptureNotFoundException extends ApiException {
  public CaptureNotFoundException(UUID captureId) {
    super(
        "capture_not_found",
        HttpStatus.NOT_FOUND,
        "capture not found: " + captureId,
        Map.of("capture_id", captureId.toString()));
  }
}
