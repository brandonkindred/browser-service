package com.looksee.utils;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.junit.jupiter.api.BeforeEach;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

public class CssUtilsTest {

    interface MockJsDriver extends WebDriver, JavascriptExecutor {}

    @Mock
    private MockJsDriver driver;

    @Mock
    private WebElement element;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    public void testLoadCssProperties() {
        when(driver.executeScript(anyString(), eq(element)))
                .thenReturn("color:red;font-size:14px;margin:0;");

        Map<String, String> result = CssUtils.loadCssProperties(element, driver);
        assertNotNull(result);
        assertEquals("red", result.get("color"));
        assertEquals("14px", result.get("font-size"));
        assertEquals("0", result.get("margin"));
    }

    @Test
    public void testLoadCssPropertiesEmpty() {
        when(driver.executeScript(anyString(), eq(element))).thenReturn("");
        Map<String, String> result = CssUtils.loadCssProperties(element, driver);
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    public void testLoadCssPropertiesWithSinglePartValue() {
        when(driver.executeScript(anyString(), eq(element))).thenReturn("color;font-size:14px;");
        Map<String, String> result = CssUtils.loadCssProperties(element, driver);
        assertNotNull(result);
        assertNull(result.get("color")); // single part should be skipped
        assertEquals("14px", result.get("font-size"));
    }

    @Test
    public void testLoadTextCssProperties() {
        when(element.getCssValue("font-family")).thenReturn("Arial");
        when(element.getCssValue("font-size")).thenReturn("16px");
        when(element.getCssValue("text-decoration-color")).thenReturn("rgb(0,0,0)");
        when(element.getCssValue("text-emphasis-color")).thenReturn("");

        Map<String, String> result = CssUtils.loadTextCssProperties(element);
        assertNotNull(result);
        assertEquals("Arial", result.get("font-family"));
        assertEquals("16px", result.get("font-size"));
        assertEquals("rgb(0,0,0)", result.get("text-decoration-color"));
        assertFalse(result.containsKey("text-emphasis-color")); // empty value excluded
    }

    @Test
    public void testLoadTextCssPropertiesAllNull() {
        when(element.getCssValue(anyString())).thenReturn(null);

        Map<String, String> result = CssUtils.loadTextCssProperties(element);
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    public void testLoadTextCssPropertiesAllEmpty() {
        when(element.getCssValue(anyString())).thenReturn("");

        Map<String, String> result = CssUtils.loadTextCssProperties(element);
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }
}
