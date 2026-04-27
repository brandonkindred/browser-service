package io.browserservice.api.ws.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record BinaryHeaderFrame(String type, String id, String mime, long length, String sha256) {

  public static final String TYPE = "binary-header";

  public static BinaryHeaderFrame of(String id, String mime, long length, String sha256) {
    return new BinaryHeaderFrame(TYPE, id, mime, length, sha256);
  }
}
