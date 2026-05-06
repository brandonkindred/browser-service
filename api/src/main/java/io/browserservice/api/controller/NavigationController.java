package io.browserservice.api.controller;

import io.browserservice.api.dto.ErrorResponse;
import io.browserservice.api.dto.NavigateRequest;
import io.browserservice.api.dto.NavigateResponse;
import io.browserservice.api.dto.PageSourceResponse;
import io.browserservice.api.dto.PageStatusResponse;
import io.browserservice.api.service.BrowserOperationsService;
import io.browserservice.api.session.CallerId;
import io.browserservice.api.web.CallerIdParamConverterProvider;
import jakarta.validation.Valid;
import java.util.UUID;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/sessions/{id}")
@Tag(name = "Navigation", description = "Page navigation and source")
public class NavigationController {

  private final BrowserOperationsService service;

  public NavigationController(BrowserOperationsService service) {
    this.service = service;
  }

  @PostMapping("/navigate")
  @Operation(summary = "Navigate to a URL", operationId = "navigate")
  @APIResponses({
    @APIResponse(
        responseCode = "200",
        description = "Navigation complete",
        content = @Content(schema = @Schema(implementation = NavigateResponse.class))),
    @APIResponse(
        responseCode = "404",
        description = "Session not found",
        content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
    @APIResponse(
        responseCode = "502",
        description = "Upstream error",
        content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  })
  public NavigateResponse navigate(
      @PathVariable UUID id,
      @RequestHeader(CallerIdParamConverterProvider.HEADER) CallerId caller,
      @Valid @RequestBody NavigateRequest req) {
    return service.navigate(id, caller, req);
  }

  @GetMapping("/source")
  @Operation(summary = "Get the current page source (HTML)", operationId = "getSource")
  @APIResponses({
    @APIResponse(
        responseCode = "200",
        content = @Content(schema = @Schema(implementation = PageSourceResponse.class))),
    @APIResponse(
        responseCode = "404",
        description = "Session not found",
        content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  })
  public PageSourceResponse source(
      @PathVariable UUID id,
      @RequestHeader(CallerIdParamConverterProvider.HEADER) CallerId caller) {
    return service.getSource(id, caller);
  }

  @GetMapping("/status")
  @Operation(
      summary = "Get derived page status (current URL + 503 detection)",
      operationId = "getPageStatus")
  @APIResponses({
    @APIResponse(
        responseCode = "200",
        content = @Content(schema = @Schema(implementation = PageStatusResponse.class))),
    @APIResponse(
        responseCode = "404",
        description = "Session not found",
        content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  })
  public PageStatusResponse status(
      @PathVariable UUID id,
      @RequestHeader(CallerIdParamConverterProvider.HEADER) CallerId caller) {
    return service.getStatus(id, caller);
  }
}
