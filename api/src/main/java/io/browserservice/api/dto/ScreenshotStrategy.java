package io.browserservice.api.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

@Schema(description = "Screenshot capture strategy.")
public enum ScreenshotStrategy {
  VIEWPORT,
  FULL_PAGE_SHUTTERBUG,
  FULL_PAGE_ASHOT,
  FULL_PAGE_SHUTTERBUG_PAUSED;

  @JsonCreator
  public static ScreenshotStrategy fromString(String value) {
    if (value == null) {
      return null;
    }
    return ScreenshotStrategy.valueOf(value.trim().toUpperCase());
  }
}
