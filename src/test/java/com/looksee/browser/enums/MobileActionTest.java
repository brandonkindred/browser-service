package com.looksee.browser.enums;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

public class MobileActionTest {

    @Test
    public void testShortNames() {
        assertEquals("tap", MobileAction.TAP.getShortName());
        assertEquals("doubleTap", MobileAction.DOUBLE_TAP.getShortName());
        assertEquals("longPress", MobileAction.LONG_PRESS.getShortName());
        assertEquals("swipeUp", MobileAction.SWIPE_UP.getShortName());
        assertEquals("swipeDown", MobileAction.SWIPE_DOWN.getShortName());
        assertEquals("swipeLeft", MobileAction.SWIPE_LEFT.getShortName());
        assertEquals("swipeRight", MobileAction.SWIPE_RIGHT.getShortName());
        assertEquals("scrollUp", MobileAction.SCROLL_UP.getShortName());
        assertEquals("scrollDown", MobileAction.SCROLL_DOWN.getShortName());
        assertEquals("pinch", MobileAction.PINCH.getShortName());
        assertEquals("zoom", MobileAction.ZOOM.getShortName());
        assertEquals("sendKeys", MobileAction.SEND_KEYS.getShortName());
        assertEquals("unknown", MobileAction.UNKNOWN.getShortName());
    }

    @Test
    public void testToString() {
        assertEquals("tap", MobileAction.TAP.toString());
        assertEquals("longPress", MobileAction.LONG_PRESS.toString());
    }

    @Test
    public void testCreateFromString() {
        assertEquals(MobileAction.TAP, MobileAction.create("tap"));
        assertEquals(MobileAction.TAP, MobileAction.create("Tap"));
        assertEquals(MobileAction.DOUBLE_TAP, MobileAction.create("doubleTap"));
        assertEquals(MobileAction.LONG_PRESS, MobileAction.create("longPress"));
        assertEquals(MobileAction.SWIPE_UP, MobileAction.create("swipeUp"));
        assertEquals(MobileAction.SWIPE_DOWN, MobileAction.create("swipeDown"));
        assertEquals(MobileAction.SWIPE_LEFT, MobileAction.create("swipeLeft"));
        assertEquals(MobileAction.SWIPE_RIGHT, MobileAction.create("swipeRight"));
        assertEquals(MobileAction.SCROLL_UP, MobileAction.create("scrollUp"));
        assertEquals(MobileAction.SCROLL_DOWN, MobileAction.create("scrollDown"));
        assertEquals(MobileAction.PINCH, MobileAction.create("pinch"));
        assertEquals(MobileAction.ZOOM, MobileAction.create("zoom"));
        assertEquals(MobileAction.SEND_KEYS, MobileAction.create("sendKeys"));
        assertEquals(MobileAction.UNKNOWN, MobileAction.create("unknown"));
    }

    @Test
    public void testCreateWithNull() {
        assertThrows(IllegalArgumentException.class, () -> MobileAction.create(null));
    }

    @Test
    public void testCreateWithInvalidValue() {
        assertThrows(IllegalArgumentException.class, () -> MobileAction.create("invalid"));
    }

    @Test
    public void testAllValuesExist() {
        assertEquals(13, MobileAction.values().length);
    }
}
