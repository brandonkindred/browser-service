package com.looksee.browsing.enums;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

public class BrowserEnvironmentTest {

    @Test
    public void testShortNames() {
        assertEquals("test", BrowserEnvironment.TEST.getShortName());
        assertEquals("discovery", BrowserEnvironment.DISCOVERY.getShortName());
    }

    @Test
    public void testToString() {
        assertEquals("test", BrowserEnvironment.TEST.toString());
        assertEquals("discovery", BrowserEnvironment.DISCOVERY.toString());
    }

    @Test
    public void testCreateFromString() {
        assertEquals(BrowserEnvironment.TEST, BrowserEnvironment.create("test"));
        assertEquals(BrowserEnvironment.TEST, BrowserEnvironment.create("Test"));
        assertEquals(BrowserEnvironment.TEST, BrowserEnvironment.create("TEST"));
        assertEquals(BrowserEnvironment.DISCOVERY, BrowserEnvironment.create("discovery"));
        assertEquals(BrowserEnvironment.DISCOVERY, BrowserEnvironment.create("DISCOVERY"));
    }

    @Test
    public void testCreateWithNull() {
        assertThrows(IllegalArgumentException.class, () -> BrowserEnvironment.create(null));
    }

    @Test
    public void testCreateWithInvalidValue() {
        assertThrows(IllegalArgumentException.class, () -> BrowserEnvironment.create("invalid"));
        assertThrows(IllegalArgumentException.class, () -> BrowserEnvironment.create(""));
    }

    @Test
    public void testAllValuesExist() {
        assertEquals(2, BrowserEnvironment.values().length);
    }
}
