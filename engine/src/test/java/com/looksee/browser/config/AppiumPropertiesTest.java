package com.looksee.browser.config;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

public class AppiumPropertiesTest {

    @Test
    public void testSetters() {
        AppiumProperties props = new AppiumProperties();
        props.setUrls("server1:4723,server2:4723");
        props.setPlatformName("Android");
        props.setDeviceName("Pixel 6");
        props.setBrowserName("Chrome");
        props.setAutomationName("UiAutomator2");
        props.setPlatformVersion("13.0");
        props.setApp("/path/to/app.apk");
        props.setConnectionTimeout(90000);
        props.setMaxRetries(5);

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
    public void testDefaults() {
        AppiumProperties props = new AppiumProperties();
        props.setUrls("server:4723");

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
        AppiumProperties props = new AppiumProperties();
        props.setUrls("s1:4723,s2:4723,s3:4723");
        String[] urls = props.getUrlsArray();
        assertEquals(3, urls.length);
        assertEquals("s1:4723", urls[0]);
    }

    @Test
    public void testGetUrlsArrayNull() {
        AppiumProperties props = new AppiumProperties();
        assertEquals(0, props.getUrlsArray().length);
    }

    @Test
    public void testGetUrlsArrayEmpty() {
        AppiumProperties props = new AppiumProperties();
        props.setUrls("  ");
        assertEquals(0, props.getUrlsArray().length);
    }
}
