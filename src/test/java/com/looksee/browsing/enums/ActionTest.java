package com.looksee.browsing.enums;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

public class ActionTest {

    @Test
    public void testShortNames() {
        assertEquals("click", Action.CLICK.getShortName());
        assertEquals("doubleClick", Action.DOUBLE_CLICK.getShortName());
        assertEquals("hover", Action.HOVER.getShortName());
        assertEquals("clickAndHold", Action.CLICK_AND_HOLD.getShortName());
        assertEquals("contextClick", Action.CONTEXT_CLICK.getShortName());
        assertEquals("release", Action.RELEASE.getShortName());
        assertEquals("sendKeys", Action.SEND_KEYS.getShortName());
        assertEquals("mouseover", Action.MOUSE_OVER.getShortName());
        assertEquals("unknown", Action.UNKNOWN.getShortName());
    }

    @Test
    public void testToString() {
        assertEquals("click", Action.CLICK.toString());
        assertEquals("doubleClick", Action.DOUBLE_CLICK.toString());
    }

    @Test
    public void testCreateFromString() {
        assertEquals(Action.CLICK, Action.create("click"));
        assertEquals(Action.CLICK, Action.create("Click"));
        assertEquals(Action.DOUBLE_CLICK, Action.create("doubleClick"));
        assertEquals(Action.HOVER, Action.create("hover"));
        assertEquals(Action.CLICK_AND_HOLD, Action.create("clickAndHold"));
        assertEquals(Action.CONTEXT_CLICK, Action.create("contextClick"));
        assertEquals(Action.RELEASE, Action.create("release"));
        assertEquals(Action.SEND_KEYS, Action.create("sendKeys"));
        assertEquals(Action.MOUSE_OVER, Action.create("mouseover"));
        assertEquals(Action.UNKNOWN, Action.create("unknown"));
    }

    @Test
    public void testCreateWithNull() {
        assertThrows(IllegalArgumentException.class, () -> Action.create(null));
    }

    @Test
    public void testCreateWithInvalidValue() {
        assertThrows(IllegalArgumentException.class, () -> Action.create("invalid"));
    }

    @Test
    public void testAllValuesExist() {
        assertEquals(9, Action.values().length);
    }
}
