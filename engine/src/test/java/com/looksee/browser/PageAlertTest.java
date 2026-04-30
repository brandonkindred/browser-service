package com.looksee.browser;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.looksee.browser.enums.AlertChoice;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.Alert;
import org.openqa.selenium.NoAlertPresentException;
import org.openqa.selenium.WebDriver;

public class PageAlertTest {

    @Test
    public void testConstructor() {
        PageAlert alert = new PageAlert("Test message");
        assertEquals("Test message", alert.getMessage());
        assertNotNull(alert.getKey());
    }

    @Test
    public void testGetKey() {
        PageAlert alert = new PageAlert("Test message");
        String key = alert.getKey();
        assertNotNull(key);
        assertTrue(key.startsWith("alert"));
        assertEquals(key, alert.generateKey());
    }

    @Test
    public void testGenerateKeyConsistency() {
        PageAlert alert1 = new PageAlert("Same message");
        PageAlert alert2 = new PageAlert("Same message");
        assertEquals(alert1.getKey(), alert2.getKey());
    }

    @Test
    public void testGenerateKeyDifferentMessages() {
        PageAlert alert1 = new PageAlert("Message 1");
        PageAlert alert2 = new PageAlert("Message 2");
        assertNotEquals(alert1.getKey(), alert2.getKey());
    }

    @Test
    public void testEquals() {
        PageAlert alert1 = new PageAlert("Test");
        PageAlert alert2 = new PageAlert("Test");
        PageAlert alert3 = new PageAlert("Different");

        assertEquals(alert1, alert2);
        assertNotEquals(alert1, alert3);
        assertEquals(alert1, alert1);
        assertNotEquals(alert1, null);
        assertNotEquals(alert1, "not a PageAlert");
    }

    @Test
    public void testHashCode() {
        PageAlert alert1 = new PageAlert("Test");
        PageAlert alert2 = new PageAlert("Test");
        assertEquals(alert1.hashCode(), alert2.hashCode());
    }

    @Test
    public void testClone() {
        PageAlert original = new PageAlert("Clone me");
        PageAlert cloned = original.clone();
        assertEquals(original, cloned);
        assertEquals(original.getMessage(), cloned.getMessage());
        assertNotSame(original, cloned);
    }

    @Test
    public void testPerformChoiceAccept() {
        WebDriver driver = mock(WebDriver.class);
        WebDriver.TargetLocator targetLocator = mock(WebDriver.TargetLocator.class);
        Alert seleniumAlert = mock(Alert.class);
        when(driver.switchTo()).thenReturn(targetLocator);
        when(targetLocator.alert()).thenReturn(seleniumAlert);

        PageAlert pageAlert = new PageAlert("Accept test");
        pageAlert.performChoice(driver, AlertChoice.ACCEPT);
        verify(seleniumAlert).accept();
    }

    @Test
    public void testPerformChoiceDismiss() {
        WebDriver driver = mock(WebDriver.class);
        WebDriver.TargetLocator targetLocator = mock(WebDriver.TargetLocator.class);
        Alert seleniumAlert = mock(Alert.class);
        when(driver.switchTo()).thenReturn(targetLocator);
        when(targetLocator.alert()).thenReturn(seleniumAlert);

        PageAlert pageAlert = new PageAlert("Dismiss test");
        pageAlert.performChoice(driver, AlertChoice.DISMISS);
        verify(seleniumAlert).dismiss();
    }

    @Test
    public void testPerformChoiceNoAlertPresent() {
        WebDriver driver = mock(WebDriver.class);
        WebDriver.TargetLocator targetLocator = mock(WebDriver.TargetLocator.class);
        when(driver.switchTo()).thenReturn(targetLocator);
        when(targetLocator.alert()).thenThrow(new NoAlertPresentException());

        PageAlert pageAlert = new PageAlert("No alert");
        // Should not throw
        assertDoesNotThrow(() -> pageAlert.performChoice(driver, AlertChoice.ACCEPT));
    }

    @Test
    public void testGetMessageStatic() {
        Alert alert = mock(Alert.class);
        when(alert.getText()).thenReturn("Static message");
        assertEquals("Static message", PageAlert.getMessage(alert));
    }
}
