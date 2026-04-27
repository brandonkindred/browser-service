package io.browserservice.api.dto;

import com.looksee.browser.enums.MobileAction;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Perform a mobile touch gesture on an element.")
public record ElementTouchRequest(
    @NotBlank @Schema(description = "Element handle to act on") String elementHandle,
    @NotNull @Schema(description = "Mobile action to perform") MobileAction action,
    @Schema(description = "Optional input (used by SEND_KEYS)", nullable = true) String input) {}
