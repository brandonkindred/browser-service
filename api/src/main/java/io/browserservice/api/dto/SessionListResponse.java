package io.browserservice.api.dto;

import java.util.List;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

@Schema(description = "List of active sessions.")
public record SessionListResponse(
    @Schema(description = "All currently open sessions") List<SessionResponse> sessions) {}
