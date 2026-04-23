package io.browserservice.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Derived page health status.")
public record PageStatusResponse(
        @Schema(description = "Current URL") String currentUrl,
        @Schema(description = "Whether the page appears to be a 503 error page") boolean is503) {
}
