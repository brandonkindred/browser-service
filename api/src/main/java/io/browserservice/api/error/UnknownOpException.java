package io.browserservice.api.error;

import java.util.Map;
import org.springframework.http.HttpStatus;

public class UnknownOpException extends ApiException {
    public UnknownOpException(String op) {
        super("unknown_op", HttpStatus.BAD_REQUEST,
                "unknown op: " + op, Map.of("op", op == null ? "" : op));
    }
}
