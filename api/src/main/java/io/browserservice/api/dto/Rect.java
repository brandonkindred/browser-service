package io.browserservice.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Axis-aligned bounding rectangle in CSS pixels.")
public record Rect(
    @Schema(description = "Left offset") int x,
    @Schema(description = "Top offset") int y,
    @Schema(description = "Rect width") int width,
    @Schema(description = "Rect height") int height) {}
