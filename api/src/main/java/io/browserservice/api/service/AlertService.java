package io.browserservice.api.service;

import com.looksee.browser.enums.AlertChoice;
import io.browserservice.api.dto.AlertRespondRequest;
import io.browserservice.api.dto.AlertStateResponse;
import io.browserservice.api.session.SessionHandle;
import io.browserservice.api.session.SessionLocks;
import io.browserservice.api.session.SessionRegistry;
import java.util.UUID;
import org.openqa.selenium.Alert;
import org.openqa.selenium.NoAlertPresentException;
import org.openqa.selenium.WebDriver;
import org.springframework.stereotype.Service;

@Service
public class AlertService {

    private final SessionRegistry registry;
    private final SessionLocks locks;

    public AlertService(SessionRegistry registry, SessionLocks locks) {
        this.registry = registry;
        this.locks = locks;
    }

    public AlertStateResponse getAlert(UUID sessionId) {
        SessionHandle handle = registry.get(sessionId);
        return locks.doWithLock(handle, AlertService::peekAlert);
    }

    /**
     * Reads the current alert state assuming the caller already holds the session lock.
     * Used by both the user-facing {@link #getAlert(UUID)} (under {@code doWithLock}) and
     * by the WS {@code AlertWatcher} (under {@code tryDoWithLock}, which must not refresh
     * idle TTL).
     */
    public static AlertStateResponse peekAlert(SessionHandle handle) {
        Alert alert = findAlert(handle.driver());
        if (alert == null) {
            return new AlertStateResponse(false, null);
        }
        return new AlertStateResponse(true, safeAlertText(alert));
    }

    public void respond(UUID sessionId, AlertRespondRequest req) {
        SessionHandle handle = registry.get(sessionId);
        locks.doWithLockVoid(handle, h -> {
            Alert alert = findAlert(h.driver());
            if (alert == null) {
                return;
            }
            if (req.input() != null && !req.input().isEmpty()) {
                try {
                    alert.sendKeys(req.input());
                } catch (Exception ignored) {
                    // sendKeys fails for non-prompt alerts; ignore and continue
                }
            }
            if (req.choice() == AlertChoice.ACCEPT) {
                alert.accept();
            } else {
                alert.dismiss();
            }
        });
    }

    private static Alert findAlert(WebDriver driver) {
        try {
            return driver.switchTo().alert();
        } catch (NoAlertPresentException e) {
            return null;
        } catch (Exception e) {
            return null;
        }
    }

    private static String safeAlertText(Alert alert) {
        try {
            return alert.getText();
        } catch (Exception e) {
            return null;
        }
    }
}
