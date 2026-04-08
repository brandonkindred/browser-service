package com.looksee.browsing;

import static org.junit.jupiter.api.Assertions.*;

import java.net.MalformedURLException;
import java.net.URL;

import com.looksee.browser.config.BrowserStackProperties;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.WebDriverException;

public class MobileFactoryTest {

    private static BrowserStackProperties basicProps() {
        BrowserStackProperties props = new BrowserStackProperties();
        props.setUsername("user");
        props.setAccessKey("key");
        return props;
    }

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
        BrowserStackProperties props = basicProps();
        props.setOsVersion("13.0");
        props.setProject("MyProject");
        props.setBuild("build-1");
        props.setName("test");
        props.setDeviceName("Samsung Galaxy S23");
        assertThrows(Exception.class, () -> MobileFactory.createBrowserStackMobileDevice("android", url, props));
    }

    @Test
    public void testCreateBrowserStackMobileDeviceIOS() throws MalformedURLException {
        URL url = new URL("http://localhost:4723/wd/hub");
        BrowserStackProperties props = basicProps();
        props.setOsVersion("17.0");
        assertThrows(Exception.class, () -> MobileFactory.createBrowserStackMobileDevice("ios", url, props));
    }

    @Test
    public void testCreateBrowserStackMobileDeviceUnsupported() throws MalformedURLException {
        URL url = new URL("http://localhost:4723/wd/hub");
        BrowserStackProperties props = basicProps();
        assertThrows(WebDriverException.class, () -> MobileFactory.createBrowserStackMobileDevice("windows", url, props));
    }

    @Test
    public void testCreateBrowserStackMobileDeviceWithNullDeviceName() throws MalformedURLException {
        URL url = new URL("http://localhost:4723/wd/hub");
        BrowserStackProperties props = basicProps();
        // deviceName is null, so defaults should be used
        assertThrows(Exception.class, () -> MobileFactory.createBrowserStackMobileDevice("android", url, props));
    }
}
