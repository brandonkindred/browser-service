package io.browserservice.api.service;

import com.looksee.browser.Browser;
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
import io.browserservice.api.dto.ScrollMode;
import io.browserservice.api.dto.ScrollOffset;
import io.browserservice.api.dto.ScrollRequest;
import io.browserservice.api.dto.Viewport;
import io.browserservice.api.dto.ViewportStateResponse;
import io.browserservice.api.error.DesktopSessionRequiredException;
import io.browserservice.api.error.ValidationFailedException;
import io.browserservice.api.session.SessionHandle;
import io.browserservice.api.session.SessionLocks;
import io.browserservice.api.session.SessionRegistry;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.UUID;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.Point;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.springframework.stereotype.Service;

@Service
public class BrowserOperationsService {

  private final SessionRegistry registry;
  private final SessionLocks locks;

  public BrowserOperationsService(SessionRegistry registry, SessionLocks locks) {
    this.registry = registry;
    this.locks = locks;
  }

  public NavigateResponse navigate(UUID sessionId, NavigateRequest req) {
    SessionHandle handle = registry.get(sessionId);
    return locks.doWithLock(
        handle,
        h -> {
          try {
            if (h.isMobile()) {
              h.asMobileDevice().navigateTo(req.url());
              h.asMobileDevice().waitForPageToLoad();
            } else {
              h.asBrowser().navigateTo(req.url());
              h.asBrowser().waitForPageToLoad();
            }
            return new NavigateResponse(h.driver().getCurrentUrl(), NavigateStatus.LOADED);
          } catch (TimeoutException e) {
            return new NavigateResponse(safeUrl(h.driver()), NavigateStatus.TIMEOUT);
          } catch (RuntimeException e) {
            return new NavigateResponse(safeUrl(h.driver()), NavigateStatus.ERROR);
          }
        });
  }

  public PageSourceResponse getSource(UUID sessionId) {
    SessionHandle handle = registry.get(sessionId);
    return locks.doWithLock(
        handle,
        h -> {
          String src = h.isMobile() ? h.asMobileDevice().getSource() : h.asBrowser().getSource();
          return new PageSourceResponse(safeUrl(h.driver()), src);
        });
  }

  public PageStatusResponse getStatus(UUID sessionId) {
    SessionHandle handle = registry.get(sessionId);
    return locks.doWithLock(
        handle,
        h -> {
          boolean is503;
          try {
            String html = h.isMobile() ? h.asMobileDevice().getSource() : h.asBrowser().getSource();
            is503 = HtmlUtils.is503Error(html);
          } catch (Exception e) {
            is503 = false;
          }
          return new PageStatusResponse(safeUrl(h.driver()), is503);
        });
  }

  public ViewportStateResponse getViewport(UUID sessionId) {
    SessionHandle handle = registry.get(sessionId);
    return locks.doWithLock(
        handle,
        h -> {
          Dimension size = h.driver().manage().window().getSize();
          Point scroll =
              h.isMobile()
                  ? h.asMobileDevice().getViewportScrollOffset()
                  : h.asBrowser().getViewportScrollOffset();
          return new ViewportStateResponse(
              new Viewport(size.getWidth(), size.getHeight()),
              new ScrollOffset(scroll.getX(), scroll.getY()));
        });
  }

  public ScrollOffset scroll(UUID sessionId, ScrollRequest req) {
    SessionHandle handle = registry.get(sessionId);
    return locks.doWithLock(
        handle,
        h -> {
          performScroll(h, req);
          Point scroll =
              h.isMobile()
                  ? h.asMobileDevice().getViewportScrollOffset()
                  : h.asBrowser().getViewportScrollOffset();
          return new ScrollOffset(scroll.getX(), scroll.getY());
        });
  }

