package io.browserservice.api;

import io.browserservice.api.config.EngineProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@EnableConfigurationProperties(EngineProperties.class)
public class BrowserServiceApplication {
  public static void main(String[] args) {
    SpringApplication.run(BrowserServiceApplication.class, args);
  }
}
