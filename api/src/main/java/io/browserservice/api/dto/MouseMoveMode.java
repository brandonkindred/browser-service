package io.browserservice.api.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Mouse move destination mode.")
public enum MouseMoveMode {
    OUT_OF_FRAME,
    TO_NON_INTERACTIVE;

    @JsonCreator
    public static MouseMoveMode fromString(String value) {
        if (value == null) {
            return null;
        }
        return MouseMoveMode.valueOf(value.trim().toUpperCase());
    }
}
