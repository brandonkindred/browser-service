package io.browserservice.api.controller;

import io.browserservice.api.dto.CreateSessionRequest;
import io.browserservice.api.dto.ErrorResponse;
import io.browserservice.api.dto.SessionListResponse;
import io.browserservice.api.dto.SessionResponse;
import io.browserservice.api.dto.SessionStateResponse;
import io.browserservice.api.service.SessionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
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
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Created",
                    content = @Content(schema = @Schema(implementation = SessionResponse.class))),
            @ApiResponse(responseCode = "400", description = "Validation failed",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "429", description = "Concurrent session cap exceeded",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "502", description = "Upstream hub unavailable",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public SessionResponse create(@Valid @RequestBody CreateSessionRequest req) {
        return sessionService.create(req);
    }

    @GetMapping
    @Operation(summary = "List active sessions", operationId = "listSessions")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Session list",
                    content = @Content(schema = @Schema(implementation = SessionListResponse.class)))
    })
    public SessionListResponse list() {
        return sessionService.list();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Describe a session", operationId = "getSession")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Session state",
                    content = @Content(schema = @Schema(implementation = SessionStateResponse.class))),
            @ApiResponse(responseCode = "404", description = "Session not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public SessionStateResponse get(@PathVariable UUID id) {
        return sessionService.describe(id);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Close a session", operationId = "deleteSession")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Closed"),
            @ApiResponse(responseCode = "404", description = "Session not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public void delete(@PathVariable UUID id) {
        sessionService.close(id);
    }
}
