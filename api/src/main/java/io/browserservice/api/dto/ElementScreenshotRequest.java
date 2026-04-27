package io.browserservice.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Request to capture a screenshot of a single element.")
public record ElementScreenshotRequest(
    @NotBlank @Schema(description = "Opaque handle previously returned by /element/find")
        String elementHandle,
    @Schema(description = "Response encoding (defaults to BINARY)", nullable = true)
        PngEncoding encoding) {}
