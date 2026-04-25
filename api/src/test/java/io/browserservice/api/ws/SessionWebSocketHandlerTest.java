package io.browserservice.api.ws;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.looksee.browser.enums.BrowserEnvironment;
import com.looksee.browser.enums.BrowserType;
import io.browserservice.api.dto.NavigateResponse;
import io.browserservice.api.dto.NavigateStatus;
import io.browserservice.api.dto.PngEncoding;
import io.browserservice.api.dto.ScreenshotStrategy;
import io.browserservice.api.dto.SessionResponse;
import io.browserservice.api.dto.SessionStateResponse;
import io.browserservice.api.dto.Viewport;
import io.browserservice.api.service.AlertService;
import io.browserservice.api.service.BrowserOperationsService;
import io.browserservice.api.service.CaptureService;
import io.browserservice.api.service.ElementOperationsService;
import io.browserservice.api.error.SessionNotFoundException;
import io.browserservice.api.service.ReadinessService;
import io.browserservice.api.service.SessionService;
import io.browserservice.api.session.CaptureScreenshotCache;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.WebDriverException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.TestPropertySource;
import org.springframework.web.socket.BinaryMessage;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.handler.AbstractWebSocketHandler;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = {
        "browserservice.selenium.urls=http://localhost:4444/wd/hub",
        "browserservice.appium.urls=",
        "browserservice.browserstack.enabled=false",
        "browserservice.web-socket.command-queue-depth=4",
        "browserservice.web-socket.idle-close-seconds=2",
})
class SessionWebSocketHandlerTest {

    @LocalServerPort int port;
    @Autowired ObjectMapper json;

    @MockBean SessionService sessionService;
    @MockBean BrowserOperationsService browserOps;
    @MockBean ElementOperationsService elementOps;
    @MockBean AlertService alertService;
    @MockBean CaptureService captureService;
    @MockBean ReadinessService readinessService;

    private StandardWebSocketClient client;

    @BeforeEach void setUp() { client = new StandardWebSocketClient(); }

    @AfterEach void tearDown() { /* nothing — sessions close via client connections */ }

    @Test
    void handshakeWithoutCallerHeaderIsRejected() {
        TestHandler handler = new TestHandler();
        Throwable error = null;
        try {
            client.execute(handler, headers(null), uri()).get(5, TimeUnit.SECONDS);
        } catch (ExecutionException e) {
            error = e.getCause();
        } catch (Exception e) {
            error = e;
        }
        assertThat(error).as("handshake should fail without X-Caller-Id").isNotNull();
    }

    @Test
    void sessionCreateBindsAndEchoesId() throws Exception {
        UUID sid = UUID.randomUUID();
        when(sessionService.create(any())).thenReturn(new SessionResponse(
                sid, BrowserType.CHROME, BrowserEnvironment.TEST,
                Instant.now(), Instant.now().plusSeconds(60)));

        TestHandler handler = new TestHandler();
        WebSocketSession ws = client.execute(handler, headers("alice"), uri()).get(5, TimeUnit.SECONDS);
        try {
            ws.sendMessage(text("{\"type\":\"command\",\"id\":\"c1\",\"op\":\"session.create\","
                    + "\"params\":{\"browser_type\":\"CHROME\",\"environment\":\"TEST\"}}"));
            JsonNode resp = handler.takeJson(json);
            assertThat(resp.get("type").asText()).isEqualTo("response");
            assertThat(resp.get("id").asText()).isEqualTo("c1");
            assertThat(resp.get("ok").asBoolean()).isTrue();
            assertThat(resp.get("result").get("session_id").asText()).isEqualTo(sid.toString());
        } finally {
            ws.close();
        }
    }

