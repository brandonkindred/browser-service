package io.browserservice.api.dto;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

@Schema(description = "Browser viewport size in CSS pixels.")
public record Viewport(
    @Schema(description = "Viewport width (px)", example = "1280") int width,
    @Schema(description = "Viewport height (px)", example = "720") int height) {}
