package io.browserservice.api.dto;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

@Schema(description = "Current page scroll offset in CSS pixels.")
public record ScrollOffset(
    @Schema(description = "Horizontal scroll offset", example = "0") int x,
    @Schema(description = "Vertical scroll offset", example = "1200") int y) {}
