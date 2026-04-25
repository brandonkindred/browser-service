package io.browserservice.api.ws.push;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.looksee.browser.Browser;
import com.looksee.browser.enums.BrowserEnvironment;
import com.looksee.browser.enums.BrowserType;
import io.browserservice.api.config.EngineProperties;
import io.browserservice.api.session.SessionHandle;
import io.browserservice.api.session.SessionLocks;
import io.browserservice.api.session.SessionRegistry;
import io.browserservice.api.ws.CallerId;
import io.browserservice.api.ws.Connection;
import io.browserservice.api.ws.WsSessionConnections;
import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Semaphore;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.NoAlertPresentException;
import org.openqa.selenium.WebDriver;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.ConcurrentWebSocketSessionDecorator;

class WatcherCoordinatorTest {

    private SessionRegistry registry;
    private SessionLocks locks;
    private WsSessionConnections connections;
    private EventBroadcaster broadcaster;
    private ScheduledExecutorService scheduler;
    private WatcherCoordinator coordinator;

    private SessionHandle handle;
    private UUID sessionId;

    @BeforeEach
    void setUp() {
        EngineProperties props = props();
        registry = new SessionRegistry(props);
        locks = new SessionLocks(props);
        connections = new WsSessionConnections();
        broadcaster = mock(EventBroadcaster.class);
        scheduler = Executors.newScheduledThreadPool(1, r -> {
            Thread t = new Thread(r, "test-ws-scheduler");
            t.setDaemon(true);
            return t;
        });
        coordinator = new WatcherCoordinator(connections, registry, locks, broadcaster, scheduler, props);

        Browser browser = mock(Browser.class);
        WebDriver driver = mock(WebDriver.class, org.mockito.Mockito.RETURNS_DEEP_STUBS);
        when(browser.getDriver()).thenReturn(driver);
        when(driver.switchTo().alert()).thenThrow(new NoAlertPresentException("no"));
        handle = SessionHandle.desktop(browser, BrowserType.CHROME, BrowserEnvironment.TEST,
                Duration.ofSeconds(30), Duration.ofSeconds(60));
        sessionId = handle.id();
        registry.acquirePermit();
        registry.register(handle);
    }

    @AfterEach
    void tearDown() {
        scheduler.shutdownNow();
    }

    @Test
    void firstAttachStartsWatchersAndLastDetachStopsThem() {
        Connection a = newConnection("alice-1");
        Connection b = newConnection("alice-2");

        coordinator.onSessionAttached(sessionId, a);
        assertThat(coordinator.isWatching(sessionId)).isTrue();

        coordinator.onSessionAttached(sessionId, b);
        assertThat(coordinator.isWatching(sessionId)).isTrue();

        coordinator.onSessionDetached(sessionId, a);
        // Still has b bound — must keep watching.
        assertThat(coordinator.isWatching(sessionId)).isTrue();

        coordinator.onSessionDetached(sessionId, b);
        assertThat(coordinator.isWatching(sessionId)).isFalse();
    }

    @Test
    void detachWithoutPriorAttachIsHarmless() {
        Connection a = newConnection("alice-1");
        coordinator.onSessionDetached(sessionId, a);
        assertThat(coordinator.isWatching(sessionId)).isFalse();
    }

    @Test
    void rapidAttachDetachInterleavingsConvergeCleanly() throws Exception {
        // Hammer the coordinator with concurrent attach/detach pairs and assert the final
        // state reconciles: empty connection set => no watchers, non-empty => watchers.
        int rounds = 200;
        Connection a = newConnection("alice-1");
        Connection b = newConnection("alice-2");

        java.util.concurrent.ExecutorService pool = java.util.concurrent.Executors.newFixedThreadPool(2);
        java.util.concurrent.CountDownLatch start = new java.util.concurrent.CountDownLatch(1);
        pool.submit(() -> {
            try { start.await(); } catch (InterruptedException ignored) {}
            for (int i = 0; i < rounds; i++) {
                coordinator.onSessionAttached(sessionId, a);
                coordinator.onSessionDetached(sessionId, a);
            }
        });
        pool.submit(() -> {
            try { start.await(); } catch (InterruptedException ignored) {}
            for (int i = 0; i < rounds; i++) {
                coordinator.onSessionAttached(sessionId, b);
                coordinator.onSessionDetached(sessionId, b);
            }
        });
        start.countDown();
        pool.shutdown();
        assertThat(pool.awaitTermination(10, java.util.concurrent.TimeUnit.SECONDS)).isTrue();

        // Final state: no connections, no watchers.
        assertThat(connections.isTracked(sessionId)).isFalse();
        assertThat(coordinator.isWatching(sessionId)).isFalse();
    }

    @Test
    void attachToVanishedSessionIsCleanedUp() {
        Connection a = newConnection("alice-1");
        registry.remove(sessionId);

        coordinator.onSessionAttached(sessionId, a);

        assertThat(coordinator.isWatching(sessionId)).isFalse();
        assertThat(connections.isTracked(sessionId)).isFalse();
    }

    private Connection newConnection(String id) {
        WebSocketSession ws = mock(WebSocketSession.class);
        when(ws.isOpen()).thenReturn(true);
        ConcurrentWebSocketSessionDecorator out = new ConcurrentWebSocketSessionDecorator(ws, 1000, 64 * 1024);
        return new Connection(CallerId.parse("alice"), id, out,
                Executors.newSingleThreadExecutor(),
                new Semaphore(8));
    }

    private static EngineProperties props() {
        return new EngineProperties(
                new EngineProperties.SessionProps(10, 60, 5, 5000),
                new EngineProperties.SeleniumProps("", 0, 0, 0, false, 0),
                new EngineProperties.AppiumProps("", "", "", 0, 0),
                new EngineProperties.BrowserStackProps(false, "", "", "", "", "", "", "", "", "", "", "", false, false, false),
                new EngineProperties.WebSocketProps("/v1/ws/sessions", 32, 300, 64, 10000, true, 250, true, 1000, true, 2000, 50));
    }
}
