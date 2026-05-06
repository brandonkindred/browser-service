package io.browserservice.api.dto;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

@Schema(description = "Reachability probe for a single upstream URL.")
public record HubStatus(
    @Schema(description = "URL that was probed") String url,
    @Schema(description = "Whether the probe succeeded") boolean reachable) {}
