package io.browserservice.api.dto;

import com.looksee.browser.enums.BrowserEnvironment;
import com.looksee.browser.enums.BrowserType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Request to create a new browser session.")
public record CreateSessionRequest(
    @NotNull
        @Schema(
            description = "Browser type",
            allowableValues = {"CHROME", "FIREFOX", "SAFARI", "IE", "ANDROID", "IOS"},
            example = "CHROME")
        BrowserType browserType,
    @NotNull
        @Schema(
            description = "Session environment",
            allowableValues = {"TEST", "DISCOVERY"},
            example = "TEST")
        BrowserEnvironment environment,
    @Min(1)
        @Schema(
            description =
                "Optional idle TTL override (seconds). Falls back to service default when omitted.",
            nullable = true)
        Integer idleTtlSeconds) {}
