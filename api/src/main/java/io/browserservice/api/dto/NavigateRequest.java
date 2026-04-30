package io.browserservice.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Navigate the session to a URL.")
public record NavigateRequest(
        @NotBlank @Schema(description = "Target URL") String url,
        @Min(1) @Schema(description = "Navigation timeout (ms)", nullable = true) Integer timeoutMs) {
}
