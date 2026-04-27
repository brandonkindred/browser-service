package io.browserservice.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Map;

@Schema(description = "Error detail envelope.")
public record ErrorDetail(
    @Schema(description = "Machine-readable error code (stable).", example = "session_not_found")
        String code,
    @Schema(description = "Human-readable error message.") String message,
    @Schema(description = "Optional structured details keyed by field name.")
        Map<String, Object> details,
    @Schema(description = "Correlating request identifier echoed back to the caller.")
        String requestId) {}
