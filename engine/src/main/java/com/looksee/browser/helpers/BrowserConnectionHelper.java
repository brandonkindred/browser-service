package com.looksee.browser.helpers;

import com.looksee.browser.Browser;
import com.looksee.browser.BrowserFactory;
import com.looksee.browser.MobileDevice;
import com.looksee.browser.MobileFactory;
import com.looksee.browser.config.BrowserStackProperties;
import com.looksee.browser.enums.BrowserEnvironment;
import com.looksee.browser.enums.BrowserType;
import java.net.MalformedURLException;
import java.net.URL;
import lombok.NoArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A helper class for creating a {@link Browser} connection.
 *
 * <p>Retries are intentionally <em>not</em> wired into this helper: this class is
 * framework-agnostic plain Java, and the retry annotation that used to live here depended on Spring
 * AOP. Callers that need retries should wrap the static methods in their own retry logic
 * (resilience4j, Failsafe, a hand-rolled loop, etc.).
 */
@NoArgsConstructor
public class BrowserConnectionHelper {
  /** The logger for the {@link BrowserConnectionHelper} class */
  @SuppressWarnings("unused")
  private static Logger log = LoggerFactory.getLogger(BrowserConnectionHelper.class);

  /** The index of the selenium hub */
  private static int SELENIUM_HUB_IDX = 0;

  private static String[] HUB_URLS;

  /** The index of the Appium server for round-robin selection */
  private static int APPIUM_SERVER_IDX = 0;

  private static String[] APPIUM_URLS;

  /** Whether BrowserStack is enabled as the connection provider */
  private static boolean browserStackEnabled = false;

  /** The BrowserStack hub URL */
  private static String browserStackHubUrl = null;

  /** The BrowserStack configuration properties */
  private static BrowserStackProperties browserStackProperties = null;

  /**
   * Sets the Selenium hub URLs used to round-robin chrome/firefox driver connections. Each entry
   * must be a fully qualified URL including scheme and path, e.g. {@code http://host:4444/wd/hub}.
   * No scheme or path rewriting is performed.
   *
   * @param urls the selenium hub URLs
   *     <p>precondition: urls != null
   */
  public static void setConfiguredSeleniumUrls(String[] urls) {
    assert urls != null;
    HUB_URLS = urls;
  }

  /**
   * Sets the Appium server URLs for mobile driver connections. Each entry must be a fully qualified
   * URL including scheme and path, e.g. {@code http://host:4723/wd/hub}. No scheme or path
   * rewriting is performed.
   *
   * @param urls the Appium server URLs
   *     <p>precondition: urls != null
   */
  public static void setConfiguredAppiumUrls(String[] urls) {
    assert urls != null;
    APPIUM_URLS = urls;
  }

  /**
   * Configures BrowserStack as the connection provider. When set, {@link #getConnection} will use
   * BrowserStack instead of the default Selenium URL-based round-robin.
   *
   * @param hubUrl the BrowserStack hub URL
   * @param properties the BrowserStack configuration properties
   *     <p>precondition: hubUrl != null precondition: properties != null
   */
  public static void setBrowserStackConfig(String hubUrl, BrowserStackProperties properties) {
    assert hubUrl != null;
    assert properties != null;

    browserStackHubUrl = hubUrl;
    browserStackProperties = properties;
    browserStackEnabled = true;
  }

  /**
   * Clears the BrowserStack configuration, reverting to the default Selenium URL-based connection.
   */
  public static void clearBrowserStackConfig() {
    browserStackEnabled = false;
    browserStackHubUrl = null;
    browserStackProperties = null;
  }

  /**
   * Creates a {@link Browser} connection.
   *
   * <p>For chrome/firefox the configured hub URLs (see {@link #setConfiguredSeleniumUrls}) are
   * round-robined and used verbatim — entries are expected to be fully qualified, e.g. {@code
   * http://host:4444/wd/hub}. The {@code environment} parameter is currently unused for URL
   * selection and is reserved for future per-environment routing.
   *
   * @param browser the browser to connect to
   * @param environment the environment to connect to
   * @return the browser connection
   *     <p>precondition: browser != null precondition: environment != null
   * @throws MalformedURLException if the url is malformed
   * @throws IllegalStateException if no Selenium hub URLs are configured for chrome/firefox
   */
  public static Browser getConnection(BrowserType browser, BrowserEnvironment environment)
      throws MalformedURLException {
    assert browser != null;
    assert environment != null;

    if (browserStackEnabled) {
      URL server_url = new URL(browserStackHubUrl);
      return BrowserFactory.createBrowserStackBrowser(
          browser.toString(), server_url, browserStackProperties);
    }

    URL server_url = null;

    if ("chrome".equalsIgnoreCase(browser.toString())
        || "firefox".equalsIgnoreCase(browser.toString())) {
      if (HUB_URLS == null || HUB_URLS.length == 0) {
        throw new IllegalStateException(
            "No Selenium hub URLs configured. Set browserservice.selenium.urls.");
      }
      server_url = new URL(HUB_URLS[Math.floorMod(SELENIUM_HUB_IDX++, HUB_URLS.length)]);
    }

    return BrowserFactory.createBrowser(browser.toString(), server_url);
  }

  /**
   * Creates a {@link MobileDevice} connection via Appium
   *
   * @param browser the mobile browser type (ANDROID, IOS)
   * @param environment the environment to connect to
   * @return the mobile device connection
   *     <p>precondition: browser != null precondition: browser.isMobile() precondition: environment
   *     != null
   * @throws MalformedURLException if the url is malformed
   * @throws IllegalStateException if Appium URLs are not configured
   */
  public static MobileDevice getMobileConnection(
      BrowserType browser, BrowserEnvironment environment) throws MalformedURLException {
    assert browser != null;
    assert browser.isMobile();
    assert environment != null;

    if (browserStackEnabled) {
      URL server_url = new URL(browserStackHubUrl);
      return MobileFactory.createBrowserStackMobileDevice(
          browser.toString(), server_url, browserStackProperties);
    }

    if (APPIUM_URLS == null || APPIUM_URLS.length == 0) {
      throw new IllegalStateException("No Appium URLs configured. Set browserservice.appium.urls.");
    }

    URL server_url = new URL(APPIUM_URLS[Math.floorMod(APPIUM_SERVER_IDX++, APPIUM_URLS.length)]);

    return MobileFactory.createMobileDevice(browser.toString(), server_url);
  }
}
