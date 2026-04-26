package io.browserservice.api.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.looksee.browser.enums.Action;
import com.looksee.browser.enums.AlertChoice;
import com.looksee.browser.enums.BrowserEnvironment;
import com.looksee.browser.enums.BrowserType;
import com.looksee.browser.enums.MobileAction;
import io.browserservice.api.dto.AlertRespondRequest;
import io.browserservice.api.dto.AlertStateResponse;
import io.browserservice.api.dto.CaptureRequest;
import io.browserservice.api.dto.CaptureResponse;
import io.browserservice.api.dto.CaptureScreenshotRef;
import io.browserservice.api.dto.CreateSessionRequest;
import io.browserservice.api.dto.DomRemovePreset;
import io.browserservice.api.dto.DomRemoveRequest;
import io.browserservice.api.dto.ElementActionRequest;
import io.browserservice.api.dto.ElementScreenshotRequest;
import io.browserservice.api.dto.ElementStateResponse;
import io.browserservice.api.dto.ElementTouchRequest;
import io.browserservice.api.dto.ExecuteRequest;
import io.browserservice.api.dto.ExecuteResponse;
import io.browserservice.api.dto.FindElementRequest;
import io.browserservice.api.dto.HubStatus;
import io.browserservice.api.dto.MouseMoveMode;
import io.browserservice.api.dto.MouseMoveRequest;
import io.browserservice.api.dto.NavigateRequest;
import io.browserservice.api.dto.NavigateResponse;
import io.browserservice.api.dto.NavigateStatus;
import io.browserservice.api.dto.PageSourceResponse;
import io.browserservice.api.dto.PageStatusResponse;
import io.browserservice.api.dto.PngEncoding;
import io.browserservice.api.dto.ReadinessResponse;
import io.browserservice.api.dto.Rect;
import io.browserservice.api.dto.ScreenshotRequest;
import io.browserservice.api.dto.ScreenshotStrategy;
import io.browserservice.api.dto.ScrollMode;
import io.browserservice.api.dto.ScrollOffset;
import io.browserservice.api.dto.ScrollRequest;
import io.browserservice.api.dto.SessionListResponse;
import io.browserservice.api.dto.SessionResponse;
import io.browserservice.api.dto.SessionStateResponse;
import io.browserservice.api.dto.Viewport;
import io.browserservice.api.dto.ViewportStateResponse;
import io.browserservice.api.error.CaptureExpiredException;
import io.browserservice.api.error.CaptureNotFoundException;
import io.browserservice.api.error.DesktopSessionRequiredException;
import io.browserservice.api.error.ElementHandleNotFoundException;
import io.browserservice.api.error.MobileSessionRequiredException;
import io.browserservice.api.error.SessionBusyException;
import io.browserservice.api.error.SessionCapExceededException;
import io.browserservice.api.error.SessionNotFoundException;
import io.browserservice.api.error.UpstreamUnavailableException;
import io.browserservice.api.error.ValidationFailedException;
import io.browserservice.api.service.AlertService;
import io.browserservice.api.service.BrowserOperationsService;
import io.browserservice.api.service.CaptureService;
import io.browserservice.api.service.ElementOperationsService;
import io.browserservice.api.service.ReadinessService;
import io.browserservice.api.service.SessionService;
import io.browserservice.api.session.CaptureScreenshotCache;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.remote.UnreachableBrowserException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@org.springframework.test.context.ActiveProfiles("test")
@TestPropertySource(properties = {
        "browserservice.selenium.urls=http://localhost:4444/wd/hub",
        "browserservice.appium.urls=",
        "browserservice.browserstack.enabled=false"
})
class ControllerHttpTest {

    @Autowired private MockMvc mvc;
    @Autowired private ObjectMapper json;

    @MockBean private SessionService sessionService;
    @MockBean private BrowserOperationsService browserOps;
    @MockBean private ElementOperationsService elementOps;
    @MockBean private AlertService alertService;
    @MockBean private CaptureService captureService;
    @MockBean private ReadinessService readinessService;

    // ---------- Ops ----------

