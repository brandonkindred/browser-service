package io.browserservice.api.controller;

import io.browserservice.api.dto.CreateSessionRequest;
import io.browserservice.api.dto.ErrorResponse;
import io.browserservice.api.dto.SessionListResponse;
import io.browserservice.api.dto.SessionResponse;
import io.browserservice.api.dto.SessionStateResponse;
import io.browserservice.api.service.SessionService;
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
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/sessions")
@Tag(name = "Sessions", description = "Session lifecycle")
public class SessionsController {

  private final SessionService sessionService;

  public SessionsController(SessionService sessionService) {
    this.sessionService = sessionService;
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(summary = "Create a browser session", operationId = "createSession")
  @APIResponses({
    @APIResponse(
        responseCode = "201",
        description = "Created",
        content = @Content(schema = @Schema(implementation = SessionResponse.class))),
    @APIResponse(
        responseCode = "400",
        description = "Validation failed",
        content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
    @APIResponse(
        responseCode = "429",
        description = "Concurrent session cap exceeded",
        content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
    @APIResponse(
        responseCode = "502",
        description = "Upstream hub unavailable",
        content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  })
  public SessionResponse create(
      @Valid @RequestBody CreateSessionRequest req,
      @RequestHeader(CallerIdParamConverterProvider.HEADER) CallerId caller) {
    return sessionService.create(req, caller);
  }

  @GetMapping
  @Operation(summary = "List active sessions", operationId = "listSessions")
  @APIResponses({
    @APIResponse(
        responseCode = "200",
        description = "Session list",
        content = @Content(schema = @Schema(implementation = SessionListResponse.class)))
  })
  public SessionListResponse list(
      @RequestHeader(CallerIdParamConverterProvider.HEADER) CallerId caller) {
    return sessionService.list(caller);
  }

  @GetMapping("/{id}")
  @Operation(summary = "Describe a session", operationId = "getSession")
  @APIResponses({
    @APIResponse(
        responseCode = "200",
        description = "Session state",
        content = @Content(schema = @Schema(implementation = SessionStateResponse.class))),
    @APIResponse(
        responseCode = "404",
        description = "Session not found",
        content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  })
  public SessionStateResponse get(
      @PathVariable UUID id,
      @RequestHeader(CallerIdParamConverterProvider.HEADER) CallerId caller) {
    return sessionService.describe(id, caller);
  }

  @DeleteMapping("/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @Operation(summary = "Close a session", operationId = "deleteSession")
  @APIResponses({
    @APIResponse(responseCode = "204", description = "Closed"),
    @APIResponse(
        responseCode = "404",
        description = "Session not found",
        content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  })
  public void delete(
      @PathVariable UUID id,
      @RequestHeader(CallerIdParamConverterProvider.HEADER) CallerId caller) {
    sessionService.close(id, caller);
  }
}
