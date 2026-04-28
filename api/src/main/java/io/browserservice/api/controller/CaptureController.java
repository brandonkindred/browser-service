package io.browserservice.api.controller;

import io.browserservice.api.dto.CaptureRequest;
import io.browserservice.api.dto.CaptureResponse;
import io.browserservice.api.dto.ErrorResponse;
import io.browserservice.api.service.CaptureService;
import io.browserservice.api.session.CallerId;
import io.browserservice.api.session.CaptureScreenshotCache;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/capture")
@Tag(name = "Capture", description = "One-shot navigate + capture + close")
public class CaptureController {

  private final CaptureService service;

  public CaptureController(CaptureService service) {
    this.service = service;
  }

  @PostMapping
  @Operation(summary = "Capture a URL end-to-end in a single call", operationId = "capture")
  @ApiResponses({
    @ApiResponse(
        responseCode = "200",
        content = @Content(schema = @Schema(implementation = CaptureResponse.class))),
    @ApiResponse(
        responseCode = "400",
        description = "Validation failed",
        content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
    @ApiResponse(
        responseCode = "429",
        description = "Concurrent session cap exceeded",
        content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
    @ApiResponse(
        responseCode = "502",
        description = "Upstream unavailable",
        content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  })
  public CaptureResponse capture(@Valid @RequestBody CaptureRequest req, CallerId caller) {
    return service.capture(req, caller);
  }

  @GetMapping(value = "/{captureId}/screenshot", produces = MediaType.IMAGE_PNG_VALUE)
  @Operation(summary = "Fetch a deferred capture screenshot", operationId = "getCaptureScreenshot")
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "PNG bytes"),
    @ApiResponse(
        responseCode = "404",
        description = "Capture not found",
        content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
    @ApiResponse(
        responseCode = "410",
        description = "Capture expired",
        content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  })
  public ResponseEntity<byte[]> getScreenshot(@PathVariable UUID captureId, CallerId caller) {
    CaptureScreenshotCache.CaptureEntry entry = service.fetchScreenshot(captureId, caller);
    return ResponseEntity.ok().contentType(MediaType.IMAGE_PNG).body(entry.pngBytes());
  }
}
