package io.browserservice.api.service;

import io.browserservice.api.error.UpstreamUnavailableException;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import javax.imageio.ImageIO;

public final class ScreenshotCodec {

  private ScreenshotCodec() {}

  public static byte[] toPng(BufferedImage image) {
    if (image == null) {
      throw new UpstreamUnavailableException("screenshot was null");
    }
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    try {
      ImageIO.write(image, "png", baos);
    } catch (IOException e) {
      throw new UpstreamUnavailableException("failed to encode screenshot as PNG", e);
    }
    return baos.toByteArray();
  }
}
