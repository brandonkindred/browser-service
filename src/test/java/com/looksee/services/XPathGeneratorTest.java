package com.looksee.services;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.looksee.services.BrowserService;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.junit.jupiter.api.Test;

/**
 * XPathGeneratorTest is a class that tests the XPathGenerator class.
 */
public class XPathGeneratorTest {

    /**
     * Tests the getXPath method for a single element.
     */
    @Test
    public void testGetXPath_SingleElement() {
        String html = "<div><span>First</span></div>";
        Document doc = Jsoup.parse(html);
        Element element = doc.select("span").first();

        String xpath = BrowserService.getXPath(element);
        assertEquals("//body/div[1]/span[1]", xpath);
    }

    /**
     * Tests the getXPath method for multiple siblings.
     */
    @Test
    public void testGetXPath_MultipleSiblings() {
        String html = "<div><span>First</span><span>Second</span><span>Third</span></div>";
        Document doc = Jsoup.parse(html);
        Element element = doc.select("span").get(1); // Select the second <span> element

        String xpath = BrowserService.getXPath(element);
        assertEquals("//body/div[1]/span[2]", xpath);
    }

    /**
     * Tests the getXPath method for nested elements.
     */
    @Test
    public void testGetXPath_NestedElements() {
        String html = "<div><span>First</span><div><span>Nested</span></div></div>";
        Document doc = Jsoup.parse(html);
        Element element = doc.select("span").get(1); // Select the second <span> element (Nested)

        String xpath = BrowserService.getXPath(element);
        assertEquals("//body/div[1]/div[1]/span[1]", xpath);
    }

    /**
     * Tests the getXPath method for multiple nested elements.
     */
    @Test
    public void testGetXPath_MultipleNestedElements() {
        String html = "<div><span>First</span><div><span>Nested</span><span>Deep Nested</span></div></div>";
        Document doc = Jsoup.parse(html);
        Element element = doc.select("span").get(2); // Select the third <span> element (Deep Nested)

        String xpath = BrowserService.getXPath(element);
        assertEquals("//body/div[1]/div[1]/span[2]", xpath);
    }

    /**
     * Tests the getXPath method for multiple same tag ancestors.
     */
    @Test
    public void testGetXPath_MultipleSameTagAncestors() {
        String html = "<div><div><span>First</span></div><div><span>Second</span></div></div>";
        Document doc = Jsoup.parse(html);
        Element element = doc.select("span").get(1); // Select the second <span> element

        String xpath = BrowserService.getXPath(element);
        assertEquals("//body/div[1]/div[2]/span[1]", xpath);
    }

    /**
     * Tests the getXPath method for deeply nested elements.
     */
    @Test
    public void testGetXPath_DeeplyNestedElement() {
        String html = "<div><div><div><div><span>Deep</span></div></div></div></div>";
        Document doc = Jsoup.parse(html);
        Element element = doc.select("span").first();

        String xpath = BrowserService.getXPath(element);
        assertEquals("//body/div[1]/div[1]/div[1]/div[1]/span[1]", xpath);
    }

    /**
     * Tests the getXPath method for elements with siblings and nested children.
     */
    @Test
    public void testGetXPath_ElementWithSiblingsAndNestedChildren() {
        String html = "<div><span>First</span><span><div><p>Nested Paragraph</p></div></span></div>";
        Document doc = Jsoup.parse(html);
        Element element = doc.select("p").first(); // Select the <p> element inside the second <span>

        String xpath = BrowserService.getXPath(element);
        assertEquals("//body/div[1]/span[2]/div[1]/p[1]", xpath);
    }

    /**
     * Tests the getXPath method for elements without siblings.
     */
    @Test
    public void testGetXPath_ElementWithoutSiblings() {
        String html = "<div><span>Only Child</span></div>";
        Document doc = Jsoup.parse(html);
        Element element = doc.select("span").first();

        String xpath = BrowserService.getXPath(element);
        assertEquals("//body/div[1]/span[1]", xpath);
    }
}