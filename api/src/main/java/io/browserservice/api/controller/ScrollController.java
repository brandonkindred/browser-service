package io.browserservice.api.controller;

import io.browserservice.api.dto.ErrorResponse;
import io.browserservice.api.dto.ScrollOffset;
import io.browserservice.api.dto.ScrollRequest;
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
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/sessions/{id}")
@Tag(name = "Scrolling", description = "Viewport scrolling operations")
public class ScrollController {

  private final BrowserOperationsService service;

  public ScrollController(BrowserOperationsService service) {
    this.service = service;
  }

  @PostMapping("/scroll")
  @Operation(summary = "Scroll the viewport", operationId = "scroll")
  @APIResponses({
    @APIResponse(
        responseCode = "200",
        content = @Content(schema = @Schema(implementation = ScrollOffset.class))),
    @APIResponse(
        responseCode = "400",
        description = "Validation failed",
        content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
    @APIResponse(
        responseCode = "404",
        description = "Session or element handle not found",
        content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  })
  public ScrollOffset scroll(
      @PathVariable UUID id, @RequestHeader("X-Caller-Id") CallerId caller, @Valid @RequestBody ScrollRequest req) {
    return service.scroll(id, caller, req);
  }
}
