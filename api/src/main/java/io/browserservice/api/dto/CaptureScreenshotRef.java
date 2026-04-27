package io.browserservice.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(
    description = "Screenshot reference — either inlined base64 or an href to fetch the PNG bytes.")
public record CaptureScreenshotRef(
    @Schema(description = "Base64-encoded PNG (present when encoding=BASE64)", nullable = true)
        String imageBase64,
    @Schema(
            description = "GET href to fetch the PNG bytes (present when encoding=BINARY)",
            nullable = true)
        String href,
    @Schema(description = "Width in px") int width,
    @Schema(description = "Height in px") int height) {}
