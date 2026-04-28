package io.browserservice.api.controller;

import io.browserservice.api.dto.ErrorResponse;
import io.browserservice.api.dto.MouseMoveRequest;
import io.browserservice.api.service.BrowserOperationsService;
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
@RequestMapping("/v1/sessions/{id}")
@Tag(name = "Mouse", description = "Desktop mouse operations")
public class MouseController {

  private final BrowserOperationsService service;

  public MouseController(BrowserOperationsService service) {
    this.service = service;
  }

  @PostMapping("/mouse/move")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @Operation(summary = "Move the mouse", operationId = "moveMouse")
  @ApiResponses({
    @ApiResponse(responseCode = "204", description = "Moved"),
    @ApiResponse(
        responseCode = "400",
        description = "Validation failed",
        content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
    @ApiResponse(
        responseCode = "404",
        description = "Session not found",
        content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
    @ApiResponse(
        responseCode = "409",
        description = "Mobile session (desktop required)",
        content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  })
  public void move(
      @PathVariable UUID id, CallerId caller, @Valid @RequestBody MouseMoveRequest req) {
    service.moveMouse(id, caller, req);
  }
}
