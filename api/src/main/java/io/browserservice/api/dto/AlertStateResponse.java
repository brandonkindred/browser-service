package io.browserservice.api.dto;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

@Schema(description = "Browser alert state.")
public record AlertStateResponse(
    @Schema(description = "Whether an alert is currently visible") boolean present,
    @Schema(description = "Alert text when present", nullable = true) String text) {}
