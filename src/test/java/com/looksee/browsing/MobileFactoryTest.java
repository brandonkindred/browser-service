package com.looksee.browsing;

import static org.junit.jupiter.api.Assertions.*;

import java.net.MalformedURLException;
import java.net.URL;

import com.looksee.config.BrowserStackProperties;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.WebDriverException;

public class MobileFactoryTest {

    @Test
    public void testCreateDriverUnsupportedPlatform() throws MalformedURLException {
        URL url = new URL("http://localhost:4723/wd/hub");
        assertThrows(WebDriverException.class, () -> MobileFactory.createDriver("windows", url));
    }

    @Test
    public void testCreateDriverUnsupportedPlatformMessage() throws MalformedURLException {
        URL url = new URL("http://localhost:4723/wd/hub");
        WebDriverException ex = assertThrows(WebDriverException.class,
                () -> MobileFactory.createDriver("unknown", url));
        assertTrue(ex.getMessage().contains("Unsupported mobile platform type"));
    }

    @Test
    public void testCreateMobileDeviceUnsupportedPlatform() throws MalformedURLException {
        URL url = new URL("http://localhost:4723/wd/hub");
        assertThrows(WebDriverException.class, () -> MobileFactory.createMobileDevice("windows", url));
    }

    @Test
    public void testCreateDriverAndroid() throws MalformedURLException {
        // Will fail to connect to Appium server, but exercises the code path
        URL url = new URL("http://localhost:4723/wd/hub");
        assertThrows(Exception.class, () -> MobileFactory.createDriver("android", url));
    }

    @Test
    public void testCreateDriverIOS() throws MalformedURLException {
        URL url = new URL("http://localhost:4723/wd/hub");
        assertThrows(Exception.class, () -> MobileFactory.createDriver("ios", url));
    }

    @Test
    public void testCreateDriverCaseInsensitive() throws MalformedURLException {
        URL url = new URL("http://localhost:4723/wd/hub");
        // "ANDROID" should match the lowercase switch
        assertThrows(Exception.class, () -> MobileFactory.createDriver("ANDROID", url));
    }

    @Test
    public void testCreateBrowserStackMobileDeviceAndroid() throws MalformedURLException {
        URL url = new URL("http://localhost:4723/wd/hub");
        BrowserStackProperties props = new BrowserStackProperties(
                "user", "key", null, "13.0",
                null, null, "MyProject", "build-1",
                "test", "Samsung Galaxy S23", true,
                false, true, 30000, 3);
        assertThrows(Exception.class, () -> MobileFactory.createBrowserStackMobileDevice("android", url, props));
    }

    @Test
    public void testCreateBrowserStackMobileDeviceIOS() throws MalformedURLException {
        URL url = new URL("http://localhost:4723/wd/hub");
        BrowserStackProperties props = new BrowserStackProperties(
                "user", "key", null, "17.0",
                null, null, null, null,
                null, null, true,
                false, true, 30000, 3);
        assertThrows(Exception.class, () -> MobileFactory.createBrowserStackMobileDevice("ios", url, props));
    }

    @Test
    public void testCreateBrowserStackMobileDeviceUnsupported() throws MalformedURLException {
        URL url = new URL("http://localhost:4723/wd/hub");
        BrowserStackProperties props = new BrowserStackProperties(
                "user", "key", null, null,
                null, null, null, null,
                null, null, null, null, null, null, null);
        assertThrows(WebDriverException.class, () -> MobileFactory.createBrowserStackMobileDevice("windows", url, props));
    }

    @Test
    public void testCreateBrowserStackMobileDeviceWithNullDeviceName() throws MalformedURLException {
        URL url = new URL("http://localhost:4723/wd/hub");
        BrowserStackProperties props = new BrowserStackProperties(
                "user", "key", null, null,
                null, null, null, null,
                null, null, null, null, null, null, null);
        // deviceName is null, so defaults should be used
        assertThrows(Exception.class, () -> MobileFactory.createBrowserStackMobileDevice("android", url, props));
    }
}
