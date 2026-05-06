package io.browserservice.api.dto;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

@Schema(description = "Current viewport size and scroll offset.")
public record ViewportStateResponse(
    @Schema(description = "Viewport size") Viewport viewport,
    @Schema(description = "Scroll offset") ScrollOffset scrollOffset) {}
