package io.browserservice.api.dto;

import com.looksee.browser.enums.BrowserEnvironment;
import com.looksee.browser.enums.BrowserType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Schema(description = "One-shot navigate + capture + close request.")
public record CaptureRequest(
        @NotBlank @Schema(description = "Target URL") String url,
        @NotNull @Schema(description = "Browser type") BrowserType browserType,
        @Schema(description = "Environment (defaults to TEST)", nullable = true) BrowserEnvironment environment,
        @Schema(description = "Screenshot strategy (defaults to VIEWPORT)", nullable = true) ScreenshotStrategy strategy,
        @Schema(description = "Response encoding (BINARY emits an href, BASE64 inlines the bytes).",
                nullable = true) PngEncoding encoding,
        @Schema(description = "Optional XPath to locate and describe a single element", nullable = true) String xpath,
        @Schema(description = "Include the page HTML source in the response", nullable = true) Boolean includeSource) {
}
