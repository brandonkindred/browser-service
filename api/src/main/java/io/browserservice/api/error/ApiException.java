package io.browserservice.api.error;

import java.util.Map;
import org.springframework.http.HttpStatus;

public abstract class ApiException extends RuntimeException {

  private final String code;
  private final HttpStatus httpStatus;
  private final Map<String, Object> details;

  protected ApiException(String code, HttpStatus httpStatus, String message) {
    this(code, httpStatus, message, null, null);
  }

  protected ApiException(
      String code, HttpStatus httpStatus, String message, Map<String, Object> details) {
    this(code, httpStatus, message, details, null);
  }

  protected ApiException(
      String code,
      HttpStatus httpStatus,
      String message,
      Map<String, Object> details,
      Throwable cause) {
    super(message, cause);
    this.code = code;
    this.httpStatus = httpStatus;
    this.details = details;
  }

  public String getCode() {
    return code;
  }

  public HttpStatus getHttpStatus() {
    return httpStatus;
  }

  public Map<String, Object> getDetails() {
    return details;
  }
}
