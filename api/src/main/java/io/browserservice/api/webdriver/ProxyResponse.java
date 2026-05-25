package io.browserservice.api.webdriver;

import java.util.Arrays;

/** Response envelope from the Selenium Grid proxy layer. */
public record ProxyResponse(int statusCode, byte[] body, String contentType) {

  /** Defensive-copy constructor. */
  public ProxyResponse(int statusCode, byte[] body, String contentType) {
    this.statusCode = statusCode;
    this.body = body == null ? null : Arrays.copyOf(body, body.length);
    this.contentType = contentType;
  }

  @Override
  public byte[] body() {
    return body == null ? null : Arrays.copyOf(body, body.length);
  }
}
