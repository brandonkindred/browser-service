package com.looksee.browsing.enums;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

public class BrowserTypeTest {

    @Test
    public void testShortNames() {
        assertEquals("chrome", BrowserType.CHROME.getShortName());
        assertEquals("firefox", BrowserType.FIREFOX.getShortName());
        assertEquals("safari", BrowserType.SAFARI.getShortName());
        assertEquals("ie", BrowserType.IE.getShortName());
        assertEquals("android", BrowserType.ANDROID.getShortName());
        assertEquals("ios", BrowserType.IOS.getShortName());
    }

    @Test
    public void testToString() {
        assertEquals("chrome", BrowserType.CHROME.toString());
        assertEquals("firefox", BrowserType.FIREFOX.toString());
        assertEquals("android", BrowserType.ANDROID.toString());
        assertEquals("ios", BrowserType.IOS.toString());
    }

    @Test
    public void testCreateFromString() {
        assertEquals(BrowserType.CHROME, BrowserType.create("chrome"));
        assertEquals(BrowserType.CHROME, BrowserType.create("Chrome"));
        assertEquals(BrowserType.CHROME, BrowserType.create("CHROME"));
        assertEquals(BrowserType.FIREFOX, BrowserType.create("firefox"));
        assertEquals(BrowserType.SAFARI, BrowserType.create("safari"));
        assertEquals(BrowserType.IE, BrowserType.create("ie"));
        assertEquals(BrowserType.ANDROID, BrowserType.create("android"));
        assertEquals(BrowserType.IOS, BrowserType.create("ios"));
        assertEquals(BrowserType.IOS, BrowserType.create("IOS"));
    }

    @Test
    public void testCreateWithNull() {
        assertThrows(IllegalArgumentException.class, () -> BrowserType.create(null));
    }

    @Test
    public void testCreateWithInvalidValue() {
        assertThrows(IllegalArgumentException.class, () -> BrowserType.create("invalid"));
        assertThrows(IllegalArgumentException.class, () -> BrowserType.create(""));
    }

    @Test
    public void testIsMobile() {
        assertFalse(BrowserType.CHROME.isMobile());
        assertFalse(BrowserType.FIREFOX.isMobile());
        assertFalse(BrowserType.SAFARI.isMobile());
        assertFalse(BrowserType.IE.isMobile());
        assertTrue(BrowserType.ANDROID.isMobile());
        assertTrue(BrowserType.IOS.isMobile());
    }

    @Test
    public void testAllValuesExist() {
        assertEquals(6, BrowserType.values().length);
    }
}
