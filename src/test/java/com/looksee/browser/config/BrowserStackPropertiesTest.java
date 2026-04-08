package com.looksee.browser.config;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

public class BrowserStackPropertiesTest {

    @Test
    public void testSetters() {
        BrowserStackProperties props = new BrowserStackProperties();
        props.setUsername("myuser");
        props.setAccessKey("mykey123");
        props.setOs("Windows");
        props.setOsVersion("11");
        props.setBrowser("Chrome");
        props.setBrowserVersion("latest");
        props.setProject("MyProject");
        props.setBuild("build-1");
        props.setName("test-session");
        props.setDeviceName("Samsung Galaxy S23");
        props.setRealMobile(false);
        props.setLocal(true);
        props.setDebug(false);
        props.setConnectionTimeout(60000);
        props.setMaxRetries(5);

        assertEquals("myuser", props.getUsername());
        assertEquals("mykey123", props.getAccessKey());
        assertEquals("Windows", props.getOs());
        assertEquals("11", props.getOsVersion());
        assertEquals("Chrome", props.getBrowser());
        assertEquals("latest", props.getBrowserVersion());
        assertEquals("MyProject", props.getProject());
        assertEquals("build-1", props.getBuild());
        assertEquals("test-session", props.getName());
        assertEquals("Samsung Galaxy S23", props.getDeviceName());
        assertFalse(props.isRealMobile());
        assertTrue(props.isLocal());
        assertFalse(props.isDebug());
        assertEquals(60000, props.getConnectionTimeout());
        assertEquals(5, props.getMaxRetries());
    }

    @Test
    public void testDefaults() {
        BrowserStackProperties props = new BrowserStackProperties();
        props.setUsername("myuser");
        props.setAccessKey("mykey123");

        assertEquals("myuser", props.getUsername());
        assertEquals("mykey123", props.getAccessKey());
        assertNull(props.getOs());
        assertNull(props.getOsVersion());
        assertNull(props.getBrowser());
        assertNull(props.getBrowserVersion());
        assertNull(props.getProject());
        assertNull(props.getBuild());
        assertNull(props.getName());
        assertNull(props.getDeviceName());
        assertTrue(props.isRealMobile());
        assertFalse(props.isLocal());
        assertTrue(props.isDebug());
        assertEquals(30000, props.getConnectionTimeout());
        assertEquals(3, props.getMaxRetries());
    }
}
