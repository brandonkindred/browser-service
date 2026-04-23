package io.browserservice.api.error;

import org.springframework.http.HttpStatus;

public class UpstreamUnavailableException extends ApiException {
    public UpstreamUnavailableException(String message) {
        super("upstream_unavailable", HttpStatus.BAD_GATEWAY, message);
    }

    public UpstreamUnavailableException(String message, Throwable cause) {
        super("upstream_unavailable", HttpStatus.BAD_GATEWAY, message, null, cause);
    }
}
