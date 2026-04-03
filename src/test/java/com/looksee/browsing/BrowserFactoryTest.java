package com.looksee.browsing;

import static org.junit.jupiter.api.Assertions.*;

import java.net.MalformedURLException;
import java.net.URL;

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
}
