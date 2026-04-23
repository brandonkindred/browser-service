package io.browserservice.api.error;

import org.springframework.http.HttpStatus;

public class DesktopSessionRequiredException extends ApiException {
    public DesktopSessionRequiredException() {
        super("desktop_session_required", HttpStatus.CONFLICT,
                "this operation is only valid for desktop sessions");
    }
}
