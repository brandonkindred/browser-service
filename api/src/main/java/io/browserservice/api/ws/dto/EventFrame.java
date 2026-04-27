package io.browserservice.api.ws.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record EventFrame(String type, String kind, Object data) {

  public static final String TYPE = "event";

  public static EventFrame of(String kind, Object data) {
    return new EventFrame(TYPE, kind, data);
  }
}
