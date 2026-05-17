package io.browserservice.api.controller;

import io.browserservice.api.dto.CaptureRequest;
import io.browserservice.api.dto.CaptureResponse;
import io.browserservice.api.dto.ErrorResponse;
import io.browserservice.api.service.CaptureService;
import io.browserservice.api.session.CaptureScreenshotCache;
import io.browserservice.api.web.CallerContext;
import jakarta.validation.Valid;
import java.util.UUID;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
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
  private final CallerContext callers;

  public CaptureController(CaptureService service, CallerContext callers) {
    this.service = service;
    this.callers = callers;
  }

  @PostMapping
  @Operation(summary = "Capture a URL end-to-end in a single call", operationId = "capture")
  @APIResponses({
    @APIResponse(
        responseCode = "200",
        content = @Content(schema = @Schema(implementation = CaptureResponse.class))),
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
        description = "Upstream unavailable",
        content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  })
  public CaptureResponse capture(@Valid @RequestBody CaptureRequest req) {
    return service.capture(req, callers.id());
  }

  @GetMapping(value = "/{captureId}/screenshot", produces = MediaType.IMAGE_PNG_VALUE)
  @Operation(summary = "Fetch a deferred capture screenshot", operationId = "getCaptureScreenshot")
  @APIResponses({
    @APIResponse(responseCode = "200", description = "PNG bytes"),
    @APIResponse(
        responseCode = "404",
        description = "Capture not found",
        content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
    @APIResponse(
        responseCode = "410",
        description = "Capture expired",
        content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  })
  public ResponseEntity<byte[]> getScreenshot(@PathVariable UUID captureId) {
    CaptureScreenshotCache.CaptureEntry entry = service.fetchScreenshot(captureId, callers.id());
    return ResponseEntity.ok().contentType(MediaType.IMAGE_PNG).body(entry.pngBytes());
  }
}
