package io.browserservice.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Current viewport size and scroll offset.")
public record ViewportStateResponse(
    @Schema(description = "Viewport size") Viewport viewport,
    @Schema(description = "Scroll offset") ScrollOffset scrollOffset) {}
