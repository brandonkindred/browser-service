package io.browserservice.api.controller;

import io.browserservice.api.dto.ElementTouchRequest;
import io.browserservice.api.dto.ErrorResponse;
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
@Tag(name = "Touch", description = "Mobile touch gestures")
public class TouchController {

  private final ElementOperationsService service;

  public TouchController(ElementOperationsService service) {
    this.service = service;
  }

  @PostMapping("/touch")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @Operation(
      summary = "Perform a mobile touch gesture on an element",
      operationId = "performElementTouch")
  @ApiResponses({
    @ApiResponse(responseCode = "204", description = "Gesture performed"),
    @ApiResponse(
        responseCode = "404",
        description = "Session or element handle not found",
        content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
    @ApiResponse(
        responseCode = "409",
        description = "Desktop session (mobile required)",
        content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  })
  public void touch(
      @PathVariable UUID id, CallerId caller, @Valid @RequestBody ElementTouchRequest req) {
    service.touch(id, caller, req);
  }
}
