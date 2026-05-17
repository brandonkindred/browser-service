package io.browserservice.api.controller;

import io.browserservice.api.dto.ElementActionRequest;
import io.browserservice.api.dto.ElementStateResponse;
import io.browserservice.api.dto.ErrorResponse;
import io.browserservice.api.dto.FindElementRequest;
import io.browserservice.api.service.ElementOperationsService;
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
@RequestMapping("/v1/sessions/{id}/element")
@Tag(name = "Elements", description = "Find elements and perform desktop actions")
public class ElementsController {

  private final ElementOperationsService service;
  private final CallerContext callers;

  public ElementsController(ElementOperationsService service, CallerContext callers) {
    this.service = service;
    this.callers = callers;
  }

  @PostMapping("/find")
  @Operation(summary = "Locate an element by XPath", operationId = "findElement")
  @APIResponses({
    @APIResponse(
        responseCode = "200",
        content = @Content(schema = @Schema(implementation = ElementStateResponse.class))),
    @APIResponse(
        responseCode = "404",
        description = "Session not found",
        content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  })
  public ElementStateResponse find(
      @PathVariable UUID id, @Valid @RequestBody FindElementRequest req) {
    return service.find(id, callers.id(), req);
  }

  @PostMapping("/action")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @Operation(
      summary = "Perform a desktop action on an element",
      operationId = "performElementAction")
  @APIResponses({
    @APIResponse(responseCode = "204", description = "Action performed"),
    @APIResponse(
        responseCode = "404",
        description = "Session or element handle not found",
        content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
    @APIResponse(
        responseCode = "409",
        description = "Mobile session (desktop required)",
        content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  })
  public void action(@PathVariable UUID id, @Valid @RequestBody ElementActionRequest req) {
    service.action(id, callers.id(), req);
  }
}
