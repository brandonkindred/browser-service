package io.browserservice.api.controller;

import io.browserservice.api.dto.CreateSessionRequest;
import io.browserservice.api.dto.ErrorResponse;
import io.browserservice.api.dto.SessionListResponse;
import io.browserservice.api.dto.SessionResponse;
import io.browserservice.api.service.SessionService;
import io.browserservice.api.web.CallerContext;
import jakarta.validation.Valid;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

// Collection endpoints only (POST /v1/sessions, GET /v1/sessions). Item-level routes
// (/v1/sessions/{id}) live on SessionsItemController — splitting works around a
// quarkus-spring-web path-merge bug where a class-level prefix with no path var
// combined with a method-level /{id} mapping fails to dispatch under RESTEasy.
@RestController
@RequestMapping("/v1/sessions")
@Tag(name = "Sessions", description = "Session lifecycle")
public class SessionsController {

  private final SessionService sessionService;
  private final CallerContext callers;

  public SessionsController(SessionService sessionService, CallerContext callers) {
    this.sessionService = sessionService;
    this.callers = callers;
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
  public SessionResponse create(@Valid @RequestBody CreateSessionRequest req) {
    return sessionService.create(req, callers.id());
  }

  @GetMapping
  @Operation(summary = "List active sessions", operationId = "listSessions")
  @APIResponses({
    @APIResponse(
        responseCode = "200",
        description = "Session list",
        content = @Content(schema = @Schema(implementation = SessionListResponse.class)))
  })
  public SessionListResponse list() {
    return sessionService.list(callers.id());
  }
}
