package io.browserservice.api.controller;

import io.browserservice.api.dto.HealthResponse;
import io.browserservice.api.dto.ReadinessResponse;
import io.browserservice.api.service.ReadinessService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(name = "Ops", description = "Health, readiness, metrics")
public class OpsController {

  private final ReadinessService readinessService;

  public OpsController(ReadinessService readinessService) {
    this.readinessService = readinessService;
  }

  @GetMapping("/healthz")
  @Operation(summary = "Liveness probe", operationId = "healthz")
  public HealthResponse healthz() {
    return HealthResponse.ok();
  }

  @GetMapping("/readyz")
  @Operation(summary = "Readiness probe", operationId = "readyz")
  public ResponseEntity<ReadinessResponse> readyz() {
    ReadinessResponse body = readinessService.probe();
    HttpStatus status =
        "ready".equals(body.status()) ? HttpStatus.OK : HttpStatus.SERVICE_UNAVAILABLE;
    return ResponseEntity.status(status).body(body);
  }
}
