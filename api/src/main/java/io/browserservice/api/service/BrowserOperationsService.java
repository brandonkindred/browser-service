package io.browserservice.api.service;

import com.looksee.browser.Browser;
import com.looksee.browser.DriverOps;
import com.looksee.browser.MobileDevice;
import com.looksee.browser.utils.HtmlUtils;
import io.browserservice.api.dto.DomRemovePreset;
import io.browserservice.api.dto.DomRemoveRequest;
import io.browserservice.api.dto.ExecuteRequest;
import io.browserservice.api.dto.ExecuteResponse;
import io.browserservice.api.dto.MouseMoveRequest;
import io.browserservice.api.dto.NavigateRequest;
import io.browserservice.api.dto.NavigateResponse;
import io.browserservice.api.dto.NavigateStatus;
import io.browserservice.api.dto.PageSourceResponse;
import io.browserservice.api.dto.PageStatusResponse;
import io.browserservice.api.dto.ScrollOffset;
import io.browserservice.api.dto.ScrollRequest;
import io.browserservice.api.dto.Viewport;
import io.browserservice.api.dto.ViewportStateResponse;
import io.browserservice.api.error.ApiException;
import io.browserservice.api.error.DesktopSessionRequiredException;
import io.browserservice.api.error.ValidationFailedException;
import io.browserservice.api.security.UrlSafetyValidator;
import io.browserservice.api.session.CallerId;
import io.browserservice.api.session.SessionHandle;
import io.browserservice.api.session.SessionLocks;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import org.eclipse.microprofile.faulttolerance.Retry;
import org.eclipse.microprofile.faulttolerance.Timeout;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.Point;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.remote.UnreachableBrowserException;
import org.springframework.stereotype.Service;

@Service
public class BrowserOperationsService {

  private final SessionService sessionService;
  private final SessionLocks locks;
  private final UrlSafetyValidator urlValidator;
  private final SeleniumGuard guard;

  public BrowserOperationsService(
      SessionService sessionService,
      SessionLocks locks,
      UrlSafetyValidator urlValidator,
      SeleniumGuard guard) {
    this.sessionService = sessionService;
    this.locks = locks;
    this.urlValidator = urlValidator;
    this.guard = guard;
  }

  public NavigateResponse navigate(UUID sessionId, CallerId caller, NavigateRequest req) {
    // Authorize first so an unauthenticated caller can't use differential 400 vs 403 responses
    // to probe whether arbitrary hostnames resolve to internal IPs.
    SessionHandle handle = sessionService.requireOwner(sessionId, caller);
    urlValidator.validate(req.url());
    return guard.execute(
        () ->
            locks.doWithLock(
                handle,
                h -> {
                  try {
                    DriverOps ops = h.ops();
                    ops.navigateTo(req.url());
                    ops.waitForPageToLoad();
                    return new NavigateResponse(h.driver().getCurrentUrl(), NavigateStatus.LOADED);
                  } catch (TimeoutException e) {
                    return new NavigateResponse(safeUrl(h.driver()), NavigateStatus.TIMEOUT);
                  } catch (WebDriverException e) {
                    // Real driver failures (UnreachableBrowserException etc.) must reach the
                    // guard so the circuit breaker can trip on a dead replica. Without this
                    // re-throw, the RuntimeException catch below would mask them as ERROR.
                    throw e;
                  } catch (RuntimeException e) {
                    return new NavigateResponse(safeUrl(h.driver()), NavigateStatus.ERROR);
                  }
                }));
  }

  // retryOn is narrowed to the one genuinely transient failure: UnreachableBrowserException.
  // The breaker counts a broader set (also NoSuchSessionException) because it cares about the
  // "Selenium dropped many sessions" pattern, but at retry-time NSE isn't transient — re-running
  // immediately won't bring the session back. Retrying on page-state WebDriverException subclasses
  // (UnhandledAlert, Stale, JavascriptException, …) would just burn ~14s on deterministic failures.
  // maxDuration is a best-effort total-time budget: MP-FT stops SCHEDULING new retries past 22s
  // but an in-flight attempt runs to completion. 22s = 3 × 7s @Timeout + 2 × ≤350ms jitter, so
  // all three attempts can fire. @Timeout is 7s, not 5s, to clear the 5s session
  // lock-acquire-timeout — a tied 5/5 race would mask SessionBusyException (409) as
  // selenium_call_timeout (504).
  @Retry(
      maxRetries = 2,
      delay = 250,
      delayUnit = ChronoUnit.MILLIS,
      jitter = 100,
      jitterDelayUnit = ChronoUnit.MILLIS,
      maxDuration = 22,
      durationUnit = ChronoUnit.SECONDS,
      retryOn = UnreachableBrowserException.class,
      abortOn = ApiException.class)
  @Timeout(value = 7, unit = ChronoUnit.SECONDS)
  public PageSourceResponse getSource(UUID sessionId, CallerId caller) {
    SessionHandle handle = sessionService.requireOwner(sessionId, caller);
    return guard.execute(
        () ->
            locks.doWithLock(
                handle,
                h -> {
                  String src = h.ops().getSource();
                  return new PageSourceResponse(safeUrl(h.driver()), src);
                }));
  }

