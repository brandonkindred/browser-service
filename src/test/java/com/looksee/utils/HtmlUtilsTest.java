package com.looksee.utils;

import static org.junit.jupiter.api.Assertions.*;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import cz.vutbr.web.css.RuleSet;
import org.junit.jupiter.api.Test;

public class HtmlUtilsTest {

    @Test
    public void testCleanSrcRemovesScripts() {
        String src = "<html><head><script>alert('hi')</script></head><body>Hello</body></html>";
        String result = HtmlUtils.cleanSrc(src);
        assertFalse(result.contains("<script>"));
        assertFalse(result.contains("alert"));
        assertTrue(result.contains("Hello"));
    }

    @Test
    public void testCleanSrcRemovesStyles() {
        String src = "<html><head><style>.a{color:red}</style></head><body>Content</body></html>";
        String result = HtmlUtils.cleanSrc(src);
        assertFalse(result.contains("<style>"));
        assertFalse(result.contains("color:red"));
        assertTrue(result.contains("Content"));
    }

    @Test
    public void testCleanSrcRemovesLinks() {
        String src = "<html><head><link rel='stylesheet' href='style.css'></head><body>Page</body></html>";
        String result = HtmlUtils.cleanSrc(src);
        assertFalse(result.contains("<link"));
        assertTrue(result.contains("Page"));
    }

    @Test
    public void testCleanSrcRemovesMeta() {
        String src = "<html><head><meta charset='utf-8'></head><body>Test</body></html>";
        String result = HtmlUtils.cleanSrc(src);
        assertFalse(result.contains("<meta"));
    }

    @Test
    public void testCleanSrcNormalizesWhitespace() {
        String src = "<html><body>Hello\n\n\tWorld   Test</body></html>";
        String result = HtmlUtils.cleanSrc(src);
        assertFalse(result.contains("\n"));
        assertFalse(result.contains("\t"));
    }

    @Test
    public void testCleanSrcRemovesEmptyStyle() {
        String src = "<html><body><div style=\"\">Content</div></body></html>";
        String result = HtmlUtils.cleanSrc(src);
        assertFalse(result.contains(" style=\"\""));
    }

    @Test
    public void testCleanSrcEmpty() {
        String result = HtmlUtils.cleanSrc("");
        assertNotNull(result);
    }

    @Test
    public void testExtractBodySimple() {
        String src = "<html><head><title>T</title></head><body><div>Hello</div></body></html>";
        String body = HtmlUtils.extractBody(src);
        assertTrue(body.contains("<div>"));
        assertTrue(body.contains("Hello"));
        assertFalse(body.contains("<title>"));
    }

    @Test
    public void testExtractBodyEmpty() {
        String body = HtmlUtils.extractBody("<html><body></body></html>");
        assertNotNull(body);
    }

    @Test
    public void testExtractBodyNoBodyTag() {
        String body = HtmlUtils.extractBody("<html><div>No body tags</div></html>");
        assertNotNull(body);
    }

    @Test
    public void testIs503ErrorTrue() {
        assertTrue(HtmlUtils.is503Error("503 Service Temporarily Unavailable"));
    }

    @Test
    public void testIs503ErrorFalse() {
        assertFalse(HtmlUtils.is503Error("<html>200 OK</html>"));
    }

    @Test
    public void testIs503ErrorEmptyString() {
        assertFalse(HtmlUtils.is503Error(""));
    }

    @Test
    public void testIs503ErrorPartialMatch() {
        assertFalse(HtmlUtils.is503Error("503 error"));
        assertTrue(HtmlUtils.is503Error("Error: 503 Service Temporarily Unavailable, please retry"));
    }

    @Test
    public void testExtractStylesheetsNoLinks() {
        String src = "<html><body>No stylesheets</body></html>";
        List<String> result = HtmlUtils.extractStylesheets(src);
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    public void testExtractStylesheetsNonStylesheetLinks() {
        String src = "<html><head><link rel='icon' href='favicon.ico'></head><body></body></html>";
        List<String> result = HtmlUtils.extractStylesheets(src);
        assertTrue(result.isEmpty());
    }

    @Test
    public void testExtractStylesheetsMalformedUrl() {
        String src = "<html><head><link rel='stylesheet' href='not-a-valid-url'></head><body></body></html>";
        List<String> result = HtmlUtils.extractStylesheets(src);
        assertNotNull(result);
        // Should handle the malformed URL gracefully
    }

    @Test
    public void testExtractRuleSetsFromStylesheetsEmpty() throws Exception {
        List<String> stylesheets = new ArrayList<>();
        URL url = new URL("http://example.com");
        List<RuleSet> result = HtmlUtils.extractRuleSetsFromStylesheets(stylesheets, url);
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    public void testExtractRuleSetsFromStylesheetsValid() throws Exception {
        List<String> stylesheets = new ArrayList<>();
        stylesheets.add("body { color: red; } h1 { font-size: 24px; }");
        URL url = new URL("http://example.com");
        List<RuleSet> result = HtmlUtils.extractRuleSetsFromStylesheets(stylesheets, url);
        assertNotNull(result);
        assertFalse(result.isEmpty());
    }

    @Test
    public void testExtractRuleSetsSkipsFontFaceAndMedia() throws Exception {
        List<String> stylesheets = new ArrayList<>();
        stylesheets.add("@font-face { font-family: 'MyFont'; src: url('font.woff'); } " +
                "@media screen { body { color: blue; } } " +
                "p { margin: 0; }");
        URL url = new URL("http://example.com");
        List<RuleSet> result = HtmlUtils.extractRuleSetsFromStylesheets(stylesheets, url);
        assertNotNull(result);
        // Should only contain the 'p' rule, not @font-face or @media
    }

    @Test
    public void testExtractRuleSetsInvalidCss() throws Exception {
        List<String> stylesheets = new ArrayList<>();
        stylesheets.add("this is not valid css {{{");
        URL url = new URL("http://example.com");
        List<RuleSet> result = HtmlUtils.extractRuleSetsFromStylesheets(stylesheets, url);
        assertNotNull(result);
    }
}
