package io.browserservice.api.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

@Schema(description = "Scroll mode.")
public enum ScrollMode {
  TO_TOP,
  TO_BOTTOM,
  TO_ELEMENT,
  TO_ELEMENT_CENTERED,
  DOWN_PERCENT,
  DOWN_FULL;

  @JsonCreator
  public static ScrollMode fromString(String value) {
    if (value == null) {
      return null;
    }
    return ScrollMode.valueOf(value.trim().toUpperCase());
  }
}
