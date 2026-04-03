package com.looksee.browsing.helpers;

import static org.junit.jupiter.api.Assertions.*;

import java.net.MalformedURLException;

import com.looksee.browsing.enums.BrowserEnvironment;
import com.looksee.browsing.enums.BrowserType;
import com.looksee.config.BrowserStackProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class BrowserConnectionHelperTest {

    @BeforeEach
    public void setUp() {
        // Reset BrowserStack state before each test to avoid cross-test interference
        BrowserConnectionHelper.clearBrowserStackConfig();
    }

    @Test
    public void testSetConfiguredSeleniumUrls() {
        String[] urls = {"hub1.example.com", "hub2.example.com"};
        assertDoesNotThrow(() -> BrowserConnectionHelper.setConfiguredSeleniumUrls(urls));
    }

    @Test
    public void testSetConfiguredAppiumUrls() {
        String[] urls = {"appium1.example.com:4723", "appium2.example.com:4723"};
        assertDoesNotThrow(() -> BrowserConnectionHelper.setConfiguredAppiumUrls(urls));
    }

    @Test
    public void testGetMobileConnectionWithoutUrls() {
        // Reset Appium URLs by setting empty
        BrowserConnectionHelper.setConfiguredAppiumUrls(new String[]{});

        assertThrows(IllegalStateException.class,
                () -> BrowserConnectionHelper.getMobileConnection(BrowserType.ANDROID, BrowserEnvironment.DISCOVERY));
    }

    @Test
    public void testGetMobileConnectionWithoutUrlsIOS() {
        BrowserConnectionHelper.setConfiguredAppiumUrls(new String[]{});

        assertThrows(IllegalStateException.class,
                () -> BrowserConnectionHelper.getMobileConnection(BrowserType.IOS, BrowserEnvironment.DISCOVERY));
    }

    @Test
    public void testSetBrowserStackConfig() {
        BrowserStackProperties props = new BrowserStackProperties(
                "testuser", "testaccesskey", null, null,
                null, null, null, null,
                null, null, null, null, null, null, null);

        assertDoesNotThrow(() -> BrowserConnectionHelper.setBrowserStackConfig(
                "https://hub-cloud.browserstack.com/wd/hub", props));
    }

    @Test
    public void testClearBrowserStackConfig() {
        BrowserStackProperties props = new BrowserStackProperties(
                "testuser", "testaccesskey", null, null,
                null, null, null, null,
                null, null, null, null, null, null, null);

        BrowserConnectionHelper.setBrowserStackConfig(
                "https://hub-cloud.browserstack.com/wd/hub", props);

        assertDoesNotThrow(() -> BrowserConnectionHelper.clearBrowserStackConfig());
    }

    @Test
    public void testGetMobileConnectionWithoutUrlsWhenBrowserStackCleared() {
        // Ensure that after clearing BrowserStack, mobile connections still require Appium URLs
        BrowserStackProperties props = new BrowserStackProperties(
                "testuser", "testaccesskey", null, null,
                null, null, null, null,
                null, "Samsung Galaxy S23", null, null, null, null, null);

        BrowserConnectionHelper.setBrowserStackConfig(
                "https://hub-cloud.browserstack.com/wd/hub", props);
        BrowserConnectionHelper.clearBrowserStackConfig();
        BrowserConnectionHelper.setConfiguredAppiumUrls(new String[]{});

        assertThrows(IllegalStateException.class,
                () -> BrowserConnectionHelper.getMobileConnection(BrowserType.ANDROID, BrowserEnvironment.DISCOVERY));
    }

    @Test
    public void testGetMobileConnectionWithNullAppiumUrls() {
        // When Appium URLs are null, should throw IllegalStateException
        BrowserConnectionHelper.setConfiguredAppiumUrls(new String[]{});
        assertThrows(IllegalStateException.class,
                () -> BrowserConnectionHelper.getMobileConnection(BrowserType.IOS, BrowserEnvironment.TEST));
    }

    @Test
    public void testGetConnectionWithDiscoveryEnvironmentChrome() {
        BrowserConnectionHelper.setConfiguredSeleniumUrls(new String[]{"localhost:4444"});
        // Will fail when trying to connect to hub, but exercises the round-robin path
        assertThrows(Exception.class,
                () -> BrowserConnectionHelper.getConnection(BrowserType.CHROME, BrowserEnvironment.DISCOVERY));
    }

    @Test
    public void testGetConnectionWithDiscoveryEnvironmentFirefox() {
        BrowserConnectionHelper.setConfiguredSeleniumUrls(new String[]{"localhost:4444"});
        assertThrows(Exception.class,
                () -> BrowserConnectionHelper.getConnection(BrowserType.FIREFOX, BrowserEnvironment.DISCOVERY));
    }

    @Test
    public void testGetConnectionWithTestEnvironment() {
        BrowserConnectionHelper.setConfiguredSeleniumUrls(new String[]{"localhost:4444"});
        // TEST environment with chrome won't match DISCOVERY branch, server_url remains null
        assertThrows(Exception.class,
                () -> BrowserConnectionHelper.getConnection(BrowserType.CHROME, BrowserEnvironment.TEST));
    }

    @Test
    public void testGetConnectionWithBrowserStack() {
        BrowserStackProperties props = new BrowserStackProperties(
                "testuser", "testaccesskey", "Windows", "11",
                "Chrome", "latest", "Project", "Build",
                "Name", null, null, null, null, null, null);
        BrowserConnectionHelper.setBrowserStackConfig(
                "https://hub-cloud.browserstack.com/wd/hub", props);
        // Will fail to connect to BrowserStack, but exercises the BrowserStack code path
        assertThrows(Exception.class,
                () -> BrowserConnectionHelper.getConnection(BrowserType.CHROME, BrowserEnvironment.DISCOVERY));
    }

    @Test
    public void testGetMobileConnectionWithBrowserStack() {
        BrowserStackProperties props = new BrowserStackProperties(
                "testuser", "testaccesskey", null, "13.0",
                null, null, null, null,
                null, "Samsung Galaxy S23", true, false, true, null, null);
        BrowserConnectionHelper.setBrowserStackConfig(
                "https://hub-cloud.browserstack.com/wd/hub", props);
        assertThrows(Exception.class,
                () -> BrowserConnectionHelper.getMobileConnection(BrowserType.ANDROID, BrowserEnvironment.DISCOVERY));
    }

    @Test
    public void testSetConfiguredSeleniumUrlsMultiple() {
        String[] urls = {"hub1.example.com", "hub2.example.com", "hub3.example.com"};
        assertDoesNotThrow(() -> BrowserConnectionHelper.setConfiguredSeleniumUrls(urls));
    }

    @Test
    public void testNoArgsConstructor() {
        BrowserConnectionHelper helper = new BrowserConnectionHelper();
        assertNotNull(helper);
    }
}
