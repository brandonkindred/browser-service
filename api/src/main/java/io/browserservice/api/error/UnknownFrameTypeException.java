package io.browserservice.api.error;

import java.util.Map;
import org.springframework.http.HttpStatus;

public class UnknownFrameTypeException extends ApiException {
  public UnknownFrameTypeException(String type) {
    super(
        "unknown_frame_type",
        HttpStatus.BAD_REQUEST,
        "unknown frame type: " + type,
        Map.of("type", type == null ? "" : type));
  }
}
