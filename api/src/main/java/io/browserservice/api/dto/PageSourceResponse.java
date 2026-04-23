package io.browserservice.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Current page source (HTML).")
public record PageSourceResponse(
        @Schema(description = "Current URL") String currentUrl,
        @Schema(description = "HTML source of the current page") String source) {
}
