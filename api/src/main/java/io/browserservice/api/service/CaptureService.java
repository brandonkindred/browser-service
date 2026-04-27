package io.browserservice.api.service;

import com.looksee.browser.Browser;
import com.looksee.browser.MobileDevice;
import com.looksee.browser.enums.BrowserEnvironment;
import io.browserservice.api.config.EngineProperties;
import io.browserservice.api.dto.CaptureRequest;
import io.browserservice.api.dto.CaptureResponse;
import io.browserservice.api.dto.CaptureScreenshotRef;
import io.browserservice.api.dto.ElementStateResponse;
import io.browserservice.api.dto.PngEncoding;
import io.browserservice.api.dto.Rect;
import io.browserservice.api.dto.ScreenshotStrategy;
import io.browserservice.api.error.UpstreamUnavailableException;
import io.browserservice.api.session.CaptureScreenshotCache;
import io.browserservice.api.session.DriverFactory;
import io.browserservice.api.session.SessionHandle;
import io.browserservice.api.session.SessionLocks;
import io.browserservice.api.session.SessionRegistry;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.time.Duration;
import java.util.Base64;
import java.util.Map;
import java.util.UUID;
import javax.imageio.ImageIO;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebElement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class CaptureService {

  private static final Logger log = LoggerFactory.getLogger(CaptureService.class);

  private final SessionRegistry registry;
  private final SessionLocks locks;
  private final DriverFactory drivers;
  private final CaptureScreenshotCache cache;
  private final Duration idleTtl;
  private final Duration absoluteTtl;

  public CaptureService(
      SessionRegistry registry,
      SessionLocks locks,
      DriverFactory drivers,
      CaptureScreenshotCache cache,
      EngineProperties props) {
    this.registry = registry;
    this.locks = locks;
    this.drivers = drivers;
    this.cache = cache;
    this.idleTtl = Duration.ofSeconds(props.session().idleTtlSeconds());
    this.absoluteTtl = Duration.ofSeconds(props.session().absoluteTtlSeconds());
  }

  public CaptureResponse capture(CaptureRequest req) {
    BrowserEnvironment env =
        req.environment() == null ? BrowserEnvironment.TEST : req.environment();
    ScreenshotStrategy strategy =
        req.strategy() == null ? ScreenshotStrategy.VIEWPORT : req.strategy();
    PngEncoding encoding = req.encoding() == null ? PngEncoding.BINARY : req.encoding();
    boolean includeSource = Boolean.TRUE.equals(req.includeSource());
    String xpath = req.xpath();

    registry.acquirePermit();
    SessionHandle handle = null;
    try {
      if (req.browserType().isMobile()) {
        MobileDevice device = drivers.createMobile(req.browserType(), env);
        handle = SessionHandle.mobile(device, req.browserType(), env, idleTtl, absoluteTtl);
      } else {
        Browser browser = drivers.createDesktop(req.browserType(), env);
        handle = SessionHandle.desktop(browser, req.browserType(), env, idleTtl, absoluteTtl);
      }
      registry.register(handle);

      SessionHandle h = handle;
      CaptureResponse response =
          locks.doWithLock(
              h,
              sess -> {
                try {
                  if (sess.isMobile()) {
                    sess.asMobileDevice().navigateTo(req.url());
                    sess.asMobileDevice().waitForPageToLoad();
                  } else {
                    sess.asBrowser().navigateTo(req.url());
                    sess.asBrowser().waitForPageToLoad();
                  }

                  ElementStateResponse element = resolveElement(sess, xpath);

                  byte[] pngBytes = renderPageScreenshot(sess, strategy);
                  int[] wh = readDimensions(pngBytes);
                  UUID captureId = cache.put(pngBytes, wh[0], wh[1]);
                  CaptureScreenshotRef ref = buildRef(captureId, pngBytes, wh[0], wh[1], encoding);

                  String source = null;
                  if (includeSource) {
                    source =
                        sess.isMobile()
                            ? sess.asMobileDevice().getSource()
                            : sess.asBrowser().getSource();
                  }

                  String currentUrl = sess.driver().getCurrentUrl();
                  return new CaptureResponse(captureId, currentUrl, ref, source, element);
                } catch (RuntimeException e) {
                  throw e;
                } catch (Exception e) {
                  throw new UpstreamUnavailableException("capture failed: " + e.getMessage(), e);
                }
              });
      return response;
    } catch (RuntimeException e) {
      // If handle was created but register hadn't occurred we still need to release the permit.
      if (handle == null) {
        registry.releasePermit();
      }
      throw e;
    } finally {
      if (handle != null) {
        try {
          registry.remove(handle.id());
        } catch (Exception e) {
          log.warn("failed to clean up capture session {}: {}", handle.id(), e.toString());
        }
      }
    }
  }

  private ElementStateResponse resolveElement(SessionHandle sess, String xpath) {
    if (xpath == null || xpath.isBlank()) {
      return null;
    }
    WebElement element;
    try {
      element =
          sess.isMobile()
              ? sess.asMobileDevice().findElement(xpath)
              : sess.asBrowser().findElement(xpath);
    } catch (NoSuchElementException e) {
      return new ElementStateResponse(null, false, false, Map.of(), null);
    }
    String handle = sess.elements().put(element);
    boolean displayed;
    try {
      displayed = element.isDisplayed();
    } catch (Exception e) {
      displayed = false;
    }
    Map<String, String> attributes =
        sess.isMobile()
            ? sess.asMobileDevice().extractAttributes(element)
            : sess.asBrowser().extractAttributes(element);
    Rect rect;
    try {
      org.openqa.selenium.Rectangle r = element.getRect();
      rect = new Rect(r.x, r.y, r.width, r.height);
    } catch (Exception e) {
      rect = null;
    }
    return new ElementStateResponse(handle, true, displayed, attributes, rect);
  }

  private byte[] renderPageScreenshot(SessionHandle sess, ScreenshotStrategy strategy)
      throws Exception {
    BufferedImage image;
    if (sess.isMobile()) {
      MobileDevice device = sess.asMobileDevice();
      image =
          switch (strategy) {
            case VIEWPORT -> device.getViewportScreenshot();
            case FULL_PAGE_SHUTTERBUG, FULL_PAGE_ASHOT, FULL_PAGE_SHUTTERBUG_PAUSED ->
                device.getFullPageScreenshot();
          };
    } else {
      Browser b = sess.asBrowser();
      image =
          switch (strategy) {
            case VIEWPORT -> b.getViewportScreenshot();
            case FULL_PAGE_SHUTTERBUG -> b.getFullPageScreenshot();
            case FULL_PAGE_ASHOT -> b.getFullPageScreenshotAshot();
            case FULL_PAGE_SHUTTERBUG_PAUSED -> b.getFullPageScreenshotShutterbug();
          };
    }
    return ScreenshotCodec.toPng(image);
  }

  private static int[] readDimensions(byte[] pngBytes) {
    try (var in = new ByteArrayInputStream(pngBytes)) {
      var image = ImageIO.read(in);
      if (image == null) {
        return new int[] {0, 0};
      }
      return new int[] {image.getWidth(), image.getHeight()};
    } catch (Exception e) {
      return new int[] {0, 0};
    }
  }

  private static CaptureScreenshotRef buildRef(
      UUID captureId, byte[] pngBytes, int width, int height, PngEncoding encoding) {
    if (encoding == PngEncoding.BASE64) {
      return new CaptureScreenshotRef(
          Base64.getEncoder().encodeToString(pngBytes), null, width, height);
    }
    return new CaptureScreenshotRef(
        null, "/v1/capture/" + captureId + "/screenshot", width, height);
  }

  public CaptureScreenshotCache.CaptureEntry fetchScreenshot(UUID captureId) {
    return cache.get(captureId);
  }
}
