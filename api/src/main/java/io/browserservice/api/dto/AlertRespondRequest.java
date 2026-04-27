package io.browserservice.api.dto;

import com.looksee.browser.enums.AlertChoice;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Respond to the current browser alert.")
public record AlertRespondRequest(
    @NotNull @Schema(description = "Alert choice") AlertChoice choice,
    @Schema(description = "Optional input for prompt alerts", nullable = true) String input) {}
