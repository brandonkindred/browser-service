package com.looksee.browser.helpers;

import static org.junit.jupiter.api.Assertions.*;

import com.looksee.browser.config.BrowserStackProperties;
import com.looksee.browser.enums.BrowserEnvironment;
import com.looksee.browser.enums.BrowserType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class BrowserConnectionHelperTest {

  private static BrowserStackProperties basicProps() {
    BrowserStackProperties props = new BrowserStackProperties();
    props.setUsername("testuser");
    props.setAccessKey("testaccesskey");
    return props;
  }

  @BeforeEach
  public void setUp() {
    // Reset BrowserStack state before each test to avoid cross-test interference
    BrowserConnectionHelper.clearBrowserStackConfig();
  }

  @Test
  public void testSetConfiguredSeleniumUrls() {
    String[] urls = {"http://hub1.example.com:4444/wd/hub", "http://hub2.example.com:4444/wd/hub"};
    assertDoesNotThrow(() -> BrowserConnectionHelper.setConfiguredSeleniumUrls(urls));
  }

  @Test
  public void testSetConfiguredAppiumUrls() {
    String[] urls = {
      "http://appium1.example.com:4723/wd/hub", "http://appium2.example.com:4723/wd/hub"
    };
    assertDoesNotThrow(() -> BrowserConnectionHelper.setConfiguredAppiumUrls(urls));
  }

  @Test
  public void testGetMobileConnectionWithoutUrls() {
    // Reset Appium URLs by setting empty
    BrowserConnectionHelper.setConfiguredAppiumUrls(new String[] {});

    assertThrows(
        IllegalStateException.class,
        () ->
            BrowserConnectionHelper.getMobileConnection(
                BrowserType.ANDROID, BrowserEnvironment.DISCOVERY));
  }

  @Test
  public void testGetMobileConnectionWithoutUrlsIOS() {
    BrowserConnectionHelper.setConfiguredAppiumUrls(new String[] {});

    assertThrows(
        IllegalStateException.class,
        () ->
            BrowserConnectionHelper.getMobileConnection(
                BrowserType.IOS, BrowserEnvironment.DISCOVERY));
  }

  @Test
  public void testSetBrowserStackConfig() {
    BrowserStackProperties props = basicProps();

    assertDoesNotThrow(
        () ->
            BrowserConnectionHelper.setBrowserStackConfig(
                "https://hub-cloud.browserstack.com/wd/hub", props));
  }

  @Test
  public void testClearBrowserStackConfig() {
    BrowserStackProperties props = basicProps();

    BrowserConnectionHelper.setBrowserStackConfig(
        "https://hub-cloud.browserstack.com/wd/hub", props);

    assertDoesNotThrow(() -> BrowserConnectionHelper.clearBrowserStackConfig());
  }

  @Test
  public void testGetMobileConnectionWithoutUrlsWhenBrowserStackCleared() {
    // Ensure that after clearing BrowserStack, mobile connections still require Appium URLs
    BrowserStackProperties props = basicProps();
    props.setDeviceName("Samsung Galaxy S23");

    BrowserConnectionHelper.setBrowserStackConfig(
        "https://hub-cloud.browserstack.com/wd/hub", props);
    BrowserConnectionHelper.clearBrowserStackConfig();
    BrowserConnectionHelper.setConfiguredAppiumUrls(new String[] {});

    assertThrows(
        IllegalStateException.class,
        () ->
            BrowserConnectionHelper.getMobileConnection(
                BrowserType.ANDROID, BrowserEnvironment.DISCOVERY));
  }

  @Test
  public void testGetMobileConnectionWithNullAppiumUrls() {
    // When Appium URLs are null, should throw IllegalStateException
    BrowserConnectionHelper.setConfiguredAppiumUrls(new String[] {});
    assertThrows(
        IllegalStateException.class,
        () ->
            BrowserConnectionHelper.getMobileConnection(BrowserType.IOS, BrowserEnvironment.TEST));
  }

  @Test
  public void testGetConnectionWithDiscoveryEnvironmentChrome() {
    BrowserConnectionHelper.setConfiguredSeleniumUrls(
        new String[] {"http://localhost:4444/wd/hub"});
    // Will fail when trying to connect to hub, but exercises the round-robin path
    assertThrows(
        Exception.class,
        () ->
            BrowserConnectionHelper.getConnection(
                BrowserType.CHROME, BrowserEnvironment.DISCOVERY));
  }

  @Test
  public void testGetConnectionWithDiscoveryEnvironmentFirefox() {
    BrowserConnectionHelper.setConfiguredSeleniumUrls(
        new String[] {"http://localhost:4444/wd/hub"});
    assertThrows(
        Exception.class,
        () ->
            BrowserConnectionHelper.getConnection(
                BrowserType.FIREFOX, BrowserEnvironment.DISCOVERY));
  }

  @Test
  public void testGetConnectionWithTestEnvironmentChromeUsesConfiguredUrl() {
    // Regression for #43: TEST environment must honor configured hub URLs (previously the
    // helper short-circuited to a null server_url for non-DISCOVERY env, causing an NPE in
    // RemoteWebDriver.<init>). The connect attempt itself will still fail since nothing is
    // listening — but the exception must come from the network layer, not a NullPointerException.
    BrowserConnectionHelper.setConfiguredSeleniumUrls(
        new String[] {"http://localhost:4444/wd/hub"});
    Exception ex =
        assertThrows(
            Exception.class,
            () ->
                BrowserConnectionHelper.getConnection(BrowserType.CHROME, BrowserEnvironment.TEST));
    assertFalse(
        ex instanceof NullPointerException,
        "TEST environment should not fall through to a null server URL: " + ex);
  }

  @Test
  public void testGetConnectionThrowsWhenNoSeleniumUrlsConfigured() {
    BrowserConnectionHelper.setConfiguredSeleniumUrls(new String[] {});
    assertThrows(
        IllegalStateException.class,
        () -> BrowserConnectionHelper.getConnection(BrowserType.CHROME, BrowserEnvironment.TEST));
  }

  @Test
  public void testGetConnectionWithBrowserStack() {
    BrowserStackProperties props = basicProps();
    props.setOs("Windows");
    props.setOsVersion("11");
    props.setBrowser("Chrome");
    props.setBrowserVersion("latest");
    props.setProject("Project");
    props.setBuild("Build");
    props.setName("Name");
    BrowserConnectionHelper.setBrowserStackConfig(
        "https://hub-cloud.browserstack.com/wd/hub", props);
    // Will fail to connect to BrowserStack, but exercises the BrowserStack code path
    assertThrows(
        Exception.class,
        () ->
            BrowserConnectionHelper.getConnection(
                BrowserType.CHROME, BrowserEnvironment.DISCOVERY));
  }

  @Test
  public void testGetMobileConnectionWithBrowserStack() {
    BrowserStackProperties props = basicProps();
    props.setOsVersion("13.0");
    props.setDeviceName("Samsung Galaxy S23");
    props.setRealMobile(true);
    props.setLocal(false);
    props.setDebug(true);
    BrowserConnectionHelper.setBrowserStackConfig(
        "https://hub-cloud.browserstack.com/wd/hub", props);
    assertThrows(
        Exception.class,
        () ->
            BrowserConnectionHelper.getMobileConnection(
                BrowserType.ANDROID, BrowserEnvironment.DISCOVERY));
  }

  @Test
  public void testSetConfiguredSeleniumUrlsMultiple() {
    String[] urls = {
      "http://hub1.example.com:4444/wd/hub",
      "http://hub2.example.com:4444/wd/hub",
      "http://hub3.example.com:4444/wd/hub"
    };
    assertDoesNotThrow(() -> BrowserConnectionHelper.setConfiguredSeleniumUrls(urls));
  }

  @Test
  public void testNoArgsConstructor() {
    BrowserConnectionHelper helper = new BrowserConnectionHelper();
    assertNotNull(helper);
  }
}
