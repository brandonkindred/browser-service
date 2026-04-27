package io.browserservice.api.error;

import java.util.Map;
import org.springframework.http.HttpStatus;

public class CommandQueueFullException extends ApiException {
  public CommandQueueFullException(int depth) {
    super(
        "command_queue_full",
        HttpStatus.SERVICE_UNAVAILABLE,
        "command queue is full; wait for in-flight responses before sending more",
        Map.of("queueDepth", depth));
  }
}
