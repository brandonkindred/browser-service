package io.browserservice.api.dto;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

@Schema(description = "Base64-encoded screenshot response.")
public record ScreenshotBase64Response(
    @Schema(description = "Base64-encoded PNG bytes") String imageBase64,
    @Schema(description = "Image width (px)") int width,
    @Schema(description = "Image height (px)") int height) {}
