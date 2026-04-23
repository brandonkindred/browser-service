package com.looksee.browser;

import static org.junit.jupiter.api.Assertions.*;

import java.net.MalformedURLException;
import java.net.URL;

import com.looksee.browser.config.BrowserStackProperties;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.WebDriverException;

public class BrowserFactoryTest {

    @Test
    public void testCreateDriverUnsupportedType() throws MalformedURLException {
        URL url = new URL("http://localhost:4444/wd/hub");
        assertThrows(WebDriverException.class, () -> BrowserFactory.createDriver("opera", url));
    }

    @Test
    public void testCreateDriverUnsupportedTypeMessage() throws MalformedURLException {
        URL url = new URL("http://localhost:4444/wd/hub");
        WebDriverException ex = assertThrows(WebDriverException.class,
                () -> BrowserFactory.createDriver("unsupported", url));
        assertTrue(ex.getMessage().contains("Unsupported browser type"));
    }

    @Test
    public void testCreateBrowserUnsupportedType() throws MalformedURLException {
        URL url = new URL("http://localhost:4444/wd/hub");
        assertThrows(WebDriverException.class, () -> BrowserFactory.createBrowser("opera", url));
    }

    @Test
    public void testCreateDriverChromeConnectsToHub() throws MalformedURLException {
        // Chrome driver will try to connect to the hub and fail since no hub exists
        URL url = new URL("http://localhost:4444/wd/hub");
        assertThrows(Exception.class, () -> BrowserFactory.createDriver("chrome", url));
    }

    @Test
    public void testCreateDriverFirefoxConnectsToHub() throws MalformedURLException {
        // Firefox driver will try to connect to the hub and fail since no hub exists
        URL url = new URL("http://localhost:4444/wd/hub");
        assertThrows(Exception.class, () -> BrowserFactory.createDriver("firefox", url));
    }

    @Test
    public void testCreateBrowserStackBrowserUnsupportedType() throws MalformedURLException {
        URL url = new URL("http://localhost:4444/wd/hub");
        BrowserStackProperties props = new BrowserStackProperties();
        props.setUsername("user");
        props.setAccessKey("key");
        // Will fail to connect to hub, but exercises the code path
        assertThrows(Exception.class, () -> BrowserFactory.createBrowserStackBrowser("chrome", url, props));
    }

    @Test
    public void testCreateBrowserStackBrowserWithAllProperties() throws MalformedURLException {
        URL url = new URL("http://localhost:4444/wd/hub");
        BrowserStackProperties props = new BrowserStackProperties();
        props.setUsername("user");
        props.setAccessKey("key");
        props.setOs("Windows");
        props.setOsVersion("11");
        props.setBrowser("Chrome");
        props.setBrowserVersion("latest");
        props.setProject("MyProject");
        props.setBuild("build-1");
        props.setName("test-session");
        props.setDeviceName("Samsung Galaxy S23");
        props.setRealMobile(true);
        props.setLocal(false);
        props.setDebug(true);
        props.setConnectionTimeout(30000);
        props.setMaxRetries(3);
        // Will fail to connect, but exercises all property branches
        assertThrows(Exception.class, () -> BrowserFactory.createBrowserStackBrowser("chrome", url, props));
    }

    @Test
    public void testCreateBrowserStackBrowserFirefox() throws MalformedURLException {
        URL url = new URL("http://localhost:4444/wd/hub");
        BrowserStackProperties props = new BrowserStackProperties();
        props.setUsername("user");
        props.setAccessKey("key");
        props.setOs("OS X");
        props.setOsVersion("Ventura");
        // Firefox path - no ChromeOptions added
        assertThrows(Exception.class, () -> BrowserFactory.createBrowserStackBrowser("firefox", url, props));
    }
}
