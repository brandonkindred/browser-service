package io.browserservice.api.dto;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

@Schema(description = "Liveness probe response.")
public record HealthResponse(
    @Schema(
            description = "Always \"ok\" when the process is alive",
            enumeration = {"ok"})
        String status) {

  public static HealthResponse ok() {
    return new HealthResponse("ok");
  }
}
