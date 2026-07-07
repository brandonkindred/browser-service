package io.browserservice.api.dto;

import com.looksee.browser.enums.BrowserType;
import java.time.Instant;
import java.util.UUID;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

@Schema(description = "Session state snapshot including current URL, viewport, and scroll offset.")
public record SessionStateResponse(
    @Schema(description = "Session identifier") UUID sessionId,
    @Schema(
            description =
                "Caller that owns the session — canonical `tenant_id:sub` derived from the"
                    + " creator's OIDC bearer token")
        String ownerId,
    @Schema(description = "Browser type") BrowserType browserType,
    @Schema(description = "Instant the session was created") Instant createdAt,
    @Schema(
            description =
                "Instant of the most recent operation that refreshed the idle clock — read-only"
                    + " endpoints (this one included) do not advance it")
        Instant lastUsedAt,
    @Schema(description = "Idle TTL in seconds") long idleTtlSeconds,
    @Schema(description = "Absolute TTL in seconds") long absoluteTtlSeconds,
    @Schema(description = "Instant at which the session will be reaped") Instant expiresAt,
    @Schema(description = "Current URL as reported by the driver", nullable = true)
        String currentUrl,
    @Schema(description = "Current viewport size", nullable = true) Viewport viewport,
    @Schema(description = "Current scroll offset", nullable = true) ScrollOffset scrollOffset) {}
