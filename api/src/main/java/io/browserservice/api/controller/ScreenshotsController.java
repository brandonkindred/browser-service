package io.browserservice.api.controller;

import io.browserservice.api.dto.ElementScreenshotRequest;
import io.browserservice.api.dto.ErrorResponse;
import io.browserservice.api.dto.PngEncoding;
import io.browserservice.api.dto.ScreenshotBase64Response;
import io.browserservice.api.dto.ScreenshotRequest;
import io.browserservice.api.service.BrowserOperationsService;
import io.browserservice.api.service.ElementOperationsService;
import io.browserservice.api.session.CallerId;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.Base64;
import java.util.UUID;
import javax.imageio.ImageIO;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/sessions/{id}")
@Tag(name = "Screenshots", description = "Page and element screenshots")
public class ScreenshotsController {

  private final BrowserOperationsService browserOps;
  private final ElementOperationsService elementOps;

  public ScreenshotsController(
      BrowserOperationsService browserOps, ElementOperationsService elementOps) {
    this.browserOps = browserOps;
    this.elementOps = elementOps;
  }

  static {
    // Eagerly load the PNG writer to avoid ServiceLoader edge cases under virtual threads.
    ImageIO.scanForPlugins();
  }

  @PostMapping(
      value = "/screenshot",
      produces = {MediaType.IMAGE_PNG_VALUE, MediaType.APPLICATION_JSON_VALUE})
  @Operation(summary = "Capture a page screenshot", operationId = "captureScreenshot")
  @APIResponses({
    @APIResponse(responseCode = "200", description = "PNG bytes or base64 JSON"),
    @APIResponse(
        responseCode = "400",
        description = "Validation failed",
        content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
    @APIResponse(
        responseCode = "404",
        description = "Session not found",
        content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  })
  public ResponseEntity<?> capture(
      @PathVariable UUID id, @RequestHeader("X-Caller-Id") CallerId caller, @Valid @RequestBody ScreenshotRequest req) {
    byte[] pngBytes = browserOps.pageScreenshot(id, caller, req.strategy());
    return respond(pngBytes, req.encoding());
  }

  @PostMapping(
      value = "/element/screenshot",
      produces = {MediaType.IMAGE_PNG_VALUE, MediaType.APPLICATION_JSON_VALUE})
  @Operation(
      summary = "Capture a screenshot of a single element",
      operationId = "captureElementScreenshot")
  @APIResponses({
    @APIResponse(responseCode = "200", description = "PNG bytes or base64 JSON"),
    @APIResponse(
        responseCode = "404",
        description = "Session or element handle not found",
        content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  })
  public ResponseEntity<?> captureElement(
      @PathVariable UUID id, @RequestHeader("X-Caller-Id") CallerId caller, @Valid @RequestBody ElementScreenshotRequest req) {
    byte[] pngBytes = elementOps.elementScreenshot(id, caller, req);
    return respond(pngBytes, req.encoding());
  }

  private ResponseEntity<?> respond(byte[] pngBytes, PngEncoding encoding) {
    if (encoding == PngEncoding.BASE64) {
      int[] wh = readDimensions(pngBytes);
      return ResponseEntity.ok(
          new ScreenshotBase64Response(Base64.getEncoder().encodeToString(pngBytes), wh[0], wh[1]));
    }
    return ResponseEntity.ok().contentType(MediaType.IMAGE_PNG).body(pngBytes);
  }

  private static int[] readDimensions(byte[] pngBytes) {
    try (var in = new java.io.ByteArrayInputStream(pngBytes)) {
      var image = ImageIO.read(in);
      if (image == null) {
        return new int[] {0, 0};
      }
      return new int[] {image.getWidth(), image.getHeight()};
    } catch (Exception e) {
      return new int[] {0, 0};
    }
  }
}
