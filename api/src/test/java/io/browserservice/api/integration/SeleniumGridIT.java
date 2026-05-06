package io.browserservice.api.integration;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.browserservice.api.dto.CreateSessionRequest;
import io.browserservice.api.dto.NavigateRequest;
import io.browserservice.api.dto.ScreenshotRequest;
import io.browserservice.api.dto.ScreenshotStrategy;
import java.time.Duration;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.BrowserWebDriverContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

/**
 * End-to-end smoke test that spins up a Selenium standalone-chrome container and exercises the
 * canonical create → navigate → screenshot → delete path against the real API stack.
 *
 * <p>Requires a working Docker daemon. Skipped via surefire (included by failsafe's {@code
 * *IT.java} naming convention).
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@org.springframework.test.context.ActiveProfiles("test")
@Testcontainers
class SeleniumGridIT {

  @Container
  static final BrowserWebDriverContainer<?> chrome =
      new BrowserWebDriverContainer<>(DockerImageName.parse("selenium/standalone-chrome:latest"))
          .withStartupTimeout(Duration.ofMinutes(3));

  @DynamicPropertySource
  static void seleniumProps(DynamicPropertyRegistry registry) {
    registry.add("browserservice.selenium.urls", () -> chrome.getSeleniumAddress().toString());
    registry.add("browserservice.appium.urls", () -> "");
    registry.add("browserservice.browserstack.enabled", () -> "false");
  }

  @Autowired private MockMvc mvc;

  @Autowired private ObjectMapper json;

  @Test
  void createNavigateScreenshotDeleteSmokeTest() throws Exception {
    CreateSessionRequest createReq =
        new CreateSessionRequest(
            com.looksee.browser.enums.BrowserType.CHROME,
            com.looksee.browser.enums.BrowserEnvironment.TEST,
            null);

    String createResponse =
        mvc.perform(
                post("/v1/sessions")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(json.writeValueAsString(createReq)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.session_id", notNullValue()))
            .andReturn()
            .getResponse()
            .getContentAsString();

    UUID sessionId = UUID.fromString(json.readTree(createResponse).get("session_id").asText());

    NavigateRequest navigateReq = new NavigateRequest("about:blank", null);
    mvc.perform(
            post("/v1/sessions/" + sessionId + "/navigate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(navigateReq)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status", is("LOADED")));

    ScreenshotRequest screenshotReq = new ScreenshotRequest(ScreenshotStrategy.VIEWPORT, null);
    mvc.perform(
            post("/v1/sessions/" + sessionId + "/screenshot")
                .accept(MediaType.IMAGE_PNG)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(screenshotReq)))
        .andExpect(status().isOk());

    mvc.perform(delete("/v1/sessions/" + sessionId)).andExpect(status().isNoContent());
  }
}
