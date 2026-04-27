package io.browserservice.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Uniform error envelope returned for non-2xx responses.")
public record ErrorResponse(@Schema(description = "Error detail payload.") ErrorDetail error) {}