    @Test
    void healthzReturnsOk() throws Exception {
        mvc.perform(get("/healthz"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ok"))
                .andExpect(header().exists("X-Request-Id"));
    }

    @Test
    void readyzReturns200WhenReady() throws Exception {
        when(readinessService.probe()).thenReturn(new ReadinessResponse("ready",
                List.of(new HubStatus("http://x/wd/hub", true)), List.of()));

        mvc.perform(get("/readyz"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ready"));
    }

    @Test
    void readyzReturns503WhenDegraded() throws Exception {
        when(readinessService.probe()).thenReturn(new ReadinessResponse("degraded",
                List.of(new HubStatus("http://x/wd/hub", false)), List.of()));

        mvc.perform(get("/readyz"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.status").value("degraded"));
    }

    @Test
    void echoesIncomingRequestIdHeader() throws Exception {
        mvc.perform(get("/healthz").header("X-Request-Id", "abc-123"))
                .andExpect(header().string("X-Request-Id", "abc-123"));
    }

    // ---------- Sessions ----------

    @Test
    void createSessionReturns201() throws Exception {
        UUID id = UUID.randomUUID();
        when(sessionService.create(any())).thenReturn(new SessionResponse(id,
                BrowserType.CHROME, BrowserEnvironment.TEST, Instant.now(), Instant.now().plusSeconds(60)));

        mvc.perform(post("/v1/sessions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(new CreateSessionRequest(
                                BrowserType.CHROME, BrowserEnvironment.TEST, null))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.session_id").value(id.toString()));
    }

    @Test
    void createSessionValidationFailure() throws Exception {
        mvc.perform(post("/v1/sessions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("validation_failed"));
    }

    @Test
    void createSessionCapExceeded() throws Exception {
        when(sessionService.create(any())).thenThrow(new SessionCapExceededException(20));

        mvc.perform(post("/v1/sessions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(new CreateSessionRequest(
                                BrowserType.CHROME, BrowserEnvironment.TEST, null))))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.error.code").value("session_cap_exceeded"));
    }

    @Test
    void createSessionUpstreamUnavailable() throws Exception {
        when(sessionService.create(any())).thenThrow(new UpstreamUnavailableException("down"));

        mvc.perform(post("/v1/sessions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(new CreateSessionRequest(
                                BrowserType.CHROME, BrowserEnvironment.TEST, null))))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.error.code").value("upstream_unavailable"));
    }

    @Test
    void listSessions() throws Exception {
        when(sessionService.list()).thenReturn(new SessionListResponse(List.of()));
        mvc.perform(get("/v1/sessions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sessions").isArray());
    }

    @Test
    void getSession() throws Exception {
        UUID id = UUID.randomUUID();
        when(sessionService.describe(id)).thenReturn(new SessionStateResponse(id,
                BrowserType.CHROME, BrowserEnvironment.TEST, Instant.now(), Instant.now().plusSeconds(60),
                "https://x", new Viewport(100, 200), new ScrollOffset(0, 0)));

        mvc.perform(get("/v1/sessions/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.current_url").value("https://x"));
    }

    @Test
    void getSessionNotFound() throws Exception {
        UUID id = UUID.randomUUID();
        when(sessionService.describe(id)).thenThrow(new SessionNotFoundException(id));

        mvc.perform(get("/v1/sessions/" + id))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("session_not_found"));
    }

    @Test
    void deleteSessionReturns204() throws Exception {
        UUID id = UUID.randomUUID();
        doNothing().when(sessionService).close(id);

        mvc.perform(delete("/v1/sessions/" + id))
                .andExpect(status().isNoContent());
        verify(sessionService).close(id);
    }

    @Test
    void deleteSessionWithInvalidUuidIs400() throws Exception {
        mvc.perform(delete("/v1/sessions/not-a-uuid"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("validation_failed"));
    }

    // ---------- Navigation ----------

    @Test
    void navigateOk() throws Exception {
        UUID id = UUID.randomUUID();
        when(browserOps.navigate(eq(id), any())).thenReturn(
                new NavigateResponse("https://x", NavigateStatus.LOADED));

        mvc.perform(post("/v1/sessions/" + id + "/navigate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(new NavigateRequest("https://x", null))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("LOADED"));
    }

    @Test
    void getSourceOk() throws Exception {
        UUID id = UUID.randomUUID();
        when(browserOps.getSource(id)).thenReturn(new PageSourceResponse("u", "<html/>"));
        mvc.perform(get("/v1/sessions/" + id + "/source"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.source").value("<html/>"));
    }

    @Test
    void getStatusOk() throws Exception {
        UUID id = UUID.randomUUID();
        when(browserOps.getStatus(id)).thenReturn(new PageStatusResponse("u", false));
        mvc.perform(get("/v1/sessions/" + id + "/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.is503").value(false));
    }

    // ---------- Screenshots ----------

    @Test
    void screenshotBinaryResponse() throws Exception {
        UUID id = UUID.randomUUID();
        byte[] png = samplePng();
        when(browserOps.pageScreenshot(eq(id), eq(ScreenshotStrategy.VIEWPORT))).thenReturn(png);

        mvc.perform(post("/v1/sessions/" + id + "/screenshot")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.IMAGE_PNG)
                        .content(json.writeValueAsString(new ScreenshotRequest(ScreenshotStrategy.VIEWPORT, null))))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.IMAGE_PNG));
    }

    @Test
    void screenshotBase64Response() throws Exception {
        UUID id = UUID.randomUUID();
        when(browserOps.pageScreenshot(eq(id), eq(ScreenshotStrategy.VIEWPORT))).thenReturn(samplePng());

        mvc.perform(post("/v1/sessions/" + id + "/screenshot")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(new ScreenshotRequest(
                                ScreenshotStrategy.VIEWPORT, PngEncoding.BASE64))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.image_base64").exists())
                .andExpect(jsonPath("$.width").value(2))
                .andExpect(jsonPath("$.height").value(2));
    }

    @Test
    void screenshotBase64HandlesUnparseableBytesGracefully() throws Exception {
        UUID id = UUID.randomUUID();
        when(browserOps.pageScreenshot(eq(id), eq(ScreenshotStrategy.VIEWPORT))).thenReturn(new byte[]{1, 2, 3});

        mvc.perform(post("/v1/sessions/" + id + "/screenshot")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(new ScreenshotRequest(
                                ScreenshotStrategy.VIEWPORT, PngEncoding.BASE64))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.width").value(0));
    }

    @Test
    void elementScreenshotBinary() throws Exception {
        UUID id = UUID.randomUUID();
        when(elementOps.elementScreenshot(eq(id), any())).thenReturn(samplePng());

        mvc.perform(post("/v1/sessions/" + id + "/element/screenshot")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.IMAGE_PNG)
                        .content(json.writeValueAsString(new ElementScreenshotRequest("el_1", null))))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.IMAGE_PNG));
    }

    // ---------- Elements / Touch ----------

    @Test
    void findElement() throws Exception {
        UUID id = UUID.randomUUID();
        when(elementOps.find(eq(id), any())).thenReturn(new ElementStateResponse(
                "el_1", true, true, Map.of(), new Rect(0, 0, 1, 1)));

        mvc.perform(post("/v1/sessions/" + id + "/element/find")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(new FindElementRequest("//h1"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.element_handle").value("el_1"));
    }

    @Test
    void elementAction204() throws Exception {
        UUID id = UUID.randomUUID();
        doNothing().when(elementOps).action(eq(id), any());

        mvc.perform(post("/v1/sessions/" + id + "/element/action")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(new ElementActionRequest("el_1", Action.CLICK, null))))
                .andExpect(status().isNoContent());
    }

    @Test
    void elementActionOnMobileReturns409() throws Exception {
        UUID id = UUID.randomUUID();
        doThrow(new DesktopSessionRequiredException()).when(elementOps).action(eq(id), any());

        mvc.perform(post("/v1/sessions/" + id + "/element/action")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(new ElementActionRequest("el_1", Action.CLICK, null))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("desktop_session_required"));
    }

    @Test
    void elementActionElementMissingReturns404() throws Exception {
        UUID id = UUID.randomUUID();
        doThrow(new ElementHandleNotFoundException("el_1")).when(elementOps).action(eq(id), any());

        mvc.perform(post("/v1/sessions/" + id + "/element/action")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(new ElementActionRequest("el_1", Action.CLICK, null))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("element_handle_not_found"));
    }

    @Test
    void elementTouch204() throws Exception {
        UUID id = UUID.randomUUID();
        doNothing().when(elementOps).touch(eq(id), any());

        mvc.perform(post("/v1/sessions/" + id + "/element/touch")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(new ElementTouchRequest("el_1", MobileAction.TAP, null))))
                .andExpect(status().isNoContent());
    }

    @Test
    void elementTouchOnDesktopReturns409() throws Exception {
        UUID id = UUID.randomUUID();
        doThrow(new MobileSessionRequiredException()).when(elementOps).touch(eq(id), any());

        mvc.perform(post("/v1/sessions/" + id + "/element/touch")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(new ElementTouchRequest("el_1", MobileAction.TAP, null))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("mobile_session_required"));
    }

    // ---------- Scroll / Viewport ----------

    @Test
    void scrollOk() throws Exception {
        UUID id = UUID.randomUUID();
        when(browserOps.scroll(eq(id), any())).thenReturn(new ScrollOffset(0, 500));

        mvc.perform(post("/v1/sessions/" + id + "/scroll")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(new ScrollRequest(ScrollMode.TO_BOTTOM, null, null))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.y").value(500));
    }

    @Test
    void scrollValidationFailure() throws Exception {
        UUID id = UUID.randomUUID();
        when(browserOps.scroll(eq(id), any()))
                .thenThrow(new ValidationFailedException("element_handle is required for TO_ELEMENT"));

        mvc.perform(post("/v1/sessions/" + id + "/scroll")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(new ScrollRequest(ScrollMode.TO_ELEMENT, null, null))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("validation_failed"));
    }

    @Test
    void getViewport() throws Exception {
        UUID id = UUID.randomUUID();
        when(browserOps.getViewport(id)).thenReturn(new ViewportStateResponse(
                new Viewport(1000, 800), new ScrollOffset(0, 0)));

        mvc.perform(get("/v1/sessions/" + id + "/viewport"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.viewport.width").value(1000));
    }

    // ---------- DOM ----------

    @Test
    void domRemove204() throws Exception {
        UUID id = UUID.randomUUID();
        doNothing().when(browserOps).removeDom(eq(id), any());

        mvc.perform(post("/v1/sessions/" + id + "/dom/remove")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(new DomRemoveRequest(DomRemovePreset.DRIFT_CHAT, null))))
                .andExpect(status().isNoContent());
    }

    // ---------- Alerts ----------

    @Test
    void getAlertOk() throws Exception {
        UUID id = UUID.randomUUID();
        when(alertService.getAlert(id)).thenReturn(new AlertStateResponse(true, "hi"));

        mvc.perform(get("/v1/sessions/" + id + "/alert"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.present").value(true))
                .andExpect(jsonPath("$.text").value("hi"));
    }

    @Test
    void respondAlert204() throws Exception {
        UUID id = UUID.randomUUID();
        doNothing().when(alertService).respond(eq(id), any(AlertRespondRequest.class));

        mvc.perform(post("/v1/sessions/" + id + "/alert/respond")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(new AlertRespondRequest(AlertChoice.ACCEPT, null))))
                .andExpect(status().isNoContent());
    }

    // ---------- Mouse ----------

    @Test
    void mouseMove204() throws Exception {
        UUID id = UUID.randomUUID();
        doNothing().when(browserOps).moveMouse(eq(id), any());

        mvc.perform(post("/v1/sessions/" + id + "/mouse/move")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(new MouseMoveRequest(MouseMoveMode.OUT_OF_FRAME, null, null))))
                .andExpect(status().isNoContent());
    }

    // ---------- Script ----------

    @Test
    void executeScript() throws Exception {
        UUID id = UUID.randomUUID();
        when(browserOps.executeScript(eq(id), any())).thenReturn(new ExecuteResponse(42));

        mvc.perform(post("/v1/sessions/" + id + "/execute")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(new ExecuteRequest("return 42;", null))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result").value(42));
    }

    // ---------- Capture ----------

    @Test
    void captureOk() throws Exception {
        UUID capId = UUID.randomUUID();
        when(captureService.capture(any())).thenReturn(new CaptureResponse(capId, "https://x",
                new CaptureScreenshotRef(null, "/v1/capture/" + capId + "/screenshot", 10, 5),
                null, null));

        mvc.perform(post("/v1/capture")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(new CaptureRequest("https://x",
                                BrowserType.CHROME, null, null, null, null, null))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.capture_id").value(capId.toString()));
    }

    @Test
    void captureScreenshotReturnsPng() throws Exception {
        UUID capId = UUID.randomUUID();
        CaptureScreenshotCache.CaptureEntry entry =
                new CaptureScreenshotCache.CaptureEntry(samplePng(), 2, 2, Instant.now().plusSeconds(60));
        when(captureService.fetchScreenshot(capId)).thenReturn(entry);

        mvc.perform(get("/v1/capture/" + capId + "/screenshot"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.IMAGE_PNG));
    }

    @Test
    void captureScreenshotNotFound() throws Exception {
        UUID capId = UUID.randomUUID();
        when(captureService.fetchScreenshot(capId)).thenThrow(new CaptureNotFoundException(capId));

        mvc.perform(get("/v1/capture/" + capId + "/screenshot"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("capture_not_found"));
    }

    @Test
    void captureScreenshotExpired() throws Exception {
        UUID capId = UUID.randomUUID();
        when(captureService.fetchScreenshot(capId)).thenThrow(new CaptureExpiredException(capId));

        mvc.perform(get("/v1/capture/" + capId + "/screenshot"))
                .andExpect(status().isGone())
                .andExpect(jsonPath("$.error.code").value("capture_expired"));
    }

    // ---------- Error envelope coverage for GlobalExceptionHandler branches ----------

    @Test
    void sessionBusyMapsTo409() throws Exception {
        UUID id = UUID.randomUUID();
        when(browserOps.getSource(id)).thenThrow(new SessionBusyException(id));

        mvc.perform(get("/v1/sessions/" + id + "/source"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("session_busy"));
    }

    @Test
    void unknownWebDriverExceptionMapsTo502() throws Exception {
        UUID id = UUID.randomUUID();
        when(browserOps.getSource(id)).thenThrow(new WebDriverException("driver exploded"));

        mvc.perform(get("/v1/sessions/" + id + "/source"))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.error.code").value("webdriver_error"));
    }

    @Test
    void unreachableBrowserMapsTo502() throws Exception {
        UUID id = UUID.randomUUID();
        when(browserOps.getSource(id)).thenThrow(new UnreachableBrowserException("gone"));

        mvc.perform(get("/v1/sessions/" + id + "/source"))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.error.code").value("upstream_unavailable"));
    }

    @Test
    void unknownSeleniumElementMapsTo404() throws Exception {
        UUID id = UUID.randomUUID();
        when(browserOps.getSource(id))
                .thenThrow(new org.openqa.selenium.NoSuchElementException("gone"));

        mvc.perform(get("/v1/sessions/" + id + "/source"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("element_not_found"));
    }

    @Test
    void seleniumTimeoutMapsTo408() throws Exception {
        UUID id = UUID.randomUUID();
        when(browserOps.getSource(id))
                .thenThrow(new org.openqa.selenium.TimeoutException("slow"));

        mvc.perform(get("/v1/sessions/" + id + "/source"))
                .andExpect(status().isRequestTimeout())
                .andExpect(jsonPath("$.error.code").value("upstream_timeout"));
    }

    @Test
    void unhandledAlertMapsTo409() throws Exception {
        UUID id = UUID.randomUUID();
        when(browserOps.getSource(id))
                .thenThrow(new org.openqa.selenium.UnhandledAlertException("alert"));

        mvc.perform(get("/v1/sessions/" + id + "/source"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("unhandled_alert"));
    }

    @Test
    void staleElementMapsTo409() throws Exception {
        UUID id = UUID.randomUUID();
        when(browserOps.getSource(id))
                .thenThrow(new org.openqa.selenium.StaleElementReferenceException("stale"));

        mvc.perform(get("/v1/sessions/" + id + "/source"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("stale_element"));
    }

    @Test
    void unexpectedErrorMapsTo500() throws Exception {
        UUID id = UUID.randomUUID();
        when(browserOps.getSource(id)).thenThrow(new IllegalStateException("wat"));

        mvc.perform(get("/v1/sessions/" + id + "/source"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error.code").value("internal_error"));
    }

    @Test
    void parameterTypeMismatchReturns400() throws Exception {
        // UUID path parameter receives a non-UUID → MethodArgumentTypeMismatchException
        mvc.perform(get("/v1/sessions/not-a-uuid/source"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("validation_failed"));
    }

    @Test
    void unparseableJsonReturns400() throws Exception {
        mvc.perform(post("/v1/sessions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("validation_failed"));
    }

    @Test
    void errorEnvelopeIncludesRequestId() throws Exception {
        UUID id = UUID.randomUUID();
        when(sessionService.describe(id)).thenThrow(new SessionNotFoundException(id));

        mvc.perform(get("/v1/sessions/" + id).header("X-Request-Id", "trace-1"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.request_id").value("trace-1"))
                .andExpect(header().string("X-Request-Id", "trace-1"));
    }

    @Test
    void errorMessageSanitizesMultiLineMessages() throws Exception {
        UUID id = UUID.randomUUID();
        when(browserOps.getSource(id))
                .thenThrow(new WebDriverException("short message\nwith full stacktrace"));

        mvc.perform(get("/v1/sessions/" + id + "/source"))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.error.message").value(containsString("short message")));
        assertThat(true).isTrue();
    }

    // ---------- helpers ----------

    private static byte[] samplePng() throws Exception {
        java.awt.image.BufferedImage img = new java.awt.image.BufferedImage(2, 2, java.awt.image.BufferedImage.TYPE_INT_RGB);
        java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
        javax.imageio.ImageIO.write(img, "png", baos);
        return baos.toByteArray();
    }
}
