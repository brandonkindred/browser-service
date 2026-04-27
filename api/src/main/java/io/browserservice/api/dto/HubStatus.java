package io.browserservice.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Reachability probe for a single upstream URL.")
public record HubStatus(
    @Schema(description = "URL that was probed") String url,
    @Schema(description = "Whether the probe succeeded") boolean reachable) {}
