package io.browserservice.api.dto;

import jakarta.validation.constraints.NotBlank;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

@Schema(description = "Locate a single element by XPath.")
public record FindElementRequest(
    @NotBlank @Schema(description = "XPath expression to resolve") String xpath) {}
