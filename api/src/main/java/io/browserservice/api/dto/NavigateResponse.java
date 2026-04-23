package io.browserservice.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Result of a navigate call.")
public record NavigateResponse(
        @Schema(description = "Current URL after navigation") String currentUrl,
        @Schema(description = "Outcome of the navigation attempt") NavigateStatus status) {
}
