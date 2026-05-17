package io.browserservice.api.service;

import com.looksee.browser.ActionFactory;
import com.looksee.browser.MobileActionFactory;
import io.browserservice.api.dto.ElementActionRequest;
import io.browserservice.api.dto.ElementScreenshotRequest;
import io.browserservice.api.dto.ElementStateResponse;
import io.browserservice.api.dto.ElementTouchRequest;
import io.browserservice.api.dto.FindElementRequest;
import io.browserservice.api.dto.Rect;
import io.browserservice.api.error.ApiException;
import io.browserservice.api.error.DesktopSessionRequiredException;
import io.browserservice.api.error.MobileSessionRequiredException;
import io.browserservice.api.error.UpstreamUnavailableException;
import io.browserservice.api.session.CallerId;
import io.browserservice.api.session.SessionHandle;
import io.browserservice.api.session.SessionLocks;
import java.awt.image.BufferedImage;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.UUID;
import org.eclipse.microprofile.faulttolerance.Retry;
import org.eclipse.microprofile.faulttolerance.Timeout;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.WebElement;
import org.springframework.stereotype.Service;

@Service
public class ElementOperationsService {

  private final SessionService sessionService;
  private final SessionLocks locks;
  private final SeleniumGuard guard;

  public ElementOperationsService(
      SessionService sessionService, SessionLocks locks, SeleniumGuard guard) {
    this.sessionService = sessionService;
    this.locks = locks;
    this.guard = guard;
  }

  @Retry(
      maxRetries = 2,
      delay = 250,
      delayUnit = ChronoUnit.MILLIS,
      jitter = 100,
      jitterDelayUnit = ChronoUnit.MILLIS,
      maxDuration = 16,
      durationUnit = ChronoUnit.SECONDS,
      retryOn = WebDriverException.class,
      abortOn = ApiException.class)
  @Timeout(value = 5, unit = ChronoUnit.SECONDS)
  public ElementStateResponse find(UUID sessionId, CallerId caller, FindElementRequest req) {
    SessionHandle handle = sessionService.requireOwner(sessionId, caller);
    return guard.execute(
        () ->
            locks.doWithLock(
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

                  // Read everything that might throw BEFORE registering the handle, so a retry
                  // after a mid-read WebDriverException doesn't leak an orphaned entry.
                  boolean displayed = safeIsDisplayed(element);
                  Map<String, String> attributes =
                      h.isMobile()
                          ? h.asMobileDevice().extractAttributes(element)
                          : h.asBrowser().extractAttributes(element);
                  Rect rect = safeRect(element);
                  String id = h.elements().put(element);
                  return new ElementStateResponse(id, true, displayed, attributes, rect);
                }));
  }

  public void action(UUID sessionId, CallerId caller, ElementActionRequest req) {
    SessionHandle handle = sessionService.requireOwner(sessionId, caller);
    if (handle.isMobile()) {
      throw new DesktopSessionRequiredException();
    }
    guard.execute(
        () -> {
          locks.doWithLockVoid(
              handle,
              h -> {
                WebElement element = h.elements().get(req.elementHandle());
                new ActionFactory(h.asBrowser().getDriver())
                    .execAction(element, req.input(), req.action());
              });
          return null;
        });
  }

  public void touch(UUID sessionId, CallerId caller, ElementTouchRequest req) {
    SessionHandle handle = sessionService.requireOwner(sessionId, caller);
    if (!handle.isMobile()) {
      throw new MobileSessionRequiredException();
    }
    guard.execute(
        () -> {
          locks.doWithLockVoid(
              handle,
              h -> {
                WebElement element = h.elements().get(req.elementHandle());
                new MobileActionFactory(h.asMobileDevice().getDriver())
                    .execAction(element, req.input(), req.action());
              });
          return null;
        });
  }

  @Retry(
      maxRetries = 2,
      delay = 250,
      delayUnit = ChronoUnit.MILLIS,
      jitter = 100,
      jitterDelayUnit = ChronoUnit.MILLIS,
      maxDuration = 16,
      durationUnit = ChronoUnit.SECONDS,
      retryOn = WebDriverException.class,
      abortOn = ApiException.class)
  public byte[] elementScreenshot(UUID sessionId, CallerId caller, ElementScreenshotRequest req) {
    SessionHandle handle = sessionService.requireOwner(sessionId, caller);
    return guard.execute(
        () ->
            locks.doWithLock(
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
                }));
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
