package io.browserservice.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Remove one or more DOM overlays by preset or class name.")
public record DomRemoveRequest(
        @NotNull @Schema(description = "Preset identifier") DomRemovePreset preset,
        @Schema(description = "Class name (required when preset == BY_CLASS)", nullable = true) String value) {
}
