package io.browserservice.api.dto;

import org.eclipse.microprofile.openapi.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Move the mouse to a predefined destination.")
public record MouseMoveRequest(
    @NotNull @Schema(description = "Mouse move mode") MouseMoveMode mode,
    @Schema(description = "Target x (required when mode == TO_NON_INTERACTIVE)", nullable = true)
        Integer x,
    @Schema(description = "Target y (required when mode == TO_NON_INTERACTIVE)", nullable = true)
        Integer y) {}