  @Retry(
      maxRetries = 2,
      delay = 250,
      delayUnit = ChronoUnit.MILLIS,
      jitter = 100,
      jitterDelayUnit = ChronoUnit.MILLIS,
      maxDuration = 22,
      durationUnit = ChronoUnit.SECONDS,
      retryOn = UnreachableBrowserException.class,
      abortOn = ApiException.class)
  @Timeout(value = 7, unit = ChronoUnit.SECONDS)
  public PageStatusResponse getStatus(UUID sessionId, CallerId caller) {
    SessionHandle handle = sessionService.requireOwner(sessionId, caller);
    return guard.execute(
        () ->
            locks.doWithLock(
                handle,
                h -> {
                  boolean is503;
                  try {
                    String html = h.ops().getSource();
                    is503 = HtmlUtils.is503Error(html);
                  } catch (WebDriverException e) {
                    // Driver-level failure must reach the guard so the breaker can trip and
                    // @Retry can re-attempt. Swallowing it here defeats both.
                    throw e;
                  } catch (RuntimeException e) {
                    // Page-source parse / HtmlUtils errors fall back to is503=false. Narrower
                    // than Exception so Errors (OOM, etc.) keep propagating.
                    is503 = false;
                  }
                  return new PageStatusResponse(safeUrl(h.driver()), is503);
                }));
  }

  @Retry(
      maxRetries = 2,
      delay = 250,
      delayUnit = ChronoUnit.MILLIS,
      jitter = 100,
      jitterDelayUnit = ChronoUnit.MILLIS,
      maxDuration = 22,
      durationUnit = ChronoUnit.SECONDS,
      retryOn = UnreachableBrowserException.class,
      abortOn = ApiException.class)
  @Timeout(value = 7, unit = ChronoUnit.SECONDS)
  public ViewportStateResponse getViewport(UUID sessionId, CallerId caller) {
    SessionHandle handle = sessionService.requireOwner(sessionId, caller);
    return guard.execute(
        () ->
            locks.doWithLock(
                handle,
                h -> {
                  Dimension size = h.driver().manage().window().getSize();
                  Point scroll = h.ops().getViewportScrollOffset();
                  return new ViewportStateResponse(
                      new Viewport(size.getWidth(), size.getHeight()),
                      new ScrollOffset(scroll.getX(), scroll.getY()));
                }));
  }

  public ScrollOffset scroll(UUID sessionId, CallerId caller, ScrollRequest req) {
    SessionHandle handle = sessionService.requireOwner(sessionId, caller);
    return guard.execute(
        () ->
            locks.doWithLock(
                handle,
                h -> {
                  performScroll(h, req);
                  Point scroll = h.ops().getViewportScrollOffset();
                  return new ScrollOffset(scroll.getX(), scroll.getY());
                }));
  }

  @Retry(
      maxRetries = 2,
      delay = 250,
      delayUnit = ChronoUnit.MILLIS,
      jitter = 100,
      jitterDelayUnit = ChronoUnit.MILLIS,
      maxDuration = 22,
      durationUnit = ChronoUnit.SECONDS,
      retryOn = UnreachableBrowserException.class,
      abortOn = ApiException.class)
  public byte[] pageScreenshot(
      UUID sessionId, CallerId caller, io.browserservice.api.dto.ScreenshotStrategy strategy) {
    SessionHandle handle = sessionService.requireOwner(sessionId, caller);
    return guard.execute(
        () ->
            locks.doWithLock(
                handle,
                h -> {
                  try {
                    BufferedImage image = renderPageScreenshot(h, strategy);
                    return ScreenshotCodec.toPng(image);
                  } catch (IOException e) {
                    throw new io.browserservice.api.error.UpstreamUnavailableException(
                        "failed to capture screenshot: " + e.getMessage(), e);
                  }
                }));
  }

