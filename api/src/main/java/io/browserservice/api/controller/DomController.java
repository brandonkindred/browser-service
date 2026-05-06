package io.browserservice.api.controller;

import io.browserservice.api.dto.DomRemoveRequest;
import io.browserservice.api.dto.ErrorResponse;
import io.browserservice.api.service.BrowserOperationsService;
import io.browserservice.api.session.CallerId;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/sessions/{id}")
@Tag(name = "DOM", description = "Direct DOM manipulation helpers")
public class DomController {

  private final BrowserOperationsService service;

  public DomController(BrowserOperationsService service) {
    this.service = service;
  }

  @PostMapping("/dom/remove")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @Operation(
      summary = "Remove DOM overlays by preset or class name",
      operationId = "removeDomElement")
  @APIResponses({
    @APIResponse(responseCode = "204", description = "Removed (idempotent)"),
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
  public void remove(
      @PathVariable UUID id, @RequestHeader("X-Caller-Id") CallerId caller, @Valid @RequestBody DomRemoveRequest req) {
    service.removeDom(id, caller, req);
  }
}
