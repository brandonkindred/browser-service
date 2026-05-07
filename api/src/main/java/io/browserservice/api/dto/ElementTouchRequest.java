package io.browserservice.api.dto;

import com.looksee.browser.enums.MobileAction;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

@Schema(description = "Perform a mobile touch gesture on an element.")
public record ElementTouchRequest(
    @NotBlank @Schema(description = "Element handle to act on") String elementHandle,
    @NotNull @Schema(description = "Mobile action to perform") MobileAction action,
    @Schema(description = "Optional input (used by SEND_KEYS)", nullable = true) String input) {}
