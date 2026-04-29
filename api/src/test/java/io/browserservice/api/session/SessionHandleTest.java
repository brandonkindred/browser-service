package io.browserservice.api.session;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.looksee.browser.Browser;
import com.looksee.browser.MobileDevice;
import com.looksee.browser.enums.BrowserEnvironment;
import com.looksee.browser.enums.BrowserType;
import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.WebDriver;

class SessionHandleTest {

  @Test
  void desktopFactoryBuildsADesktopSession() {
    Browser browser = mock(Browser.class);
    WebDriver driver = mock(WebDriver.class);
    org.mockito.Mockito.when(browser.getDriver()).thenReturn(driver);

    SessionHandle handle =
        SessionHandle.desktop(
            browser,
            CallerId.parse("alice"),
            BrowserType.CHROME,
            BrowserEnvironment.TEST,
            Duration.ofSeconds(30),
            Duration.ofSeconds(60));

    assertThat(handle.isMobile()).isFalse();
    assertThat(handle.asBrowser()).isSameAs(browser);
    assertThat(handle.driver()).isSameAs(driver);
    assertThat(handle.browserType()).isEqualTo(BrowserType.CHROME);
    assertThat(handle.environment()).isEqualTo(BrowserEnvironment.TEST);
    assertThat(handle.idleTtl()).isEqualTo(Duration.ofSeconds(30));
    assertThat(handle.absoluteTtl()).isEqualTo(Duration.ofSeconds(60));
    assertThat(handle.createdAt()).isNotNull();
    assertThat(handle.lastUsedAt()).isEqualTo(handle.createdAt());
    assertThat(handle.isClosed()).isFalse();
    assertThat(handle.id()).isNotNull();
    assertThat(handle.lock()).isNotNull();
    assertThat(handle.elements()).isNotNull();
  }

  @Test
  void mobileFactoryBuildsAMobileSession() {
    MobileDevice device = mock(MobileDevice.class);
    WebDriver driver = mock(WebDriver.class);
    org.mockito.Mockito.when(device.getDriver()).thenReturn(driver);

    SessionHandle handle =
        SessionHandle.mobile(
            device,
            CallerId.parse("alice"),
            BrowserType.ANDROID,
            BrowserEnvironment.DISCOVERY,
            Duration.ofSeconds(15),
            Duration.ofSeconds(45));

    assertThat(handle.isMobile()).isTrue();
    assertThat(handle.asMobileDevice()).isSameAs(device);
    assertThat(handle.driver()).isSameAs(driver);
    assertThat(handle.owner().value()).isEqualTo("alice");
    assertThatThrownBy(handle::asBrowser).isInstanceOf(IllegalStateException.class);
  }

  @Test
  void asMobileDeviceOnDesktopThrows() {
    Browser browser = mock(Browser.class);
    SessionHandle handle =
        SessionHandle.desktop(
            browser,
            CallerId.parse("alice"),
            BrowserType.CHROME,
            BrowserEnvironment.TEST,
            Duration.ofSeconds(30),
            Duration.ofSeconds(60));
    assertThatThrownBy(handle::asMobileDevice).isInstanceOf(IllegalStateException.class);
  }

  @Test
  void touchAdvancesLastUsedAt() throws InterruptedException {
    SessionHandle handle =
        SessionHandle.desktop(
            mock(Browser.class),
            CallerId.parse("alice"),
            BrowserType.CHROME,
            BrowserEnvironment.TEST,
            Duration.ofSeconds(30),
            Duration.ofSeconds(60));
    Instant before = handle.lastUsedAt();
    Thread.sleep(5);
    handle.touch();
    assertThat(handle.lastUsedAt()).isAfter(before);
  }

