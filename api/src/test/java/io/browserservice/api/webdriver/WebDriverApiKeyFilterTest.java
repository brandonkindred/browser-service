package io.browserservice.api.webdriver;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.browserservice.api.session.CallerId;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.UriInfo;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class WebDriverApiKeyFilterTest {

  private static final String KEYS_CONFIG = "sk_test_123:acme:alice,sk_test_456:corp:bob";

  @Test
  void basicAuthWithValidKeyUsesUsernameAsSubject() {
    WebDriverApiKeyFilter filter = new WebDriverApiKeyFilter(KEYS_CONFIG);
    ContainerRequestContext ctx =
        mockRequest("/wd/hub/session", basicAuth("myuser", "sk_test_123"));

    filter.filter(ctx);

    ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
    verify(ctx).setProperty(eq(WebDriverCallerHolder.REQUEST_ATTRIBUTE), captor.capture());
    CallerId caller = (CallerId) captor.getValue();
    assertThat(caller).isEqualTo(CallerId.of("acme", "myuser"));
  }

  @Test
  void basicAuthWithBlankUsernameFallsBackToKeySubject() {
    WebDriverApiKeyFilter filter = new WebDriverApiKeyFilter(KEYS_CONFIG);
    ContainerRequestContext ctx = mockRequest("/wd/hub/session", basicAuth("", "sk_test_123"));

    filter.filter(ctx);

    ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
    verify(ctx).setProperty(eq(WebDriverCallerHolder.REQUEST_ATTRIBUTE), captor.capture());
    CallerId caller = (CallerId) captor.getValue();
    assertThat(caller).isEqualTo(CallerId.of("acme", "alice"));
  }

  @Test
  void basicAuthWithInvalidKeyReturns401() {
    WebDriverApiKeyFilter filter = new WebDriverApiKeyFilter(KEYS_CONFIG);
    ContainerRequestContext ctx = mockRequest("/wd/hub/session", basicAuth("user", "wrong-key"));

    filter.filter(ctx);

    verify(ctx).abortWith(org.mockito.ArgumentMatchers.any());
  }

  @Test
  void apiKeyHeaderWithValidKeySetsCaller() {
    WebDriverApiKeyFilter filter = new WebDriverApiKeyFilter(KEYS_CONFIG);
    ContainerRequestContext ctx = mockRequest("/wd/hub/session", null);
    when(ctx.getHeaderString("X-API-Key")).thenReturn("sk_test_456");

    filter.filter(ctx);

    ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
    verify(ctx).setProperty(eq(WebDriverCallerHolder.REQUEST_ATTRIBUTE), captor.capture());
    CallerId caller = (CallerId) captor.getValue();
    assertThat(caller).isEqualTo(CallerId.of("corp", "bob"));
  }

  @Test
  void noAuthReturns401() {
    WebDriverApiKeyFilter filter = new WebDriverApiKeyFilter(KEYS_CONFIG);
    ContainerRequestContext ctx = mockRequest("/wd/hub/session", null);

    filter.filter(ctx);

    verify(ctx).abortWith(org.mockito.ArgumentMatchers.any());
  }

  @Test
  void nonWdPathIsIgnored() {
    WebDriverApiKeyFilter filter = new WebDriverApiKeyFilter(KEYS_CONFIG);
    ContainerRequestContext ctx = mockRequest("/v1/sessions", null);

    filter.filter(ctx);

    verify(ctx, never()).abortWith(org.mockito.ArgumentMatchers.any());
    verify(ctx, never())
        .setProperty(
            eq(WebDriverCallerHolder.REQUEST_ATTRIBUTE), org.mockito.ArgumentMatchers.any());
  }

  @Test
  void emptyConfigYieldsNoKeys() {
    WebDriverApiKeyFilter filter = new WebDriverApiKeyFilter("");
    ContainerRequestContext ctx = mockRequest("/wd/hub/session", basicAuth("u", "anykey"));

    filter.filter(ctx);

    verify(ctx).abortWith(org.mockito.ArgumentMatchers.any());
  }

  @Test
  void differentUsernamesSameKeyGetDistinctCallers() {
    WebDriverApiKeyFilter filter = new WebDriverApiKeyFilter(KEYS_CONFIG);

    ContainerRequestContext ctx1 =
        mockRequest("/wd/hub/session", basicAuth("user-a", "sk_test_123"));
    filter.filter(ctx1);
    ArgumentCaptor<Object> cap1 = ArgumentCaptor.forClass(Object.class);
    verify(ctx1).setProperty(eq(WebDriverCallerHolder.REQUEST_ATTRIBUTE), cap1.capture());

    ContainerRequestContext ctx2 =
        mockRequest("/wd/hub/session", basicAuth("user-b", "sk_test_123"));
    filter.filter(ctx2);
    ArgumentCaptor<Object> cap2 = ArgumentCaptor.forClass(Object.class);
    verify(ctx2).setProperty(eq(WebDriverCallerHolder.REQUEST_ATTRIBUTE), cap2.capture());

    assertThat(cap1.getValue()).isNotEqualTo(cap2.getValue());
    assertThat(((CallerId) cap1.getValue()).subject()).isEqualTo("user-a");
    assertThat(((CallerId) cap2.getValue()).subject()).isEqualTo("user-b");
  }

  private static ContainerRequestContext mockRequest(String path, String authHeader) {
    ContainerRequestContext ctx = mock(ContainerRequestContext.class);
    UriInfo uriInfo = mock(UriInfo.class);
    when(uriInfo.getPath()).thenReturn(path);
    when(ctx.getUriInfo()).thenReturn(uriInfo);
    when(ctx.getHeaderString("Authorization")).thenReturn(authHeader);
    when(ctx.getHeaderString("X-API-Key")).thenReturn(null);
    return ctx;
  }

  private static String basicAuth(String username, String password) {
    String credentials = username + ":" + password;
    return "Basic "
        + Base64.getEncoder().encodeToString(credentials.getBytes(StandardCharsets.UTF_8));
  }
}
