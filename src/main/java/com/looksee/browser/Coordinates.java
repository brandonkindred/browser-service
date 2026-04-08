package com.looksee.browser;

import lombok.Getter;
import lombok.Setter;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.Point;
import org.openqa.selenium.WebElement;

/**
 * Represents the coordinates of an element
 */
@Getter
@Setter
public class Coordinates {

    private final int width;
    private final int height;
    private final int x;
    private final int y;

    /**
     * Constructs a {@link Coordinates} object
     *
     * @param element the element to get the coordinates from
     * @param devicePixelRatio the device pixel ratio
     *
     * precondition: element != null
     * precondition: devicePixelRatio != null
     */
    public Coordinates(WebElement element, Double devicePixelRatio) {
        assert element != null;
        assert devicePixelRatio != null;
        Point point = element.getLocation();
        Dimension size = element.getSize();
        this.width = (int)(size.getWidth()*devicePixelRatio);
        this.height = (int)(size.getHeight()*devicePixelRatio);
        this.x = (int)(point.getX()*devicePixelRatio);
        this.y = (int)(point.getY()*devicePixelRatio);
    }

    /**
     * Constructs a {@link Coordinates} object
     *
     * @param point the point of the coordinates
     * @param size the size of the coordinates
     * @param devicePixelRatio the device pixel ratio
     *
     * precondition: point != null
     * precondition: size != null
     * precondition: devicePixelRatio != null
     */
    public Coordinates(Point point, Dimension size, Double devicePixelRatio) {
        assert point != null;
        assert size != null;
        assert devicePixelRatio != null;
        this.width = (int)(size.getWidth()*devicePixelRatio);
        this.height = (int)(size.getHeight()*devicePixelRatio);
        this.x = (int)(point.getX()*devicePixelRatio);
        this.y = (int)(point.getY()*devicePixelRatio);
    }

}
