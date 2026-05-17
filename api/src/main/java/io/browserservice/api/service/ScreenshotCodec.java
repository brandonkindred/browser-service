package io.browserservice.api.service;

import io.browserservice.api.error.ScreenshotEncodingFailedException;
import io.browserservice.api.error.UpstreamUnavailableException;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import javax.imageio.ImageIO;

public final class ScreenshotCodec {

  private ScreenshotCodec() {}

  public static byte[] toPng(BufferedImage image) {
    if (image == null) {
      // Null means the upstream Selenium call returned nothing — that's an upstream issue.
      throw new UpstreamUnavailableException("screenshot was null");
    }
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    try {
      ImageIO.write(image, "png", baos);
    } catch (IOException e) {
      // Local PNG encoding failure (rare, deterministic) — not an upstream issue, so don't
      // pretend it is. 500 + screenshot_encoding_failed gives operators an accurate signal.
      throw new ScreenshotEncodingFailedException("failed to encode screenshot as PNG", e);
    }
    return baos.toByteArray();
  }
}
