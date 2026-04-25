package io.browserservice.api.session;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

import com.looksee.browser.Browser;
import com.looksee.browser.enums.BrowserEnvironment;
import com.looksee.browser.enums.BrowserType;
import io.browserservice.api.config.EngineProperties;
import io.browserservice.api.error.SessionBusyException;
import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SessionLocksTest {

    private SessionLocks locks;
    private SessionHandle handle;

    @BeforeEach
    void setUp() {
        locks = new SessionLocks(props(50));
        handle = SessionHandle.desktop(mock(Browser.class), BrowserType.CHROME, BrowserEnvironment.TEST,
                Duration.ofSeconds(10), Duration.ofSeconds(60));
    }

    @Test
    void doWithLockRunsWorkAndTouches() throws InterruptedException {
        Instant before = handle.lastUsedAt();
        Thread.sleep(2);
        Integer result = locks.doWithLock(handle, h -> 42);
        assertThat(result).isEqualTo(42);
        assertThat(handle.lastUsedAt()).isAfter(before);
        assertThat(handle.lock().isHeldByCurrentThread()).isFalse();
    }

    @Test
    void doWithLockVoidRunsWorkAndReleasesLock() {
        locks.doWithLockVoid(handle, h -> {});
        assertThat(handle.lock().isHeldByCurrentThread()).isFalse();
    }

    @Test
    void sessionBusyWhenLockCannotBeAcquired() throws Exception {
        SessionLocks quickFail = new SessionLocks(props(1));
        Thread holder = new Thread(() -> {
            handle.lock().lock();
            try {
                Thread.sleep(200);
            } catch (InterruptedException ignored) {
            } finally {
                handle.lock().unlock();
            }
        });
        holder.start();
        Thread.sleep(30);
        try {
            assertThatThrownBy(() -> quickFail.doWithLock(handle, h -> "x"))
                    .isInstanceOf(SessionBusyException.class);
        } finally {
            holder.join();
        }
    }

    @Test
    void interruptedAcquireMapsToSessionBusy() throws Exception {
        SessionLocks slow = new SessionLocks(props(10_000));
        handle.lock().lock();
        try {
            Thread caller = new Thread(() ->
                    assertThatThrownBy(() -> slow.doWithLock(handle, h -> "x"))
                            .isInstanceOf(SessionBusyException.class));
            caller.start();
            Thread.sleep(30);
            caller.interrupt();
            caller.join();
            assertThat(caller.isAlive()).isFalse();
        } finally {
            handle.lock().unlock();
        }
    }

    private static EngineProperties props(long lockTimeoutMs) {
        return new EngineProperties(
                new EngineProperties.SessionProps(10, 60, 1, lockTimeoutMs),
                new EngineProperties.SeleniumProps("", 0, 0, 0, false, 0),
                new EngineProperties.AppiumProps("", "", "", 0, 0),
                new EngineProperties.BrowserStackProps(false, "", "", "", "", "", "", "", "", "", "", "", false, false, false),
                new EngineProperties.WebSocketProps("/v1/ws/sessions", 32, 300, 64, 10000));
    }
}
