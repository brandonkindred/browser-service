package com.looksee.browsing.enums;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

public class AlertChoiceTest {

    @Test
    public void testShortNames() {
        assertEquals("dismiss", AlertChoice.DISMISS.getShortName());
        assertEquals("accept", AlertChoice.ACCEPT.getShortName());
    }

    @Test
    public void testToString() {
        assertEquals("dismiss", AlertChoice.DISMISS.toString());
        assertEquals("accept", AlertChoice.ACCEPT.toString());
    }

    @Test
    public void testCreateFromString() {
        assertEquals(AlertChoice.DISMISS, AlertChoice.create("dismiss"));
        assertEquals(AlertChoice.DISMISS, AlertChoice.create("Dismiss"));
        assertEquals(AlertChoice.ACCEPT, AlertChoice.create("accept"));
        assertEquals(AlertChoice.ACCEPT, AlertChoice.create("ACCEPT"));
    }

    @Test
    public void testCreateWithNull() {
        assertThrows(IllegalArgumentException.class, () -> AlertChoice.create(null));
    }

    @Test
    public void testCreateWithInvalidValue() {
        assertThrows(IllegalArgumentException.class, () -> AlertChoice.create("invalid"));
    }

    @Test
    public void testAllValuesExist() {
        assertEquals(2, AlertChoice.values().length);
    }
}
