package io.browserservice.api.web;

import io.browserservice.api.error.CallerUnidentifiedException;
import io.browserservice.api.session.CallerId;
import org.springframework.core.MethodParameter;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

@Component
public class CallerIdArgumentResolver implements HandlerMethodArgumentResolver {

  public static final String CALLER_HEADER = "X-Caller-Id";

  @Override
  public boolean supportsParameter(MethodParameter parameter) {
    return CallerId.class.equals(parameter.getParameterType());
  }

  @Override
  public Object resolveArgument(
      MethodParameter parameter,
      ModelAndViewContainer mavContainer,
      NativeWebRequest webRequest,
      WebDataBinderFactory binderFactory) {
    String raw = webRequest.getHeader(CALLER_HEADER);
    if (raw == null || raw.trim().isEmpty()) {
      throw new CallerUnidentifiedException();
    }
    try {
      return CallerId.parse(raw);
    } catch (IllegalArgumentException e) {
      throw new CallerUnidentifiedException(e.getMessage(), e);
    }
  }
}
