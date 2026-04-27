package io.browserservice.api.controller;

import io.browserservice.api.dto.ErrorResponse;
import io.browserservice.api.dto.ExecuteRequest;
import io.browserservice.api.dto.ExecuteResponse;
import io.browserservice.api.service.BrowserOperationsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/sessions/{id}")
@Tag(name = "Script", description = "Arbitrary JavaScript execution")
public class ScriptController {

  private final BrowserOperationsService service;

  public ScriptController(BrowserOperationsService service) {
    this.service = service;
  }

  @PostMapping("/execute")
  @Operation(summary = "Execute arbitrary JavaScript", operationId = "executeScript")
  @ApiResponses({
    @ApiResponse(
        responseCode = "200",
        content = @Content(schema = @Schema(implementation = ExecuteResponse.class))),
    @ApiResponse(
        responseCode = "404",
        description = "Session not found",
        content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  })
  public ExecuteResponse execute(@PathVariable UUID id, @Valid @RequestBody ExecuteRequest req) {
    return service.executeScript(id, req);
  }
}
