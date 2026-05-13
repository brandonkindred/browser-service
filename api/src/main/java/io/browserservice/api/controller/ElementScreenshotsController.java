package io.browserservice.api.controller;

import io.browserservice.api.dto.ElementScreenshotRequest;
import io.browserservice.api.dto.ErrorResponse;
import io.browserservice.api.service.ElementOperationsService;
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
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Element-level screenshot lives in its own class so it does not co-habit a {@code
 * quarkus-spring-web} {@code @RequestMapping} with the page-level variant; see {@link
 * ScreenshotsController} for the workaround rationale.
 */
@RestController
@RequestMapping("/v1/sessions/{id}/element/screenshot")
@Tag(name = "Screenshots", description = "Page and element screenshots")
public class ElementScreenshotsController {

  private final ElementOperationsService elementOps;

  /** Constructs the controller with its element operations collaborator. */
  public ElementScreenshotsController(ElementOperationsService elementOps) {
    this.elementOps = elementOps;
  }

  /** Captures a single-element screenshot using a handle returned by {@code /element/find}. */
  @PostMapping(produces = {MediaType.IMAGE_PNG_VALUE, MediaType.APPLICATION_JSON_VALUE})
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
      @PathVariable UUID id,
      @RequestHeader(CallerIdParamConverterProvider.HEADER) CallerId caller,
      @Valid @RequestBody ElementScreenshotRequest req) {
    byte[] pngBytes = elementOps.elementScreenshot(id, caller, req);
    return ScreenshotsController.respond(pngBytes, req.encoding());
  }
}
