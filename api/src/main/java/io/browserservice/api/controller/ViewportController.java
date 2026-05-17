package io.browserservice.api.controller;

import io.browserservice.api.dto.ErrorResponse;
import io.browserservice.api.dto.ViewportStateResponse;
import io.browserservice.api.service.BrowserOperationsService;
import io.browserservice.api.web.CallerContext;
import java.util.UUID;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/sessions/{id}")
@Tag(name = "Scrolling")
public class ViewportController {

  private final BrowserOperationsService service;
  private final CallerContext callers;

  public ViewportController(BrowserOperationsService service, CallerContext callers) {
    this.service = service;
    this.callers = callers;
  }

  @GetMapping("/viewport")
  @Operation(summary = "Get current viewport size and scroll offset", operationId = "getViewport")
  @APIResponses({
    @APIResponse(
        responseCode = "200",
        content = @Content(schema = @Schema(implementation = ViewportStateResponse.class))),
    @APIResponse(
        responseCode = "404",
        description = "Session not found",
        content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  })
  public ViewportStateResponse viewport(@PathVariable UUID id) {
    return service.getViewport(id, callers.id());
  }
}
