package io.browserservice.api.dto;

import com.looksee.browser.enums.BrowserEnvironment;
import com.looksee.browser.enums.BrowserType;
import java.time.Instant;
import java.util.UUID;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

@Schema(description = "Summary of an active browser session.")
public record SessionResponse(
    @Schema(description = "Session identifier") UUID sessionId,
    @Schema(
            description =
                "Caller that owns the session — canonical `tenant_id:sub` derived from the"
                    + " creator's OIDC bearer token")
        String ownerId,
    @Schema(description = "Browser type") BrowserType browserType,
    @Schema(description = "Session environment") BrowserEnvironment environment,
    @Schema(description = "Instant the session was created") Instant createdAt,
    @Schema(description = "Instant at which the session will be reaped") Instant expiresAt) {}