  public byte[] pageScreenshot(
      UUID sessionId, io.browserservice.api.dto.ScreenshotStrategy strategy) {
    SessionHandle handle = registry.get(sessionId);
    return locks.doWithLock(
        handle,
        h -> {
          try {
            BufferedImage image = renderPageScreenshot(h, strategy);
            return ScreenshotCodec.toPng(image);
          } catch (IOException e) {
            throw new io.browserservice.api.error.UpstreamUnavailableException(
                "failed to capture screenshot: " + e.getMessage(), e);
          }
        });
  }

  public void removeDom(UUID sessionId, DomRemoveRequest req) {
    SessionHandle handle = registry.get(sessionId);
    if (handle.isMobile()) {
      throw new DesktopSessionRequiredException();
    }
    if (req.preset() == DomRemovePreset.BY_CLASS
        && (req.value() == null || req.value().isBlank())) {
      throw new ValidationFailedException("value is required when preset == BY_CLASS");
    }
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
  }

  public void moveMouse(UUID sessionId, MouseMoveRequest req) {
    SessionHandle handle = registry.get(sessionId);
    if (handle.isMobile()) {
      throw new DesktopSessionRequiredException();
    }
    locks.doWithLockVoid(
        handle,
        h -> {
          Browser browser = h.asBrowser();
          switch (req.mode()) {
            case OUT_OF_FRAME -> browser.moveMouseOutOfFrame();
            case TO_NON_INTERACTIVE -> {
              if (req.x() == null || req.y() == null) {
                throw new ValidationFailedException("x and y are required for TO_NON_INTERACTIVE");
              }
              browser.moveMouseToNonInteractive(new Point(req.x(), req.y()));
            }
          }
        });
  }

  public ExecuteResponse executeScript(UUID sessionId, ExecuteRequest req) {
    SessionHandle handle = registry.get(sessionId);
    return locks.doWithLock(
        handle,
        h -> {
          JavascriptExecutor js = (JavascriptExecutor) h.driver();
          Object[] args = req.args() == null ? new Object[0] : req.args().toArray();
          Object result = js.executeScript(req.script(), args);
          return new ExecuteResponse(result);
        });
  }

  private void performScroll(SessionHandle h, ScrollRequest req) {
    Browser browser = h.isMobile() ? null : h.asBrowser();
    MobileDevice mobile = h.isMobile() ? h.asMobileDevice() : null;
    ScrollMode mode = req.mode();
    switch (mode) {
      case TO_TOP -> {
        if (browser != null) browser.scrollToTopOfPage();
        else mobile.scrollToTopOfPage();
      }
      case TO_BOTTOM -> {
        if (browser != null) browser.scrollToBottomOfPage();
        else mobile.scrollToBottomOfPage();
      }
      case TO_ELEMENT -> {
        if (req.elementHandle() == null || req.elementHandle().isBlank()) {
          throw new ValidationFailedException("element_handle is required for TO_ELEMENT");
        }
        WebElement el = h.elements().get(req.elementHandle());
        if (browser != null) browser.scrollToElement(el);
        else mobile.scrollToElement(el);
      }
      case TO_ELEMENT_CENTERED -> {
        if (req.elementHandle() == null || req.elementHandle().isBlank()) {
          throw new ValidationFailedException("element_handle is required for TO_ELEMENT_CENTERED");
        }
        WebElement el = h.elements().get(req.elementHandle());
        if (browser != null) {
          browser.scrollToElementCentered(el);
        } else {
          // MobileDevice has no "centered" variant; fall through to scrollToElement
          mobile.scrollToElement(el);
        }
      }
      case DOWN_PERCENT -> {
        if (req.percent() == null) {
          throw new ValidationFailedException("percent is required for DOWN_PERCENT");
        }
        if (browser != null) browser.scrollDownPercent(req.percent());
        else mobile.scrollDownPercent(req.percent());
      }
      case DOWN_FULL -> {
        if (browser != null) browser.scrollDownFull();
        else mobile.scrollDownFull();
      }
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
