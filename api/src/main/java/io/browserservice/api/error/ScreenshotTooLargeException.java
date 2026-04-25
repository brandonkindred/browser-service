package io.browserservice.api.error;

import java.util.Map;
import org.springframework.http.HttpStatus;

public class ScreenshotTooLargeException extends ApiException {

    public ScreenshotTooLargeException(long size, long limit) {
        super("screenshot_too_large", HttpStatus.PAYLOAD_TOO_LARGE,
                "screenshot exceeds maxBinaryFrameBytes",
                Map.of("size", size, "limit", limit));
    }
}
