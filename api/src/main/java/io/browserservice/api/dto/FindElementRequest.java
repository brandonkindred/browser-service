package io.browserservice.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Locate a single element by XPath.")
public record FindElementRequest(
        @NotBlank @Schema(description = "XPath expression to resolve") String xpath) {
}