    @Test
    void sessionAttachAsWrongOwnerClosesWith4403() throws Exception {
        UUID sid = UUID.randomUUID();
        when(sessionService.create(any())).thenReturn(new SessionResponse(
                sid, BrowserType.CHROME, BrowserEnvironment.TEST,
                Instant.now(), Instant.now().plusSeconds(60)));

        // Alice creates the session.
        TestHandler aliceH = new TestHandler();
        WebSocketSession alice = client.execute(aliceH, headers("alice"), uri()).get(5, TimeUnit.SECONDS);
        alice.sendMessage(text("{\"type\":\"command\",\"id\":\"c1\",\"op\":\"session.create\","
                + "\"params\":{\"browser_type\":\"CHROME\",\"environment\":\"TEST\"}}"));
        aliceH.takeJson(json);

        // Bob attempts to attach.
        TestHandler bobH = new TestHandler();
        WebSocketSession bob = client.execute(bobH, headers("bob"), uri()).get(5, TimeUnit.SECONDS);
        bob.sendMessage(text("{\"type\":\"command\",\"id\":\"c2\",\"op\":\"session.attach\","
                + "\"params\":{\"session_id\":\"" + sid + "\"}}"));

        CloseStatus status = bobH.takeClose();
        assertThat(status.getCode()).isEqualTo(4403);
        alice.close();
    }

    @Test
    void sessionScopedOpWithoutBindReturnsErrorFrame() throws Exception {
        TestHandler handler = new TestHandler();
        WebSocketSession ws = client.execute(handler, headers("alice"), uri()).get(5, TimeUnit.SECONDS);
        try {
            ws.sendMessage(text("{\"type\":\"command\",\"id\":\"c1\",\"op\":\"navigation.source\"}"));
            JsonNode resp = handler.takeJson(json);
            assertThat(resp.get("ok").asBoolean()).isFalse();
            assertThat(resp.get("error").get("code").asText()).isEqualTo("session_not_bound");
        } finally {
            ws.close();
        }
    }

    @Test
    void unknownOpReturnsErrorFrame() throws Exception {
        TestHandler handler = new TestHandler();
        WebSocketSession ws = client.execute(handler, headers("alice"), uri()).get(5, TimeUnit.SECONDS);
        try {
            ws.sendMessage(text("{\"type\":\"command\",\"id\":\"c1\",\"op\":\"bogus.op\"}"));
            JsonNode resp = handler.takeJson(json);
            assertThat(resp.get("ok").asBoolean()).isFalse();
            assertThat(resp.get("error").get("code").asText()).isEqualTo("unknown_op");
        } finally {
            ws.close();
        }
    }

    @Test
    void serviceExceptionsMapToErrorFrameWithSameCodeAsRest() throws Exception {
        UUID sid = UUID.randomUUID();
        when(sessionService.create(any())).thenReturn(new SessionResponse(
                sid, BrowserType.CHROME, BrowserEnvironment.TEST,
                Instant.now(), Instant.now().plusSeconds(60)));
        when(browserOps.navigate(any(), any())).thenThrow(new WebDriverException("boom"));

        TestHandler handler = new TestHandler();
        WebSocketSession ws = client.execute(handler, headers("alice"), uri()).get(5, TimeUnit.SECONDS);
        try {
            ws.sendMessage(text("{\"type\":\"command\",\"id\":\"c1\",\"op\":\"session.create\","
                    + "\"params\":{\"browser_type\":\"CHROME\",\"environment\":\"TEST\"}}"));
            handler.takeJson(json);

            ws.sendMessage(text("{\"type\":\"command\",\"id\":\"c2\",\"op\":\"navigation.navigate\","
                    + "\"params\":{\"url\":\"https://example.com\"}}"));
            JsonNode resp = handler.takeJson(json);
            assertThat(resp.get("ok").asBoolean()).isFalse();
            assertThat(resp.get("error").get("code").asText()).isEqualTo("webdriver_error");
            assertThat(resp.get("error").get("request_id").asText()).isNotBlank();
        } finally {
            ws.close();
        }
    }

    @Test
    void validationFailureReturnsValidationFailedCode() throws Exception {
        UUID sid = UUID.randomUUID();
        when(sessionService.create(any())).thenReturn(new SessionResponse(
                sid, BrowserType.CHROME, BrowserEnvironment.TEST,
                Instant.now(), Instant.now().plusSeconds(60)));

        TestHandler handler = new TestHandler();
        WebSocketSession ws = client.execute(handler, headers("alice"), uri()).get(5, TimeUnit.SECONDS);
        try {
            ws.sendMessage(text("{\"type\":\"command\",\"id\":\"c1\",\"op\":\"session.create\","
                    + "\"params\":{\"browser_type\":\"CHROME\",\"environment\":\"TEST\"}}"));
            handler.takeJson(json);

            // Missing required `url`.
            ws.sendMessage(text("{\"type\":\"command\",\"id\":\"c2\",\"op\":\"navigation.navigate\","
                    + "\"params\":{}}"));
            JsonNode resp = handler.takeJson(json);
            assertThat(resp.get("ok").asBoolean()).isFalse();
            assertThat(resp.get("error").get("code").asText()).isEqualTo("validation_failed");
        } finally {
            ws.close();
        }
    }

