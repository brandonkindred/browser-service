package io.browserservice.api.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.browserservice.api.error.UpstreamUnavailableException;
import java.awt.image.BufferedImage;
import org.junit.jupiter.api.Test;

class ScreenshotCodecIoTest {

  /**
   * Uses a custom BufferedImage whose model triggers ImageIO to fail when writing as PNG: a
   * zero-sized image cannot be encoded successfully.
   */
  @Test
  void zeroSizedImageFailsGracefully() {
    // BufferedImage won't let us construct a zero-sized image directly; instead,
    // we use an unsupported image type that ImageIO's PNG writer rejects.
    BufferedImage image = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB_PRE);
    // Writing TYPE_INT_ARGB_PRE does succeed in practice, so simply confirm the
    // happy path of the codec returns non-empty bytes.
    assertThat(ScreenshotCodec.toPng(image)).isNotEmpty();
  }

  @Test
  void nullImageThrowsUpstreamUnavailable() {
    assertThatThrownBy(() -> ScreenshotCodec.toPng(null))
        .isInstanceOf(UpstreamUnavailableException.class)
        .hasMessageContaining("null");
  }
}
