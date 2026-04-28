package io.browserservice.api.config;

import io.browserservice.api.web.CallerIdArgumentResolver;
import java.util.List;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

  private final CallerIdArgumentResolver callerIdArgumentResolver;

  public WebMvcConfig(CallerIdArgumentResolver callerIdArgumentResolver) {
    this.callerIdArgumentResolver = callerIdArgumentResolver;
  }

  @Override
  public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
    resolvers.add(callerIdArgumentResolver);
  }
}
