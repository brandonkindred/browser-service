package io.browserservice.api.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Screenshot output encoding.")
public enum PngEncoding {
    BINARY,
    BASE64;

    @JsonCreator
    public static PngEncoding fromString(String value) {
        if (value == null) {
            return null;
        }
        return PngEncoding.valueOf(value.trim().toUpperCase());
    }
}
