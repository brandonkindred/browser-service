package io.browserservice.api.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

@Schema(description = "DOM removal preset.")
public enum DomRemovePreset {
  DRIFT_CHAT,
  GDPR_MODAL,
  GDPR,
  BY_CLASS;

  @JsonCreator
  public static DomRemovePreset fromString(String value) {
    if (value == null) {
      return null;
    }
    return DomRemovePreset.valueOf(value.trim().toUpperCase());
  }
}
