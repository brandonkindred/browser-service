package com.looksee.browser;

import static org.junit.jupiter.api.Assertions.*;

import cz.vutbr.web.css.CSSProperty;
import cz.vutbr.web.css.CSSProperty.Margin;
import cz.vutbr.web.css.CSSProperty.LineHeight;
import org.junit.jupiter.api.Test;

public class CssPropertyFactoryTest {

    @Test
    public void testConstructMarginProperty() {
        String result = CssPropertyFactory.construct(Margin.AUTO);
        assertEquals("auto", result);
    }

    @Test
    public void testConstructMarginLength() {
        String result = CssPropertyFactory.construct(Margin.length);
        assertNotNull(result);
    }

    @Test
    public void testConstructLineHeightProperty() {
        String result = CssPropertyFactory.construct(LineHeight.NORMAL);
        assertEquals("normal", result);
    }

    @Test
    public void testConstructLineHeightNumber() {
        String result = CssPropertyFactory.construct(LineHeight.number);
        assertNotNull(result);
    }

    @Test
    public void testConstructOtherProperty() {
        // Test with a property that is neither Margin nor LineHeight
        CSSProperty.Color color = CSSProperty.Color.color;
        String result = CssPropertyFactory.construct(color);
        assertNotNull(result);
    }
}