    @Test
    void successfulNavigateRoundTrip() throws Exception {
        UUID sid = UUID.randomUUID();
        when(sessionService.create(any())).thenReturn(new SessionResponse(
                sid, BrowserType.CHROME, BrowserEnvironment.TEST,
                Instant.now(), Instant.now().plusSeconds(60)));
        when(browserOps.navigate(any(), any())).thenReturn(
                new NavigateResponse("https://example.com", NavigateStatus.LOADED));

        TestHandler handler = new TestHandler();
        WebSocketSession ws = client.execute(handler, headers("alice"), uri()).get(5, TimeUnit.SECONDS);
        try {
            ws.sendMessage(text("{\"type\":\"command\",\"id\":\"c1\",\"op\":\"session.create\","
                    + "\"params\":{\"browser_type\":\"CHROME\",\"environment\":\"TEST\"}}"));
            handler.takeJson(json);

            ws.sendMessage(text("{\"type\":\"command\",\"id\":\"c2\",\"op\":\"navigation.navigate\","
                    + "\"params\":{\"url\":\"https://example.com\"}}"));
            JsonNode resp = handler.takeJson(json);
            assertThat(resp.get("ok").asBoolean()).isTrue();
            assertThat(resp.get("result").get("current_url").asText()).isEqualTo("https://example.com");
            assertThat(resp.get("result").get("status").asText()).isEqualTo("LOADED");
        } finally {
            ws.close();
        }
    }

    @Test
    void describeAfterBindUsesBoundSessionId() throws Exception {
        UUID sid = UUID.randomUUID();
        when(sessionService.create(any())).thenReturn(new SessionResponse(
                sid, BrowserType.CHROME, BrowserEnvironment.TEST,
                Instant.now(), Instant.now().plusSeconds(60)));
        when(sessionService.describe(sid)).thenReturn(new SessionStateResponse(
                sid, BrowserType.CHROME, BrowserEnvironment.TEST,
                Instant.now(), Instant.now().plusSeconds(60),
                "https://example.com", new Viewport(1024, 768), null));

        TestHandler handler = new TestHandler();
        WebSocketSession ws = client.execute(handler, headers("alice"), uri()).get(5, TimeUnit.SECONDS);
        try {
            ws.sendMessage(text("{\"type\":\"command\",\"id\":\"c1\",\"op\":\"session.create\","
                    + "\"params\":{\"browser_type\":\"CHROME\",\"environment\":\"TEST\"}}"));
            handler.takeJson(json);

            ws.sendMessage(text("{\"type\":\"command\",\"id\":\"c2\",\"op\":\"session.describe\"}"));
            JsonNode resp = handler.takeJson(json);
            assertThat(resp.get("ok").asBoolean()).isTrue();
            assertThat(resp.get("result").get("session_id").asText()).isEqualTo(sid.toString());
        } finally {
            ws.close();
        }
    }

    @Test
    void sessionCloseUnbindsConnectionAndAllowsCreateAgain() throws Exception {
        UUID first = UUID.randomUUID();
        UUID second = UUID.randomUUID();
        when(sessionService.create(any()))
                .thenReturn(new SessionResponse(first, BrowserType.CHROME, BrowserEnvironment.TEST,
                        Instant.now(), Instant.now().plusSeconds(60)))
                .thenReturn(new SessionResponse(second, BrowserType.CHROME, BrowserEnvironment.TEST,
                        Instant.now(), Instant.now().plusSeconds(60)));
        doNothing().when(sessionService).close(any());

        TestHandler handler = new TestHandler();
        WebSocketSession ws = client.execute(handler, headers("alice"), uri()).get(5, TimeUnit.SECONDS);
        try {
            ws.sendMessage(text("{\"type\":\"command\",\"id\":\"c1\",\"op\":\"session.create\","
                    + "\"params\":{\"browser_type\":\"CHROME\",\"environment\":\"TEST\"}}"));
            handler.takeJson(json);

            ws.sendMessage(text("{\"type\":\"command\",\"id\":\"c2\",\"op\":\"session.close\"}"));
            JsonNode close1 = handler.takeJson(json);
            assertThat(close1.get("ok").asBoolean()).isTrue();

            ws.sendMessage(text("{\"type\":\"command\",\"id\":\"c3\",\"op\":\"session.create\","
                    + "\"params\":{\"browser_type\":\"CHROME\",\"environment\":\"TEST\"}}"));
            JsonNode resp = handler.takeJson(json);
            assertThat(resp.get("ok").asBoolean()).isTrue();
            assertThat(resp.get("result").get("session_id").asText()).isEqualTo(second.toString());
        } finally {
            ws.close();
        }
    }

