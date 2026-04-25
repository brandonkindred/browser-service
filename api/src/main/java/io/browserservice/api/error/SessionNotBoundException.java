package io.browserservice.api.error;

import org.springframework.http.HttpStatus;

public class SessionNotBoundException extends ApiException {
    public SessionNotBoundException() {
        super("session_not_bound", HttpStatus.BAD_REQUEST,
                "this connection is not bound to a session; send session.create or session.attach first");
    }
}
