package io.browserservice.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Scroll the viewport.")
public record ScrollRequest(
    @NotNull @Schema(description = "Scroll mode") ScrollMode mode,
    @Schema(
            description = "Element handle (required for TO_ELEMENT and TO_ELEMENT_CENTERED)",
            nullable = true)
        String elementHandle,
    @Schema(
            description = "Percent (0-1) of viewport to scroll down (required for DOWN_PERCENT)",
            nullable = true)
        Double percent) {}
