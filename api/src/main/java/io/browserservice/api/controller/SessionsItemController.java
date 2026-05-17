package io.browserservice.api.controller;

import io.browserservice.api.dto.ErrorResponse;
import io.browserservice.api.dto.SessionStateResponse;
import io.browserservice.api.service.SessionService;
import io.browserservice.api.web.CallerContext;
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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Item-level routes ({@code GET}, {@code DELETE} on {@code /v1/sessions/{id}}). See {@link
 * SessionsController} for why item-level mappings live in a separate class.
 */
@RestController
@RequestMapping("/v1/sessions/{id}")
@Tag(name = "Sessions", description = "Session lifecycle")
public class SessionsItemController {

  private final SessionService sessionService;
  private final CallerContext callers;

  /** Constructs the controller with its session service collaborator. */
  public SessionsItemController(SessionService sessionService, CallerContext callers) {
    this.sessionService = sessionService;
    this.callers = callers;
  }

  /** Returns the current state of the session owned by the caller. */
  @GetMapping
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
  public SessionStateResponse get(@PathVariable UUID id) {
    return sessionService.describe(id, callers.id());
  }

  /** Closes the session owned by the caller. */
  @DeleteMapping
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @Operation(summary = "Close a session", operationId = "deleteSession")
  @APIResponses({
    @APIResponse(responseCode = "204", description = "Closed"),
    @APIResponse(
        responseCode = "404",
        description = "Session not found",
        content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  })
  public void delete(@PathVariable UUID id) {
    sessionService.close(id, callers.id());
  }
}
