package io.browserservice.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Result of a JavaScript execution.")
public record ExecuteResponse(
    @Schema(description = "JSON-serialized result of the script", nullable = true) Object result) {}
