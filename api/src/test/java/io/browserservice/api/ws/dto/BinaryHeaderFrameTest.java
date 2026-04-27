package io.browserservice.api.ws.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import org.junit.jupiter.api.Test;

class BinaryHeaderFrameTest {

  private final ObjectMapper mapper =
      new ObjectMapper()
          .setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE)
          .findAndRegisterModules();

  @Test
  void serializesWithSnakeCaseKeysMatchingTheWireProtocol() throws Exception {
    BinaryHeaderFrame frame = BinaryHeaderFrame.of("c-1", "image/png", 1234, "abcd");
    String json = mapper.writeValueAsString(frame);
    assertThat(json)
        .contains("\"type\":\"binary-header\"")
        .contains("\"id\":\"c-1\"")
        .contains("\"mime\":\"image/png\"")
        .contains("\"length\":1234")
        .contains("\"sha256\":\"abcd\"");
  }
}
