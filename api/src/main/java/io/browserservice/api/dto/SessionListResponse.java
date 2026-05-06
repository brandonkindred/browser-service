package io.browserservice.api.dto;

import org.eclipse.microprofile.openapi.annotations.media.Schema;
import java.util.List;

@Schema(description = "List of active sessions.")
public record SessionListResponse(
    @Schema(description = "All currently open sessions") List<SessionResponse> sessions) {}
