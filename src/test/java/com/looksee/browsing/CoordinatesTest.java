package com.looksee.browsing;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.Test;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.Point;
import org.openqa.selenium.WebElement;

public class CoordinatesTest {

    @Test
    public void testConstructorFromWebElement() {
        WebElement element = mock(WebElement.class);
        when(element.getLocation()).thenReturn(new Point(10, 20));
        when(element.getSize()).thenReturn(new Dimension(100, 200));

        Coordinates coords = new Coordinates(element, 1.0);
        assertEquals(100, coords.getWidth());
        assertEquals(200, coords.getHeight());
        assertEquals(10, coords.getX());
        assertEquals(20, coords.getY());
    }

    @Test
    public void testConstructorWithDevicePixelRatio() {
        WebElement element = mock(WebElement.class);
        when(element.getLocation()).thenReturn(new Point(10, 20));
        when(element.getSize()).thenReturn(new Dimension(100, 200));

        Coordinates coords = new Coordinates(element, 2.0);
        assertEquals(200, coords.getWidth());
        assertEquals(400, coords.getHeight());
        assertEquals(20, coords.getX());
        assertEquals(40, coords.getY());
    }

    @Test
    public void testConstructorFromPointAndDimension() {
        Point point = new Point(50, 75);
        Dimension size = new Dimension(300, 400);

        Coordinates coords = new Coordinates(point, size, 1.0);
        assertEquals(300, coords.getWidth());
        assertEquals(400, coords.getHeight());
        assertEquals(50, coords.getX());
        assertEquals(75, coords.getY());
    }

    @Test
    public void testConstructorFromPointAndDimensionWithRatio() {
        Point point = new Point(50, 75);
        Dimension size = new Dimension(300, 400);

        Coordinates coords = new Coordinates(point, size, 1.5);
        assertEquals(450, coords.getWidth());
        assertEquals(600, coords.getHeight());
        assertEquals(75, coords.getX());
        assertEquals(112, coords.getY());
    }

    @Test
    public void testZeroCoordinates() {
        Point point = new Point(0, 0);
        Dimension size = new Dimension(0, 0);

        Coordinates coords = new Coordinates(point, size, 1.0);
        assertEquals(0, coords.getWidth());
        assertEquals(0, coords.getHeight());
        assertEquals(0, coords.getX());
        assertEquals(0, coords.getY());
    }
}
