package io.browserservice.api.controller;

import io.browserservice.api.dto.ErrorResponse;
import io.browserservice.api.dto.ExecuteRequest;
import io.browserservice.api.dto.ExecuteResponse;
import io.browserservice.api.service.BrowserOperationsService;
import io.browserservice.api.web.CallerContext;
import jakarta.validation.Valid;
import java.util.UUID;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
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
  private final CallerContext callers;

  public ScriptController(BrowserOperationsService service, CallerContext callers) {
    this.service = service;
    this.callers = callers;
  }

  @PostMapping("/execute")
  @Operation(summary = "Execute arbitrary JavaScript", operationId = "executeScript")
  @APIResponses({
    @APIResponse(
        responseCode = "200",
        content = @Content(schema = @Schema(implementation = ExecuteResponse.class))),
    @APIResponse(
        responseCode = "404",
        description = "Session not found",
        content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  })
  public ExecuteResponse execute(@PathVariable UUID id, @Valid @RequestBody ExecuteRequest req) {
    return service.executeScript(id, callers.id(), req);
  }
}
