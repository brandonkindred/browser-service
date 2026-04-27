package io.browserservice.api.error;

import java.util.Map;
import org.springframework.http.HttpStatus;

public class ElementHandleNotFoundException extends ApiException {
  public ElementHandleNotFoundException(String handle) {
    super(
        "element_handle_not_found",
        HttpStatus.NOT_FOUND,
        "element handle not found: " + handle,
        Map.of("element_handle", handle));
  }
}