  @Test
  void readAccessorsDoNotAdvanceLastUsedAt() throws InterruptedException {
    SessionHandle handle =
        SessionHandle.desktop(
            mock(Browser.class),
            CallerId.parse("alice"),
            BrowserType.CHROME,
            BrowserEnvironment.TEST,
            Duration.ofSeconds(30),
            Duration.ofSeconds(60));
    final Instant before = handle.lastUsedAt();
    Thread.sleep(5);

    // Read paths a describe/list response would touch — none should refresh idle.
    handle.id();
    handle.browserType();
    handle.environment();
    handle.createdAt();
    handle.lastUsedAt();
    handle.expiresAt();
    handle.isExpired(Instant.now());
    handle.isClosed();

    assertThat(handle.lastUsedAt()).isEqualTo(before);
  }

  @Test
  void expiresAtPrefersTheEarlierOfIdleOrAbsoluteTtl() {
    SessionHandle handle =
        SessionHandle.desktop(
            mock(Browser.class),
            CallerId.parse("alice"),
            BrowserType.CHROME,
            BrowserEnvironment.TEST,
            Duration.ofSeconds(10),
            Duration.ofSeconds(60));
    Instant idleExpiry = handle.lastUsedAt().plus(Duration.ofSeconds(10));
    Instant absoluteExpiry = handle.createdAt().plus(Duration.ofSeconds(60));
    Instant expected = idleExpiry.isBefore(absoluteExpiry) ? idleExpiry : absoluteExpiry;
    assertThat(handle.expiresAt()).isEqualTo(expected);
  }

  @Test
  void isExpiredReflectsBothTtls() {
    SessionHandle handle =
        SessionHandle.desktop(
            mock(Browser.class),
            CallerId.parse("alice"),
            BrowserType.CHROME,
            BrowserEnvironment.TEST,
            Duration.ofMillis(1),
            Duration.ofSeconds(10));
    assertThat(handle.isExpired(Instant.now().plusSeconds(5))).isTrue();
  }

  @Test
  void expiryReasonReturnsIdleWhenIdleDeadlineCrossesFirst() {
    SessionHandle handle =
        SessionHandle.desktop(
            mock(Browser.class),
            CallerId.parse("alice"),
            BrowserType.CHROME,
            BrowserEnvironment.TEST,
            Duration.ofSeconds(10),
            Duration.ofSeconds(60));
    Instant pastIdle = handle.lastUsedAt().plus(Duration.ofSeconds(15));
    assertThat(handle.expiryReason(pastIdle)).isEqualTo(SessionHandle.ExpiryReason.IDLE);
  }

  @Test
  void expiryReasonReturnsAbsoluteWhenAbsoluteDeadlineCrossesFirst() {
    SessionHandle handle =
        SessionHandle.desktop(
            mock(Browser.class),
            CallerId.parse("alice"),
            BrowserType.CHROME,
            BrowserEnvironment.TEST,
            Duration.ofSeconds(60),
            Duration.ofSeconds(10));
    Instant pastAbsolute = handle.createdAt().plus(Duration.ofSeconds(15));
    assertThat(handle.expiryReason(pastAbsolute)).isEqualTo(SessionHandle.ExpiryReason.ABSOLUTE);
  }

  @Test
  void expiryReasonAtAbsoluteBoundaryReturnsAbsolute() {
    // now == absoluteDeadline, idle has not been crossed → ABSOLUTE.
    SessionHandle handle =
        SessionHandle.desktop(
            mock(Browser.class),
            CallerId.parse("alice"),
            BrowserType.CHROME,
            BrowserEnvironment.TEST,
            Duration.ofSeconds(60),
            Duration.ofSeconds(10));
    Instant boundary = handle.createdAt().plus(Duration.ofSeconds(10));
    assertThat(handle.expiryReason(boundary)).isEqualTo(SessionHandle.ExpiryReason.ABSOLUTE);
  }

  @Test
  void expiryReasonBeforeAnyDeadlineFallsBackToEarlierDeadline() {
    // Neither deadline crossed yet — helper still returns whichever will trip first
    // so callers can reason about the next reap. Idle (10s) trips before absolute (60s).
    SessionHandle handle =
        SessionHandle.desktop(
            mock(Browser.class),
            CallerId.parse("alice"),
            BrowserType.CHROME,
            BrowserEnvironment.TEST,
            Duration.ofSeconds(10),
            Duration.ofSeconds(60));
    assertThat(handle.expiryReason(handle.createdAt())).isEqualTo(SessionHandle.ExpiryReason.IDLE);
  }

