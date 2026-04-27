package io.browserservice.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Liveness probe response.")
public record HealthResponse(
    @Schema(
            description = "Always \"ok\" when the process is alive",
            allowableValues = {"ok"})
        String status) {

  public static HealthResponse ok() {
    return new HealthResponse("ok");
  }
}
