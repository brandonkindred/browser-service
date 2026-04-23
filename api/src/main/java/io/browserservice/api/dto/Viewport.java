package io.browserservice.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Browser viewport size in CSS pixels.")
public record Viewport(
        @Schema(description = "Viewport width (px)", example = "1280") int width,
        @Schema(description = "Viewport height (px)", example = "720") int height) {
}