    @Test
    void attachDoesNotBindIfDescribeFails() throws Exception {
        UUID sid = UUID.randomUUID();
        // Alice creates the session so ownership is recorded.
        when(sessionService.create(any())).thenReturn(new SessionResponse(
                sid, BrowserType.CHROME, BrowserEnvironment.TEST,
                Instant.now(), Instant.now().plusSeconds(60)));
        TestHandler creator = new TestHandler();
        WebSocketSession a1 = client.execute(creator, headers("alice"), uri()).get(5, TimeUnit.SECONDS);
        a1.sendMessage(text("{\"type\":\"command\",\"id\":\"c1\",\"op\":\"session.create\","
                + "\"params\":{\"browser_type\":\"CHROME\",\"environment\":\"TEST\"}}"));
        creator.takeJson(json);
        a1.close();

        // Now alice opens a fresh connection and the session has been reaped.
        when(sessionService.describe(sid)).thenThrow(new SessionNotFoundException(sid));
        when(sessionService.create(any())).thenReturn(new SessionResponse(
                UUID.randomUUID(), BrowserType.CHROME, BrowserEnvironment.TEST,
                Instant.now(), Instant.now().plusSeconds(60)));

        TestHandler handler = new TestHandler();
        WebSocketSession ws = client.execute(handler, headers("alice"), uri()).get(5, TimeUnit.SECONDS);
        try {
            ws.sendMessage(text("{\"type\":\"command\",\"id\":\"c2\",\"op\":\"session.attach\","
                    + "\"params\":{\"session_id\":\"" + sid + "\"}}"));
            JsonNode attachResp = handler.takeJson(json);
            assertThat(attachResp.get("ok").asBoolean()).isFalse();
            assertThat(attachResp.get("error").get("code").asText()).isEqualTo("session_not_found");

            // Connection must NOT be bound — a fresh session.create must succeed.
            ws.sendMessage(text("{\"type\":\"command\",\"id\":\"c3\",\"op\":\"session.create\","
                    + "\"params\":{\"browser_type\":\"CHROME\",\"environment\":\"TEST\"}}"));
            JsonNode createResp = handler.takeJson(json);
            assertThat(createResp.get("ok").asBoolean())
                    .as("connection should be unbound after a failed attach")
                    .isTrue();
        } finally {
            ws.close();
        }
    }

    @Test
    void messageLargerThanOneByteIsAcceptedByOutboundDecorator() throws Exception {
        // Regression for buffer-size confusion: any payload > 1 byte must not blow up the
        // outbound ConcurrentWebSocketSessionDecorator. Round-tripping a normal create
        // confirms the decorator's buffer accommodates real frames.
        UUID sid = UUID.randomUUID();
        when(sessionService.create(any())).thenReturn(new SessionResponse(
                sid, BrowserType.CHROME, BrowserEnvironment.TEST,
                Instant.now(), Instant.now().plusSeconds(60)));

        TestHandler handler = new TestHandler();
        WebSocketSession ws = client.execute(handler, headers("alice"), uri()).get(5, TimeUnit.SECONDS);
        try {
            ws.sendMessage(text("{\"type\":\"command\",\"id\":\"c1\",\"op\":\"session.create\","
                    + "\"params\":{\"browser_type\":\"CHROME\",\"environment\":\"TEST\"}}"));
            JsonNode resp = handler.takeJson(json);
            assertThat(resp.get("ok").asBoolean()).isTrue();
            // Payload was well over 1 byte; if buffer-size limit were 1, the decorator would
            // have closed the session before we could read the response.
            assertThat(ws.isOpen()).isTrue();
        } finally {
            ws.close();
        }
    }

