package io.browserservice.api.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.browserservice.api.error.UpstreamUnavailableException;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;

class ScreenshotCodecTest {

  @Test
  void toPngRoundTripsAnImage() throws Exception {
    BufferedImage image = new BufferedImage(10, 5, BufferedImage.TYPE_INT_RGB);
    byte[] bytes = ScreenshotCodec.toPng(image);
    assertThat(bytes).isNotEmpty();

    BufferedImage decoded = ImageIO.read(new ByteArrayInputStream(bytes));
    assertThat(decoded.getWidth()).isEqualTo(10);
    assertThat(decoded.getHeight()).isEqualTo(5);
  }

  @Test
  void nullImageThrowsUpstreamUnavailable() {
    assertThatThrownBy(() -> ScreenshotCodec.toPng(null))
        .isInstanceOf(UpstreamUnavailableException.class);
  }
}
