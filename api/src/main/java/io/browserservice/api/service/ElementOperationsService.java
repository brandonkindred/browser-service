package io.browserservice.api.service;

import com.looksee.browser.ActionFactory;
import com.looksee.browser.MobileActionFactory;
import io.browserservice.api.dto.ElementActionRequest;
import io.browserservice.api.dto.ElementScreenshotRequest;
import io.browserservice.api.dto.ElementStateResponse;
import io.browserservice.api.dto.ElementTouchRequest;
import io.browserservice.api.dto.FindElementRequest;
import io.browserservice.api.dto.Rect;
import io.browserservice.api.error.DesktopSessionRequiredException;
import io.browserservice.api.error.MobileSessionRequiredException;
import io.browserservice.api.error.UpstreamUnavailableException;
import io.browserservice.api.session.SessionHandle;
import io.browserservice.api.session.SessionLocks;
import io.browserservice.api.session.SessionRegistry;
import java.awt.image.BufferedImage;
import java.util.Map;
import java.util.UUID;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebElement;
import org.springframework.stereotype.Service;

@Service
public class ElementOperationsService {

  private final SessionRegistry registry;
  private final SessionLocks locks;

  public ElementOperationsService(SessionRegistry registry, SessionLocks locks) {
    this.registry = registry;
    this.locks = locks;
  }

  public ElementStateResponse find(UUID sessionId, FindElementRequest req) {
    SessionHandle handle = registry.get(sessionId);
    return locks.doWithLock(
        handle,
        h -> {
          WebElement element;
          try {
            element =
                h.isMobile()
                    ? h.asMobileDevice().findElement(req.xpath())
                    : h.asBrowser().findElement(req.xpath());
          } catch (NoSuchElementException e) {
            return new ElementStateResponse(null, false, false, Map.of(), null);
          }

          String id = h.elements().put(element);
          boolean displayed = safeIsDisplayed(element);
          Map<String, String> attributes =
              h.isMobile()
                  ? h.asMobileDevice().extractAttributes(element)
                  : h.asBrowser().extractAttributes(element);
          Rect rect = safeRect(element);
          return new ElementStateResponse(id, true, displayed, attributes, rect);
        });
  }

  public void action(UUID sessionId, ElementActionRequest req) {
    SessionHandle handle = registry.get(sessionId);
    if (handle.isMobile()) {
      throw new DesktopSessionRequiredException();
    }
    locks.doWithLockVoid(
        handle,
        h -> {
          WebElement element = h.elements().get(req.elementHandle());
          new ActionFactory(h.asBrowser().getDriver())
              .execAction(element, req.input(), req.action());
        });
  }

  public void touch(UUID sessionId, ElementTouchRequest req) {
    SessionHandle handle = registry.get(sessionId);
    if (!handle.isMobile()) {
      throw new MobileSessionRequiredException();
    }
    locks.doWithLockVoid(
        handle,
        h -> {
          WebElement element = h.elements().get(req.elementHandle());
          new MobileActionFactory(h.asMobileDevice().getDriver())
              .execAction(element, req.input(), req.action());
        });
  }

  public byte[] elementScreenshot(UUID sessionId, ElementScreenshotRequest req) {
    SessionHandle handle = registry.get(sessionId);
    return locks.doWithLock(
        handle,
        h -> {
          WebElement element = h.elements().get(req.elementHandle());
          BufferedImage image;
          try {
            image =
                h.isMobile()
                    ? h.asMobileDevice().getElementScreenshot(element)
                    : h.asBrowser().getElementScreenshot(element);
          } catch (RuntimeException e) {
            throw e;
          } catch (Exception e) {
            throw new UpstreamUnavailableException(
                "failed to capture element screenshot: " + e.getMessage(), e);
          }
          return ScreenshotCodec.toPng(image);
        });
  }

  private static boolean safeIsDisplayed(WebElement element) {
    try {
      return element.isDisplayed();
    } catch (Exception e) {
      return false;
    }
  }

  private static Rect safeRect(WebElement element) {
    try {
      org.openqa.selenium.Rectangle r = element.getRect();
      return new Rect(r.x, r.y, r.width, r.height);
    } catch (Exception e) {
      return null;
    }
  }
}