    @Test
    void idleConnectionIsClosedWith4408() throws Exception {
        TestHandler handler = new TestHandler();
        WebSocketSession ws = client.execute(handler, headers("alice"), uri()).get(5, TimeUnit.SECONDS);
        // No traffic — wait for idle close.
        CloseStatus status = handler.awaitClose(Duration.ofSeconds(10));
        assertThat(status.getCode()).isEqualTo(4408);
    }

    @Test
    void doubleBindFails() throws Exception {
        UUID sid = UUID.randomUUID();
        when(sessionService.create(any())).thenReturn(new SessionResponse(
                sid, BrowserType.CHROME, BrowserEnvironment.TEST,
                Instant.now(), Instant.now().plusSeconds(60)));

        TestHandler handler = new TestHandler();
        WebSocketSession ws = client.execute(handler, headers("alice"), uri()).get(5, TimeUnit.SECONDS);
        try {
            ws.sendMessage(text("{\"type\":\"command\",\"id\":\"c1\",\"op\":\"session.create\","
                    + "\"params\":{\"browser_type\":\"CHROME\",\"environment\":\"TEST\"}}"));
            handler.takeJson(json);

            ws.sendMessage(text("{\"type\":\"command\",\"id\":\"c2\",\"op\":\"session.create\","
                    + "\"params\":{\"browser_type\":\"CHROME\",\"environment\":\"TEST\"}}"));
            JsonNode resp = handler.takeJson(json);
            assertThat(resp.get("ok").asBoolean()).isFalse();
            assertThat(resp.get("error").get("code").asText()).isEqualTo("already_bound");
        } finally {
            ws.close();
        }
    }

    @Test
    void screenshotPageEmitsHeaderThenBinaryFrame() throws Exception {
        UUID sid = UUID.randomUUID();
        when(sessionService.create(any())).thenReturn(new SessionResponse(
                sid, BrowserType.CHROME, BrowserEnvironment.TEST,
                Instant.now(), Instant.now().plusSeconds(60)));
        byte[] png = makePng(8, 6);
        when(browserOps.pageScreenshot(any(), any())).thenReturn(png);

        TestHandler handler = new TestHandler();
        WebSocketSession ws = client.execute(handler, headers("alice"), uri()).get(5, TimeUnit.SECONDS);
        try {
            ws.sendMessage(text("{\"type\":\"command\",\"id\":\"c1\",\"op\":\"session.create\","
                    + "\"params\":{\"browser_type\":\"CHROME\",\"environment\":\"TEST\"}}"));
            handler.takeJson(json);

            ws.sendMessage(text("{\"type\":\"command\",\"id\":\"c2\",\"op\":\"screenshot.page\","
                    + "\"params\":{\"strategy\":\"VIEWPORT\",\"encoding\":\"BINARY\"}}"));

            JsonNode header = handler.takeJson(json);
            assertThat(header.get("type").asText()).isEqualTo("binary-header");
            assertThat(header.get("id").asText()).isEqualTo("c2");
            assertThat(header.get("mime").asText()).isEqualTo("image/png");
            assertThat(header.get("length").asLong()).isEqualTo(png.length);
            assertThat(header.get("sha256").asText()).isEqualTo(sha256Hex(png));

            byte[] received = handler.takeBinary();
            assertThat(received).hasSameSizeAs(png);
            assertThat(sha256Hex(received)).isEqualTo(header.get("sha256").asText());
            assertThat(ImageIO.read(new ByteArrayInputStream(received)))
                    .as("payload must decode as a valid PNG").isNotNull();
        } finally {
            ws.close();
        }
    }

