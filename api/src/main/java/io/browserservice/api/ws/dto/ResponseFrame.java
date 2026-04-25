package io.browserservice.api.ws.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.browserservice.api.dto.ErrorDetail;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ResponseFrame(String type, String id, boolean ok, Object result, ErrorDetail error) {

    public static final String TYPE = "response";

    public static ResponseFrame success(String id, Object result) {
        return new ResponseFrame(TYPE, id, true, result, null);
    }

    public static ResponseFrame failure(String id, ErrorDetail error) {
        return new ResponseFrame(TYPE, id, false, null, error);
    }
}