  @Test
  void expiryReasonWhenBothCrossedPicksTheEarlierDeadline() {
    // Both crossed; absolute deadline is earlier than idle deadline → ABSOLUTE.
    SessionHandle handle =
        SessionHandle.desktop(
            mock(Browser.class),
            CallerId.parse("alice"),
            BrowserType.CHROME,
            BrowserEnvironment.TEST,
            Duration.ofSeconds(30),
            Duration.ofSeconds(5));
    Instant farFuture = handle.createdAt().plus(Duration.ofMinutes(10));
    assertThat(handle.expiryReason(farFuture)).isEqualTo(SessionHandle.ExpiryReason.ABSOLUTE);
  }

  @Test
  void expiryReasonWireValueIsLowercase() {
    assertThat(SessionHandle.ExpiryReason.IDLE.wireValue()).isEqualTo("idle");
    assertThat(SessionHandle.ExpiryReason.ABSOLUTE.wireValue()).isEqualTo("absolute");
  }

  @Test
  void closeOnceReturnsTrueFirstTimeAndFalseAfter() {
    Browser browser = mock(Browser.class);
    SessionHandle handle =
        SessionHandle.desktop(
            browser,
            CallerId.parse("alice"),
            BrowserType.CHROME,
            BrowserEnvironment.TEST,
            Duration.ofSeconds(30),
            Duration.ofSeconds(60));

    assertThat(handle.closeOnce()).isTrue();
    assertThat(handle.closeOnce()).isFalse();
    assertThat(handle.isClosed()).isTrue();
    verify(browser).close();
  }

  @Test
  void closeOnceSwallowsBrowserCloseException() {
    Browser browser = mock(Browser.class);
    doThrow(new RuntimeException("boom")).when(browser).close();

    SessionHandle handle =
        SessionHandle.desktop(
            browser,
            CallerId.parse("alice"),
            BrowserType.CHROME,
            BrowserEnvironment.TEST,
            Duration.ofSeconds(30),
            Duration.ofSeconds(60));

    assertThat(handle.closeOnce()).isTrue();
  }

  @Test
  void closeOnceForMobileCallsMobileClose() {
    MobileDevice device = mock(MobileDevice.class);
    SessionHandle handle =
        SessionHandle.mobile(
            device,
            CallerId.parse("alice"),
            BrowserType.ANDROID,
            BrowserEnvironment.TEST,
            Duration.ofSeconds(30),
            Duration.ofSeconds(60));
    assertThat(handle.closeOnce()).isTrue();
    verify(device).close();
  }

  @Test
  void closeOnceForMobileSwallowsException() {
    MobileDevice device = mock(MobileDevice.class);
    doThrow(new RuntimeException("boom")).when(device).close();
    SessionHandle handle =
        SessionHandle.mobile(
            device,
            CallerId.parse("alice"),
            BrowserType.IOS,
            BrowserEnvironment.TEST,
            Duration.ofSeconds(30),
            Duration.ofSeconds(60));
    assertThat(handle.closeOnce()).isTrue();
  }

  @Test
  void desktopFactoryRejectsNullOwner() {
    Browser browser = mock(Browser.class);
    assertThatThrownBy(
            () ->
                SessionHandle.desktop(
                    browser,
                    null,
                    BrowserType.CHROME,
                    BrowserEnvironment.TEST,
                    Duration.ofSeconds(30),
                    Duration.ofSeconds(60)))
        .isInstanceOf(NullPointerException.class);
  }

  @Test
  void mobileFactoryRejectsNullOwner() {
    MobileDevice device = mock(MobileDevice.class);
    assertThatThrownBy(
            () ->
                SessionHandle.mobile(
                    device,
                    null,
                    BrowserType.ANDROID,
                    BrowserEnvironment.TEST,
                    Duration.ofSeconds(30),
                    Duration.ofSeconds(60)))
        .isInstanceOf(NullPointerException.class);
  }
}