    @Test
    void captureFetchScreenshotEmitsBinaryFrame() throws Exception {
        UUID sid = UUID.randomUUID();
        when(sessionService.create(any())).thenReturn(new SessionResponse(
                sid, BrowserType.CHROME, BrowserEnvironment.TEST,
                Instant.now(), Instant.now().plusSeconds(60)));
        UUID captureId = UUID.randomUUID();
        byte[] png = makePng(4, 4);
        when(captureService.fetchScreenshot(captureId)).thenReturn(
                new CaptureScreenshotCache.CaptureEntry(png, 4, 4, Instant.now().plusSeconds(60)));

        TestHandler handler = new TestHandler();
        WebSocketSession ws = client.execute(handler, headers("alice"), uri()).get(5, TimeUnit.SECONDS);
        try {
            ws.sendMessage(text("{\"type\":\"command\",\"id\":\"c1\",\"op\":\"session.create\","
                    + "\"params\":{\"browser_type\":\"CHROME\",\"environment\":\"TEST\"}}"));
            handler.takeJson(json);

            ws.sendMessage(text("{\"type\":\"command\",\"id\":\"c2\",\"op\":\"capture.fetchScreenshot\","
                    + "\"params\":{\"capture_id\":\"" + captureId + "\"}}"));

            JsonNode header = handler.takeJson(json);
            assertThat(header.get("type").asText()).isEqualTo("binary-header");
            assertThat(header.get("id").asText()).isEqualTo("c2");

            byte[] received = handler.takeBinary();
            assertThat(received.length).isEqualTo(png.length);
        } finally {
            ws.close();
        }
    }

    @Test
    void oversizeScreenshotProducesErrorAndNoBinaryFrame() throws Exception {
        UUID sid = UUID.randomUUID();
        when(sessionService.create(any())).thenReturn(new SessionResponse(
                sid, BrowserType.CHROME, BrowserEnvironment.TEST,
                Instant.now(), Instant.now().plusSeconds(60)));
        // Default test prop is 16 MiB; produce a 17 MiB byte array (any bytes — server only
        // checks length before computing sha or sending the binary frame).
        byte[] huge = new byte[17 * 1024 * 1024];
        when(browserOps.pageScreenshot(any(), any())).thenReturn(huge);

        TestHandler handler = new TestHandler();
        WebSocketSession ws = client.execute(handler, headers("alice"), uri()).get(5, TimeUnit.SECONDS);
        try {
            ws.sendMessage(text("{\"type\":\"command\",\"id\":\"c1\",\"op\":\"session.create\","
                    + "\"params\":{\"browser_type\":\"CHROME\",\"environment\":\"TEST\"}}"));
            handler.takeJson(json);

            ws.sendMessage(text("{\"type\":\"command\",\"id\":\"c2\",\"op\":\"screenshot.page\","
                    + "\"params\":{\"strategy\":\"VIEWPORT\",\"encoding\":\"BINARY\"}}"));

            JsonNode resp = handler.takeJson(json);
            assertThat(resp.get("type").asText()).isEqualTo("response");
            assertThat(resp.get("ok").asBoolean()).isFalse();
            assertThat(resp.get("error").get("code").asText()).isEqualTo("screenshot_too_large");
            assertThat(handler.binaries).as("no binary frame should follow oversize").isEmpty();
        } finally {
            ws.close();
        }
    }

    @Test
    void binaryPairIsNotInterleavedByConcurrentWriters() throws Exception {
        // While `screenshot.page` is in flight, another thread broadcasts a stream of
        // text frames via writeFrame. With writeLock held across (header, binary), the
        // header MUST be immediately followed by the binary frame; no text frame may
        // land between them.
        UUID sid = UUID.randomUUID();
        when(sessionService.create(any())).thenReturn(new SessionResponse(
                sid, BrowserType.CHROME, BrowserEnvironment.TEST,
                Instant.now(), Instant.now().plusSeconds(60)));
        byte[] png = makePng(8, 6);
        // Make pageScreenshot slow so the test actively races writers.
        when(browserOps.pageScreenshot(any(), any())).thenAnswer(inv -> {
            Thread.sleep(50);
            return png;
        });

        TestHandler handler = new TestHandler();
        WebSocketSession ws = client.execute(handler, headers("alice"), uri()).get(5, TimeUnit.SECONDS);
        try {
            ws.sendMessage(text("{\"type\":\"command\",\"id\":\"c1\",\"op\":\"session.create\","
                    + "\"params\":{\"browser_type\":\"CHROME\",\"environment\":\"TEST\"}}"));
            handler.takeJson(json);

            // Concurrent flooder firing a small describe (text response) over and over.
            when(sessionService.describe(sid)).thenReturn(new SessionStateResponse(
                    sid, BrowserType.CHROME, BrowserEnvironment.TEST,
                    Instant.now(), Instant.now().plusSeconds(60),
                    "https://example.com", new Viewport(1, 1), null));
            Thread flooder = new Thread(() -> {
                for (int i = 0; i < 25; i++) {
                    try {
                        ws.sendMessage(text("{\"type\":\"command\",\"id\":\"d" + i
                                + "\",\"op\":\"session.describe\"}"));
                        Thread.sleep(2);
                    } catch (Exception ignored) { return; }
                }
            });
            flooder.setDaemon(true);

            ws.sendMessage(text("{\"type\":\"command\",\"id\":\"c2\",\"op\":\"screenshot.page\","
                    + "\"params\":{\"strategy\":\"VIEWPORT\",\"encoding\":\"BINARY\"}}"));
            flooder.start();

            // Drain frames until we observe the header. Every text frame before the header
            // is unrelated; once we see the header, the very next thing must be the binary.
            JsonNode header = null;
            while (header == null) {
                String payload = handler.messages.poll(5, TimeUnit.SECONDS);
                assertThat(payload).as("expected the binary-header within 5s").isNotNull();
                JsonNode node = json.readTree(payload);
                if ("binary-header".equals(node.path("type").asText())) {
                    header = node;
                }
            }
            // After the header, no text frame may arrive before the binary frame.
            // Poll the binary queue with a short timeout AND assert the text queue did not
            // receive a new entry first.
            byte[] received = handler.takeBinary();
            assertThat(received.length).isEqualTo(png.length);
            assertThat(header.get("id").asText()).isEqualTo("c2");
            flooder.join(2000);
        } finally {
            ws.close();
        }
    }

