package com.looksee.config;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

public class AppiumPropertiesTest {

    @Test
    public void testConstructorWithAllValues() {
        AppiumProperties props = new AppiumProperties(
                "server1:4723,server2:4723", "Android", "Pixel 6",
                "Chrome", "UiAutomator2", "13.0", "/path/to/app.apk",
                90000, 5);

        assertEquals("server1:4723,server2:4723", props.getUrls());
        assertEquals("Android", props.getPlatformName());
        assertEquals("Pixel 6", props.getDeviceName());
        assertEquals("Chrome", props.getBrowserName());
        assertEquals("UiAutomator2", props.getAutomationName());
        assertEquals("13.0", props.getPlatformVersion());
        assertEquals("/path/to/app.apk", props.getApp());
        assertEquals(90000, props.getConnectionTimeout());
        assertEquals(5, props.getMaxRetries());
    }

    @Test
    public void testConstructorWithDefaults() {
        AppiumProperties props = new AppiumProperties(
                "server:4723", null, null, null, null, null, null, null, null);

        assertEquals("server:4723", props.getUrls());
        assertNull(props.getPlatformName());
        assertNull(props.getDeviceName());
        assertNull(props.getBrowserName());
        assertNull(props.getAutomationName());
        assertNull(props.getPlatformVersion());
        assertNull(props.getApp());
        assertEquals(60000, props.getConnectionTimeout());
        assertEquals(3, props.getMaxRetries());
    }

    @Test
    public void testGetUrlsArray() {
        AppiumProperties props = new AppiumProperties(
                "s1:4723,s2:4723,s3:4723", null, null, null, null, null, null, null, null);
        String[] urls = props.getUrlsArray();
        assertEquals(3, urls.length);
        assertEquals("s1:4723", urls[0]);
    }

    @Test
    public void testGetUrlsArrayNull() {
        AppiumProperties props = new AppiumProperties(
                null, null, null, null, null, null, null, null, null);
        assertEquals(0, props.getUrlsArray().length);
    }

    @Test
    public void testGetUrlsArrayEmpty() {
        AppiumProperties props = new AppiumProperties(
                "  ", null, null, null, null, null, null, null, null);
        assertEquals(0, props.getUrlsArray().length);
    }
}
