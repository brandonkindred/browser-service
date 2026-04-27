package io.browserservice.api.controller;

import io.browserservice.api.dto.AlertRespondRequest;
import io.browserservice.api.dto.AlertStateResponse;
import io.browserservice.api.dto.ErrorResponse;
import io.browserservice.api.service.AlertService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/sessions/{id}")
@Tag(name = "Alerts", description = "Browser alert detection and response")
public class AlertsController {

  private final AlertService service;

  public AlertsController(AlertService service) {
    this.service = service;
  }

  @GetMapping("/alert")
  @Operation(summary = "Detect the current alert, if any", operationId = "getAlert")
  @ApiResponses({
    @ApiResponse(
        responseCode = "200",
        content = @Content(schema = @Schema(implementation = AlertStateResponse.class))),
    @ApiResponse(
        responseCode = "404",
        description = "Session not found",
        content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  })
  public AlertStateResponse getAlert(@PathVariable UUID id) {
    return service.getAlert(id);
  }

  @PostMapping("/alert/respond")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @Operation(summary = "Respond to the current alert", operationId = "respondToAlert")
  @ApiResponses({
    @ApiResponse(responseCode = "204", description = "Alert handled (idempotent)"),
    @ApiResponse(
        responseCode = "404",
        description = "Session not found",
        content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  })
  public void respond(@PathVariable UUID id, @Valid @RequestBody AlertRespondRequest req) {
    service.respond(id, req);
  }
}
