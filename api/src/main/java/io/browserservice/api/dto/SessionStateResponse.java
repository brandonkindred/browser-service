package io.browserservice.api.dto;

import com.looksee.browser.enums.BrowserEnvironment;
import com.looksee.browser.enums.BrowserType;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.UUID;

@Schema(description = "Session state snapshot including current URL, viewport, and scroll offset.")
public record SessionStateResponse(
        @Schema(description = "Session identifier") UUID sessionId,
        @Schema(description = "Browser type") BrowserType browserType,
        @Schema(description = "Session environment") BrowserEnvironment environment,
        @Schema(description = "Instant the session was created") Instant createdAt,
        @Schema(description = "Instant at which the session will be reaped") Instant expiresAt,
        @Schema(description = "Current URL as reported by the driver", nullable = true) String currentUrl,
        @Schema(description = "Current viewport size", nullable = true) Viewport viewport,
        @Schema(description = "Current scroll offset", nullable = true) ScrollOffset scrollOffset) {
}
