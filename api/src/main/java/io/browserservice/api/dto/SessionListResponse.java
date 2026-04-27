package io.browserservice.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "List of active sessions.")
public record SessionListResponse(
    @Schema(description = "All currently open sessions") List<SessionResponse> sessions) {}
