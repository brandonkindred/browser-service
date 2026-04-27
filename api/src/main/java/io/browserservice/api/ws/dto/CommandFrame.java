package io.browserservice.api.ws.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.JsonNode;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record CommandFrame(String type, String id, String op, JsonNode params) {

  public static final String TYPE = "command";
}
