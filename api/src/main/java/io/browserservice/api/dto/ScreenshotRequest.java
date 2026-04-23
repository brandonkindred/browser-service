package io.browserservice.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Request to capture a page screenshot.")
public record ScreenshotRequest(
        @NotNull @Schema(description = "Capture strategy") ScreenshotStrategy strategy,
        @Schema(description = "Response encoding (defaults to BINARY)", nullable = true) PngEncoding encoding) {
}
