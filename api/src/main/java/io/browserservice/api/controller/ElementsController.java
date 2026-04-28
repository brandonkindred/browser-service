package io.browserservice.api.controller;

import io.browserservice.api.dto.ElementActionRequest;
import io.browserservice.api.dto.ElementStateResponse;
import io.browserservice.api.dto.ErrorResponse;
import io.browserservice.api.dto.FindElementRequest;
import io.browserservice.api.service.ElementOperationsService;
import io.browserservice.api.session.CallerId;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
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

  public ElementsController(ElementOperationsService service) {
    this.service = service;
  }

  @PostMapping("/find")
  @Operation(summary = "Locate an element by XPath", operationId = "findElement")
  @ApiResponses({
    @ApiResponse(
        responseCode = "200",
        content = @Content(schema = @Schema(implementation = ElementStateResponse.class))),
    @ApiResponse(
        responseCode = "404",
        description = "Session not found",
        content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  })
  public ElementStateResponse find(
      @PathVariable UUID id, CallerId caller, @Valid @RequestBody FindElementRequest req) {
    return service.find(id, caller, req);
  }

  @PostMapping("/action")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @Operation(
      summary = "Perform a desktop action on an element",
      operationId = "performElementAction")
  @ApiResponses({
    @ApiResponse(responseCode = "204", description = "Action performed"),
    @ApiResponse(
        responseCode = "404",
        description = "Session or element handle not found",
        content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
    @ApiResponse(
        responseCode = "409",
        description = "Mobile session (desktop required)",
        content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  })
  public void action(
      @PathVariable UUID id, CallerId caller, @Valid @RequestBody ElementActionRequest req) {
    service.action(id, caller, req);
  }
}
