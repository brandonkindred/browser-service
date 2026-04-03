package com.looksee.config;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

public class SeleniumPropertiesTest {

    @Test
    public void testConstructorWithAllValues() {
        SeleniumProperties props = new SeleniumProperties(
                "http://hub1:4444,http://hub2:4444", 5000, 5, false, 20000);
        assertEquals("http://hub1:4444,http://hub2:4444", props.getUrls());
        assertEquals(5000, props.getConnectionTimeout());
        assertEquals(5, props.getMaxRetries());
        assertFalse(props.isImplicitWaitEnabled());
        assertEquals(20000, props.getImplicitWaitTimeout());
    }

    @Test
    public void testConstructorWithDefaults() {
        SeleniumProperties props = new SeleniumProperties("http://hub:4444", null, null, null, null);
        assertEquals("http://hub:4444", props.getUrls());
        assertEquals(30000, props.getConnectionTimeout());
        assertEquals(3, props.getMaxRetries());
        assertTrue(props.isImplicitWaitEnabled());
        assertEquals(10000, props.getImplicitWaitTimeout());
    }

    @Test
    public void testGetUrlsArray() {
        SeleniumProperties props = new SeleniumProperties("hub1,hub2,hub3", null, null, null, null);
        String[] urls = props.getUrlsArray();
        assertEquals(3, urls.length);
        assertEquals("hub1", urls[0]);
        assertEquals("hub2", urls[1]);
        assertEquals("hub3", urls[2]);
    }

    @Test
    public void testGetUrlsArrayNull() {
        SeleniumProperties props = new SeleniumProperties(null, null, null, null, null);
        String[] urls = props.getUrlsArray();
        assertEquals(0, urls.length);
    }

    @Test
    public void testGetUrlsArrayEmpty() {
        SeleniumProperties props = new SeleniumProperties("  ", null, null, null, null);
        String[] urls = props.getUrlsArray();
        assertEquals(0, urls.length);
    }

    @Test
    public void testGetUrlsSingleUrl() {
        SeleniumProperties props = new SeleniumProperties("http://single-hub:4444", null, null, null, null);
        String[] urls = props.getUrlsArray();
        assertEquals(1, urls.length);
        assertEquals("http://single-hub:4444", urls[0]);
    }
}
