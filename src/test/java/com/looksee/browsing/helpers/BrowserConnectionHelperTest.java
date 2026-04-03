package com.looksee.browsing.helpers;

import static org.junit.jupiter.api.Assertions.*;

import java.net.MalformedURLException;

import com.looksee.browsing.enums.BrowserEnvironment;
import com.looksee.browsing.enums.BrowserType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class BrowserConnectionHelperTest {

    @BeforeEach
    public void setUp() {
        // Reset state before each test
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
}
