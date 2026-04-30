package io.browserservice.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Map;

@Schema(description = "Resolved element state including handle, visibility, attributes, and rect.")
public record ElementStateResponse(
        @Schema(description = "Opaque handle for subsequent calls") String elementHandle,
        @Schema(description = "Whether the element exists in the DOM") boolean found,
        @Schema(description = "Whether the element is displayed to the user") boolean displayed,
        @Schema(description = "Flattened attribute map") Map<String, String> attributes,
        @Schema(description = "Element bounding rect", nullable = true) Rect rect) {
}
