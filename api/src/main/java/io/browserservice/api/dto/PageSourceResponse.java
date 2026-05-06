package io.browserservice.api.dto;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

@Schema(description = "Current page source (HTML).")
public record PageSourceResponse(
    @Schema(description = "Current URL") String currentUrl,
    @Schema(description = "HTML source of the current page") String source) {}
