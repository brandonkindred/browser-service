package io.browserservice.api.service;

import io.browserservice.api.config.EngineProperties;
import io.browserservice.api.dto.HubStatus;
import io.browserservice.api.dto.ReadinessResponse;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class ReadinessService {

  private static final Logger log = LoggerFactory.getLogger(ReadinessService.class);
  private static final Duration PROBE_TIMEOUT = Duration.ofSeconds(2);

  private final EngineProperties props;
  private final HttpClient http;

  public ReadinessService(EngineProperties props) {
    this.props = props;
    this.http = HttpClient.newBuilder().connectTimeout(PROBE_TIMEOUT).build();
  }

  public ReadinessResponse probe() {
    List<HubStatus> seleniumHubs = probeAll(props.selenium().urls(), "/status");
    List<HubStatus> appiumServers = probeAll(props.appium().urls(), "/status");

    boolean seleniumReady =
        !seleniumHubs.isEmpty() && seleniumHubs.stream().anyMatch(HubStatus::reachable);
    boolean appiumConfigured = !appiumServers.isEmpty();
    boolean appiumReady =
        !appiumConfigured || appiumServers.stream().anyMatch(HubStatus::reachable);

    String status = seleniumReady && appiumReady ? "ready" : "degraded";
    return new ReadinessResponse(status, seleniumHubs, appiumServers);
  }

  private List<HubStatus> probeAll(String commaSeparated, String statusPath) {
    if (commaSeparated == null || commaSeparated.isBlank()) {
      return List.of();
    }
    List<HubStatus> results = new ArrayList<>();
    for (String raw :
        Arrays.stream(commaSeparated.split(","))
            .map(String::trim)
            .filter(s -> !s.isEmpty())
            .toList()) {
      results.add(new HubStatus(raw, probeOne(raw, statusPath)));
    }
    return results;
  }

  private boolean probeOne(String baseUrl, String statusPath) {
    try {
      URI uri =
          URI.create(
              baseUrl.endsWith("/")
                  ? baseUrl.substring(0, baseUrl.length() - 1) + statusPath
                  : baseUrl + statusPath);
      HttpRequest req = HttpRequest.newBuilder(uri).GET().timeout(PROBE_TIMEOUT).build();
      HttpResponse<Void> resp = http.send(req, HttpResponse.BodyHandlers.discarding());
      return resp.statusCode() >= 200 && resp.statusCode() < 500;
    } catch (Exception e) {
      log.debug("readiness probe failed for {}: {}", baseUrl, e.toString());
      return false;
    }
  }
}
