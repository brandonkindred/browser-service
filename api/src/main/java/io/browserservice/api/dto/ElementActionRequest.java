package io.browserservice.api.dto;

import com.looksee.browser.enums.Action;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Perform a desktop action on an element.")
public record ElementActionRequest(
        @NotBlank @Schema(description = "Element handle to act on") String elementHandle,
        @NotNull @Schema(description = "Desktop action to perform") Action action,
        @Schema(description = "Optional input (used by SEND_KEYS)", nullable = true) String input) {
}
