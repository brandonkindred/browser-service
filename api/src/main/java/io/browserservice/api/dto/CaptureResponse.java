package io.browserservice.api.dto;

import org.eclipse.microprofile.openapi.annotations.media.Schema;
import java.util.UUID;

@Schema(description = "One-shot capture result.")
public record CaptureResponse(
    @Schema(description = "Capture identifier usable to fetch the bytes later") UUID captureId,
    @Schema(description = "URL after the load completed") String currentUrl,
    @Schema(description = "Screenshot reference") CaptureScreenshotRef screenshot,
    @Schema(description = "Page HTML source (when include_source=true)", nullable = true)
        String source,
    @Schema(description = "Resolved element state (when xpath is set)", nullable = true)
        ElementStateResponse element) {}