  public void removeDom(UUID sessionId, CallerId caller, DomRemoveRequest req) {
    SessionHandle handle = sessionService.requireOwner(sessionId, caller);
    if (handle.isMobile()) {
      throw new DesktopSessionRequiredException();
    }
    if (req.preset() == DomRemovePreset.BY_CLASS
        && (req.value() == null || req.value().isBlank())) {
      throw new ValidationFailedException("value is required when preset == BY_CLASS");
    }
    guard.execute(
        () -> {
          locks.doWithLockVoid(
              handle,
              h -> {
                Browser browser = h.asBrowser();
                switch (req.preset()) {
                  case DRIFT_CHAT -> browser.removeDriftChat();
                  case GDPR_MODAL -> browser.removeGDPRmodals();
                  case GDPR -> browser.removeGDPR();
                  case BY_CLASS -> browser.removeElement(req.value());
                }
              });
          return null;
        });
  }

  public void moveMouse(UUID sessionId, CallerId caller, MouseMoveRequest req) {
    SessionHandle handle = sessionService.requireOwner(sessionId, caller);
    if (handle.isMobile()) {
      throw new DesktopSessionRequiredException();
    }
    guard.execute(
        () -> {
          locks.doWithLockVoid(
              handle,
              h -> {
                Browser browser = h.asBrowser();
                switch (req.mode()) {
                  case OUT_OF_FRAME -> browser.moveMouseOutOfFrame();
                  case TO_NON_INTERACTIVE -> {
                    if (req.x() == null || req.y() == null) {
                      throw new ValidationFailedException(
                          "x and y are required for TO_NON_INTERACTIVE");
                    }
                    browser.moveMouseToNonInteractive(new Point(req.x(), req.y()));
                  }
                }
              });
          return null;
        });
  }

  public ExecuteResponse executeScript(UUID sessionId, CallerId caller, ExecuteRequest req) {
    SessionHandle handle = sessionService.requireOwner(sessionId, caller);
    return guard.execute(
        () ->
            locks.doWithLock(
                handle,
                h -> {
                  JavascriptExecutor js = (JavascriptExecutor) h.driver();
                  Object[] args = req.args() == null ? new Object[0] : req.args().toArray();
                  Object result = js.executeScript(req.script(), args);
                  return new ExecuteResponse(result);
                }));
  }

  private void performScroll(SessionHandle h, ScrollRequest req) {
    DriverOps ops = h.ops();
    switch (req.mode()) {
      case TO_TOP -> ops.scrollToTopOfPage();
      case TO_BOTTOM -> ops.scrollToBottomOfPage();
      case TO_ELEMENT -> {
        if (req.elementHandle() == null || req.elementHandle().isBlank()) {
          throw new ValidationFailedException("element_handle is required for TO_ELEMENT");
        }
        ops.scrollToElement(h.elements().get(req.elementHandle()));
      }
      case TO_ELEMENT_CENTERED -> {
        if (req.elementHandle() == null || req.elementHandle().isBlank()) {
          throw new ValidationFailedException("element_handle is required for TO_ELEMENT_CENTERED");
        }
        WebElement el = h.elements().get(req.elementHandle());
        if (h.isMobile()) {
          ops.scrollToElement(el);
        } else {
          h.asBrowser().scrollToElementCentered(el);
        }
      }
      case DOWN_PERCENT -> {
        if (req.percent() == null) {
          throw new ValidationFailedException("percent is required for DOWN_PERCENT");
        }
        ops.scrollDownPercent(req.percent());
      }
      case DOWN_FULL -> ops.scrollDownFull();
    }
  }

  private BufferedImage renderPageScreenshot(
      SessionHandle h, io.browserservice.api.dto.ScreenshotStrategy strategy) throws IOException {
    if (h.isMobile()) {
      MobileDevice device = h.asMobileDevice();
      return switch (strategy) {
        case VIEWPORT -> device.getViewportScreenshot();
        case FULL_PAGE_SHUTTERBUG, FULL_PAGE_ASHOT, FULL_PAGE_SHUTTERBUG_PAUSED ->
            device.getFullPageScreenshot();
      };
    }
    Browser browser = h.asBrowser();
    return switch (strategy) {
      case VIEWPORT -> browser.getViewportScreenshot();
      case FULL_PAGE_SHUTTERBUG -> browser.getFullPageScreenshot();
      case FULL_PAGE_ASHOT -> browser.getFullPageScreenshotAshot();
      case FULL_PAGE_SHUTTERBUG_PAUSED -> browser.getFullPageScreenshotShutterbug();
    };
  }

  private static String safeUrl(WebDriver driver) {
    try {
      return driver.getCurrentUrl();
    } catch (Exception e) {
      return null;
    }
  }
}
