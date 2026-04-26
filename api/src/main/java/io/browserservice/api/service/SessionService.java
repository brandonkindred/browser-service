package io.browserservice.api.service;

import com.looksee.browser.Browser;
import com.looksee.browser.MobileDevice;
import com.looksee.browser.enums.BrowserType;
import io.browserservice.api.config.EngineProperties;
import io.browserservice.api.dto.CreateSessionRequest;
import io.browserservice.api.dto.ScrollOffset;
import io.browserservice.api.dto.SessionListResponse;
import io.browserservice.api.dto.SessionResponse;
import io.browserservice.api.dto.SessionStateResponse;
import io.browserservice.api.dto.Viewport;
import io.browserservice.api.persistence.BrowserSessionTracker;
import io.browserservice.api.session.DriverFactory;
import io.browserservice.api.session.SessionHandle;
import io.browserservice.api.session.SessionLocks;
import io.browserservice.api.session.SessionRegistry;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.Point;
import org.openqa.selenium.WebDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class SessionService {

    private static final Logger log = LoggerFactory.getLogger(SessionService.class);

    private final SessionRegistry registry;
    private final SessionLocks locks;
    private final DriverFactory drivers;
    private final BrowserSessionTracker tracker;
    private final Duration defaultIdleTtl;
    private final Duration absoluteTtl;

    public SessionService(SessionRegistry registry, SessionLocks locks,
                          DriverFactory drivers, BrowserSessionTracker tracker,
                          EngineProperties props) {
        this.registry = registry;
        this.locks = locks;
        this.drivers = drivers;
        this.tracker = tracker;
        this.defaultIdleTtl = Duration.ofSeconds(props.session().idleTtlSeconds());
        this.absoluteTtl = Duration.ofSeconds(props.session().absoluteTtlSeconds());
    }

    public SessionResponse create(CreateSessionRequest req) {
        registry.acquirePermit();
        SessionHandle handle = null;
        try {
            Duration idleTtl = req.idleTtlSeconds() == null
                    ? defaultIdleTtl
                    : Duration.ofSeconds(req.idleTtlSeconds());
            if (req.browserType().isMobile()) {
                MobileDevice device = drivers.createMobile(req.browserType(), req.environment());
                handle = SessionHandle.mobile(device, req.browserType(), req.environment(), idleTtl, absoluteTtl);
            } else {
                Browser browser = drivers.createDesktop(req.browserType(), req.environment());
                handle = SessionHandle.desktop(browser, req.browserType(), req.environment(), idleTtl, absoluteTtl);
            }
            tracker.recordCreate(handle);
            registry.register(handle);
            log.info("created session id={} browserType={} environment={}",
                    handle.id(), handle.browserType(), handle.environment());
            return toSummary(handle);
        } catch (RuntimeException e) {
            if (handle != null) {
                handle.closeOnce();
            }
            registry.releasePermit();
            throw e;
        }
    }

    public SessionListResponse list() {
        List<SessionResponse> items = registry.snapshot().stream()
                .filter(h -> !h.isClosed())
                .map(SessionService::toSummary)
                .toList();
        return new SessionListResponse(items);
    }

    public SessionStateResponse describe(UUID id) {
        SessionHandle handle = registry.get(id);
        return locks.doWithLock(handle, this::toStateLocked);
    }

    public void close(UUID id) {
        SessionHandle handle = registry.get(id);
        registry.remove(handle.id());
        tracker.recordClientClose(handle.id());
        log.info("closed session id={}", id);
    }

    private SessionStateResponse toStateLocked(SessionHandle handle) {
        WebDriver driver = handle.driver();
        String currentUrl = safeGetCurrentUrl(driver);
        Viewport viewport = safeViewport(driver);
        ScrollOffset scrollOffset = safeScrollOffset(handle);
        return new SessionStateResponse(
                handle.id(),
                handle.browserType(),
                handle.environment(),
                handle.createdAt(),
                handle.expiresAt(),
                currentUrl,
                viewport,
                scrollOffset);
    }

    private static SessionResponse toSummary(SessionHandle handle) {
        return new SessionResponse(
                handle.id(),
                handle.browserType(),
                handle.environment(),
                handle.createdAt(),
                handle.expiresAt());
    }

    private static String safeGetCurrentUrl(WebDriver driver) {
        try {
            return driver.getCurrentUrl();
        } catch (Exception e) {
            return null;
        }
    }

    private static Viewport safeViewport(WebDriver driver) {
        try {
            Dimension size = driver.manage().window().getSize();
            return new Viewport(size.getWidth(), size.getHeight());
        } catch (Exception e) {
            return null;
        }
    }

    private static ScrollOffset safeScrollOffset(SessionHandle handle) {
        try {
            Point p = handle.isMobile()
                    ? handle.asMobileDevice().getViewportScrollOffset()
                    : handle.asBrowser().getViewportScrollOffset();
            return new ScrollOffset(p.getX(), p.getY());
        } catch (Exception e) {
            return null;
        }
    }
}
