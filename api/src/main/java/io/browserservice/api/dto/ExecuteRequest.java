package io.browserservice.api.dto;

import jakarta.validation.constraints.NotBlank;
import java.util.List;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

@Schema(description = "Execute arbitrary JavaScript inside the session.")
public record ExecuteRequest(
    @NotBlank @Schema(description = "JavaScript source to run") String script,
    @Schema(description = "Optional positional arguments passed to the script", nullable = true)
        List<Object> args) {}
