package io.browserservice.api.controller;

import io.browserservice.api.dto.ElementTouchRequest;
import io.browserservice.api.dto.ErrorResponse;
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
@Tag(name = "Touch", description = "Mobile touch gestures")
public class TouchController {

  private final ElementOperationsService service;
  private final CallerContext callers;

  public TouchController(ElementOperationsService service, CallerContext callers) {
    this.service = service;
    this.callers = callers;
  }

  @PostMapping("/touch")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @Operation(
      summary = "Perform a mobile touch gesture on an element",
      operationId = "performElementTouch")
  @APIResponses({
    @APIResponse(responseCode = "204", description = "Gesture performed"),
    @APIResponse(
        responseCode = "404",
        description = "Session or element handle not found",
        content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
    @APIResponse(
        responseCode = "409",
        description = "Desktop session (mobile required)",
        content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  })
  public void touch(@PathVariable UUID id, @Valid @RequestBody ElementTouchRequest req) {
    service.touch(id, callers.id(), req);
  }
}