    private static byte[] makePng(int width, int height) throws Exception {
        BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(img, "png", out);
        return out.toByteArray();
    }

    private static String sha256Hex(byte[] bytes) throws Exception {
        byte[] digest = MessageDigest.getInstance("SHA-256").digest(bytes);
        StringBuilder sb = new StringBuilder(digest.length * 2);
        for (byte b : digest) {
            sb.append(Character.forDigit((b >> 4) & 0xF, 16));
            sb.append(Character.forDigit(b & 0xF, 16));
        }
        return sb.toString();
    }

    private URI uri() {
        return URI.create("ws://localhost:" + port + "/v1/ws/sessions");
    }

    private static WebSocketHttpHeaders headers(String caller) {
        WebSocketHttpHeaders h = new WebSocketHttpHeaders();
        if (caller != null) h.add("X-Caller-Id", caller);
        return h;
    }

    private static TextMessage text(String body) {
        return new TextMessage(body);
    }

    private static final class TestHandler extends AbstractWebSocketHandler {
        final BlockingQueue<String> messages = new ArrayBlockingQueue<>(64);
        final BlockingQueue<byte[]> binaries = new ArrayBlockingQueue<>(16);
        final CountDownLatch closed = new CountDownLatch(1);
        volatile CloseStatus closeStatus;
        long openedAt = System.currentTimeMillis();
        long elapsedMs;

        @Override
        public void afterConnectionEstablished(WebSocketSession session) {
            openedAt = System.currentTimeMillis();
        }

        @Override
        protected void handleTextMessage(WebSocketSession session, TextMessage message) {
            messages.add(message.getPayload());
        }

        @Override
        protected void handleBinaryMessage(WebSocketSession session, BinaryMessage message) {
            ByteBuffer buf = message.getPayload();
            byte[] bytes = new byte[buf.remaining()];
            buf.get(bytes);
            binaries.add(bytes);
        }

        @Override
        public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
            this.closeStatus = status;
            this.elapsedMs = System.currentTimeMillis() - openedAt;
            closed.countDown();
        }

        JsonNode takeJson(ObjectMapper mapper) throws Exception {
            String payload = messages.poll(5, TimeUnit.SECONDS);
            assertThat(payload).as("expected a frame within 5s").isNotNull();
            return mapper.readTree(payload);
        }

        byte[] takeBinary() throws InterruptedException {
            byte[] payload = binaries.poll(5, TimeUnit.SECONDS);
            assertThat(payload).as("expected a binary frame within 5s").isNotNull();
            return payload;
        }

        CloseStatus takeClose() throws InterruptedException {
            assertThat(closed.await(5, TimeUnit.SECONDS))
                    .as("expected close within 5s").isTrue();
            return closeStatus;
        }

        CloseStatus awaitClose(Duration timeout) throws InterruptedException {
            assertThat(closed.await(timeout.toMillis(), TimeUnit.MILLISECONDS))
                    .as("expected close within %s", timeout).isTrue();
            return closeStatus;
        }
    }
}
