package com.looksee.browser.utils;

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

    @Test
    public void testLoadCssPropertiesMultipleColons() {
        // Properties with colons in values (e.g., URLs)
        when(driver.executeScript(anyString(), eq(element)))
                .thenReturn("background:url(http;font-size:14px;");

        Map<String, String> result = CssUtils.loadCssProperties(element, driver);
        assertNotNull(result);
        assertEquals("14px", result.get("font-size"));
    }

    @Test
    public void testLoadCssPropertiesSemicolonOnly() {
        when(driver.executeScript(anyString(), eq(element))).thenReturn(";");

        Map<String, String> result = CssUtils.loadCssProperties(element, driver);
        assertNotNull(result);
    }

    @Test
    public void testLoadPreRenderCssProperties() throws Exception {
        org.jsoup.nodes.Document jsoupDoc = org.jsoup.Jsoup.parse("<html><body><p>Hello</p></body></html>");
        org.jsoup.nodes.Element elem = jsoupDoc.select("p").first();

        java.util.Map<String, java.util.Map<String, String>> ruleSetList = new java.util.HashMap<>();
        java.util.Map<String, String> rules = new java.util.HashMap<>();
        rules.put("color", "red");
        ruleSetList.put("p", rules);

        java.net.URL url = new java.net.URL("http://example.com");
        // w3c_document is asserted non-null but not used in the method body
        javax.xml.parsers.DocumentBuilder db = javax.xml.parsers.DocumentBuilderFactory.newInstance().newDocumentBuilder();
        org.w3c.dom.Document w3cDoc = db.newDocument();

        Map<String, String> result = CssUtils.loadPreRenderCssProperties(
                jsoupDoc, w3cDoc, ruleSetList, url, "//p", elem);
        assertNotNull(result);
        assertEquals("red", result.get("color"));
    }

    @Test
    public void testLoadPreRenderCssPropertiesSkipsAtRules() throws Exception {
        org.jsoup.nodes.Document jsoupDoc = org.jsoup.Jsoup.parse("<html><body><p>Hello</p></body></html>");
        org.jsoup.nodes.Element elem = jsoupDoc.select("p").first();

        java.util.Map<String, java.util.Map<String, String>> ruleSetList = new java.util.HashMap<>();
        java.util.Map<String, String> rules = new java.util.HashMap<>();
        rules.put("font-family", "Arial");
        ruleSetList.put("@font-face", rules);

        java.net.URL url = new java.net.URL("http://example.com");
        javax.xml.parsers.DocumentBuilder db = javax.xml.parsers.DocumentBuilderFactory.newInstance().newDocumentBuilder();
        org.w3c.dom.Document w3cDoc = db.newDocument();

        Map<String, String> result = CssUtils.loadPreRenderCssProperties(
                jsoupDoc, w3cDoc, ruleSetList, url, "//p", elem);
        assertNotNull(result);
        assertTrue(result.isEmpty()); // @font-face should be skipped
    }

    @Test
    public void testLoadPreRenderCssPropertiesWithPseudoSelector() throws Exception {
        org.jsoup.nodes.Document jsoupDoc = org.jsoup.Jsoup.parse("<html><body><p>Hello</p></body></html>");
        org.jsoup.nodes.Element elem = jsoupDoc.select("p").first();

        java.util.Map<String, java.util.Map<String, String>> ruleSetList = new java.util.HashMap<>();
        java.util.Map<String, String> rules = new java.util.HashMap<>();
        rules.put("content", "''");
        ruleSetList.put("p:before", rules);

        java.net.URL url = new java.net.URL("http://example.com");
        javax.xml.parsers.DocumentBuilder db = javax.xml.parsers.DocumentBuilderFactory.newInstance().newDocumentBuilder();
        org.w3c.dom.Document w3cDoc = db.newDocument();

        Map<String, String> result = CssUtils.loadPreRenderCssProperties(
                jsoupDoc, w3cDoc, ruleSetList, url, "//p", elem);
        assertNotNull(result);
        // Should strip :before and match <p>
        assertEquals("''", result.get("content"));
    }

    @Test
    public void testLoadPreRenderCssPropertiesNoMatch() throws Exception {
        org.jsoup.nodes.Document jsoupDoc = org.jsoup.Jsoup.parse("<html><body><p>Hello</p></body></html>");
        org.jsoup.nodes.Element elem = jsoupDoc.select("p").first();

        java.util.Map<String, java.util.Map<String, String>> ruleSetList = new java.util.HashMap<>();
        java.util.Map<String, String> rules = new java.util.HashMap<>();
        rules.put("color", "blue");
        ruleSetList.put("div", rules);

        java.net.URL url = new java.net.URL("http://example.com");
        javax.xml.parsers.DocumentBuilder db = javax.xml.parsers.DocumentBuilderFactory.newInstance().newDocumentBuilder();
        org.w3c.dom.Document w3cDoc = db.newDocument();

        Map<String, String> result = CssUtils.loadPreRenderCssProperties(
                jsoupDoc, w3cDoc, ruleSetList, url, "//p", elem);
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    public void testLoadTextCssPropertiesPartial() {
        when(element.getCssValue("font-family")).thenReturn("Helvetica");
        when(element.getCssValue("font-size")).thenReturn(null);
        when(element.getCssValue("text-decoration-color")).thenReturn("rgb(0,0,0)");
        when(element.getCssValue("text-emphasis-color")).thenReturn("transparent");

        Map<String, String> result = CssUtils.loadTextCssProperties(element);
        assertNotNull(result);
        assertEquals("Helvetica", result.get("font-family"));
        assertFalse(result.containsKey("font-size"));
        assertEquals("transparent", result.get("text-emphasis-color"));
    }
}
