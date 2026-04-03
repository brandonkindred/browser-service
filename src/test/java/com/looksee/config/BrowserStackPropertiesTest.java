package com.looksee.config;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

public class BrowserStackPropertiesTest {

    @Test
    public void testConstructorWithAllValues() {
        BrowserStackProperties props = new BrowserStackProperties(
                "myuser", "mykey123", "Windows", "11",
                "Chrome", "latest", "MyProject", "build-1",
                "test-session", true, false, 60000, 5);

        assertEquals("myuser", props.getUsername());
        assertEquals("mykey123", props.getAccessKey());
        assertEquals("Windows", props.getOs());
        assertEquals("11", props.getOsVersion());
        assertEquals("Chrome", props.getBrowser());
        assertEquals("latest", props.getBrowserVersion());
        assertEquals("MyProject", props.getProject());
        assertEquals("build-1", props.getBuild());
        assertEquals("test-session", props.getName());
        assertTrue(props.isLocal());
        assertFalse(props.isDebug());
        assertEquals(60000, props.getConnectionTimeout());
        assertEquals(5, props.getMaxRetries());
    }

    @Test
    public void testConstructorWithDefaults() {
        BrowserStackProperties props = new BrowserStackProperties(
                "myuser", "mykey123", null, null,
                null, null, null, null,
                null, null, null, null, null);

        assertEquals("myuser", props.getUsername());
        assertEquals("mykey123", props.getAccessKey());
        assertNull(props.getOs());
        assertNull(props.getOsVersion());
        assertNull(props.getBrowser());
        assertNull(props.getBrowserVersion());
        assertNull(props.getProject());
        assertNull(props.getBuild());
        assertNull(props.getName());
        assertFalse(props.isLocal());
        assertTrue(props.isDebug());
        assertEquals(30000, props.getConnectionTimeout());
        assertEquals(3, props.getMaxRetries());
    }
}
