package io.browserservice.api.dto;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

@Schema(description = "Derived page health status.")
public record PageStatusResponse(
    @Schema(description = "Current URL") String currentUrl,
    @Schema(description = "Whether the page appears to be a 503 error page") boolean is503) {}
