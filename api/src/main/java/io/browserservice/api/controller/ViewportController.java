package io.browserservice.api.controller;

import io.browserservice.api.dto.ErrorResponse;
import io.browserservice.api.dto.ViewportStateResponse;
import io.browserservice.api.service.BrowserOperationsService;
import io.browserservice.api.session.CallerId;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/sessions/{id}")
@Tag(name = "Scrolling")
public class ViewportController {

  private final BrowserOperationsService service;

  public ViewportController(BrowserOperationsService service) {
    this.service = service;
  }

  @GetMapping("/viewport")
  @Operation(summary = "Get current viewport size and scroll offset", operationId = "getViewport")
  @ApiResponses({
    @ApiResponse(
        responseCode = "200",
        content = @Content(schema = @Schema(implementation = ViewportStateResponse.class))),
    @ApiResponse(
        responseCode = "404",
        description = "Session not found",
        content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  })
  public ViewportStateResponse viewport(@PathVariable UUID id, CallerId caller) {
    return service.getViewport(id, caller);
  }
}
