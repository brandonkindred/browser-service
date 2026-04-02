package com.looksee.browsing;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.Point;

/**
 * Unit tests for browsing package classes.
 */
class BrowsingClassesTest {

    // ===== Coordinates =====
    @Test
    void coordinatesFromPointAndDimension() {
        Point point = new Point(10, 20);
        Dimension size = new Dimension(100, 200);
        Coordinates coords = new Coordinates(point, size, 2.0);
        assertEquals(200, coords.getWidth());
        assertEquals(400, coords.getHeight());
        assertEquals(20, coords.getX());
        assertEquals(40, coords.getY());
    }

    @Test
    void coordinatesWithPixelRatioOne() {
        Point point = new Point(50, 100);
        Dimension size = new Dimension(300, 400);
        Coordinates coords = new Coordinates(point, size, 1.0);
        assertEquals(300, coords.getWidth());
        assertEquals(400, coords.getHeight());
        assertEquals(50, coords.getX());
        assertEquals(100, coords.getY());
    }

    // ===== ElementNode =====
    @Test
    void elementNodeConstructor() {
        ElementNode<String> node = new ElementNode<>("root");
        assertEquals("root", node.getData());
        assertNotNull(node.getChildren());
        assertTrue(node.getChildren().isEmpty());
    }

    @Test
    void elementNodeAddChild() {
        ElementNode<String> parent = new ElementNode<>("parent");
        ElementNode<String> child = new ElementNode<>("child");
        parent.addChild(child);
        assertEquals(1, parent.getChildren().size());
        assertEquals(parent, child.getParent());
    }

    @Test
    void elementNodeSetParent() {
        ElementNode<String> parent = new ElementNode<>("parent");
        ElementNode<String> child = new ElementNode<>("child");
        child.setParent(parent);
        assertEquals(parent, child.getParent());
    }

    // ===== ValueDomain =====
    @Test
    void valueDomainDefaultConstructor() {
        ValueDomain domain = new ValueDomain();
        assertNotNull(domain.getValues());
        assertFalse(domain.getValues().isEmpty()); // contains empty string
    }

    @Test
    void valueDomainGenerateAllValueTypes() {
        ValueDomain domain = new ValueDomain();
        int initialSize = domain.getValues().size();
        domain.addRandomRealNumbers();
        assertTrue(domain.getValues().size() > initialSize);
    }

    @Test
    void valueDomainToString() {
        ValueDomain domain = new ValueDomain();
        assertEquals("", domain.toString());
    }

    // ===== Row =====
    @Test
    void rowDefaultConstructor() {
        com.looksee.browsing.table.Row row = new com.looksee.browsing.table.Row();
        assertNotNull(row);
    }

    // ===== Table =====
    @Test
    void tableDefaultConstructor() {
        com.looksee.browsing.table.Table table = new com.looksee.browsing.table.Table();
        assertNotNull(table);
    }

    // ===== FormField =====
    @Test
    void formFieldDefaultConstructor() {
        com.looksee.models.form.FormField field = new com.looksee.models.form.FormField();
        assertNotNull(field);
    }
}
