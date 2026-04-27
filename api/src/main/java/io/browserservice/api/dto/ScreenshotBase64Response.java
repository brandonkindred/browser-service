package io.browserservice.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Base64-encoded screenshot response.")
public record ScreenshotBase64Response(
    @Schema(description = "Base64-encoded PNG bytes") String imageBase64,
    @Schema(description = "Image width (px)") int width,
    @Schema(description = "Image height (px)") int height) {}
