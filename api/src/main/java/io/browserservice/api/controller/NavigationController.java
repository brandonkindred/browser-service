package io.browserservice.api.controller;

import io.browserservice.api.dto.ErrorResponse;
import io.browserservice.api.dto.NavigateRequest;
import io.browserservice.api.dto.NavigateResponse;
import io.browserservice.api.dto.PageSourceResponse;
import io.browserservice.api.dto.PageStatusResponse;
import io.browserservice.api.service.BrowserOperationsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
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
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Navigation complete",
                    content = @Content(schema = @Schema(implementation = NavigateResponse.class))),
            @ApiResponse(responseCode = "404", description = "Session not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "502", description = "Upstream error",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public NavigateResponse navigate(@PathVariable UUID id, @Valid @RequestBody NavigateRequest req) {
        return service.navigate(id, req);
    }

    @GetMapping("/source")
    @Operation(summary = "Get the current page source (HTML)", operationId = "getSource")
    @ApiResponses({
            @ApiResponse(responseCode = "200",
                    content = @Content(schema = @Schema(implementation = PageSourceResponse.class))),
            @ApiResponse(responseCode = "404", description = "Session not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public PageSourceResponse source(@PathVariable UUID id) {
        return service.getSource(id);
    }

    @GetMapping("/status")
    @Operation(summary = "Get derived page status (current URL + 503 detection)",
            operationId = "getPageStatus")
    @ApiResponses({
            @ApiResponse(responseCode = "200",
                    content = @Content(schema = @Schema(implementation = PageStatusResponse.class))),
            @ApiResponse(responseCode = "404", description = "Session not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public PageStatusResponse status(@PathVariable UUID id) {
        return service.getStatus(id);
    }
}
