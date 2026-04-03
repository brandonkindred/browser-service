package com.looksee.browsing;

import static org.junit.jupiter.api.Assertions.*;

import java.net.MalformedURLException;
import java.net.URL;

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
}
