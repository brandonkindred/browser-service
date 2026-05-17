package io.browserservice.api.controller;

import io.browserservice.api.dto.ErrorResponse;
import io.browserservice.api.dto.MouseMoveRequest;
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
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/sessions/{id}")
@Tag(name = "Mouse", description = "Desktop mouse operations")
public class MouseController {

  private final BrowserOperationsService service;
  private final CallerContext callers;

  public MouseController(BrowserOperationsService service, CallerContext callers) {
    this.service = service;
    this.callers = callers;
  }

  @PostMapping("/mouse/move")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @Operation(summary = "Move the mouse", operationId = "moveMouse")
  @APIResponses({
    @APIResponse(responseCode = "204", description = "Moved"),
    @APIResponse(
        responseCode = "400",
        description = "Validation failed",
        content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
    @APIResponse(
        responseCode = "404",
        description = "Session not found",
        content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
    @APIResponse(
        responseCode = "409",
        description = "Mobile session (desktop required)",
        content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  })
  public void move(@PathVariable UUID id, @Valid @RequestBody MouseMoveRequest req) {
    service.moveMouse(id, callers.id(), req);
  }
}
