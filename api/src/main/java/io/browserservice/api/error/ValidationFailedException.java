package io.browserservice.api.error;

import java.util.Map;
import org.springframework.http.HttpStatus;

public class ValidationFailedException extends ApiException {
    public ValidationFailedException(String message) {
        super("validation_failed", HttpStatus.BAD_REQUEST, message);
    }

    public ValidationFailedException(String message, Map<String, Object> details) {
        super("validation_failed", HttpStatus.BAD_REQUEST, message, details);
    }
}
