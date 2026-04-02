package com.looksee.utils;

import com.looksee.models.ElementState;
import java.awt.image.BufferedImage;
import java.io.IOException;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.Point;

/**
 * Utility class for extracting element screenshots from full-page images.
 */
public final class ScreenshotUtils {

	private ScreenshotUtils() {
		// Utility class — prevent instantiation
	}

	/**
	 * Extracts a screenshot of an element from a full-page screenshot image.
	 *
	 * @param element_state the element to get a screenshot of
	 * @param page_screenshot the full-page screenshot to extract from
	 * @return the cropped screenshot of the element
	 * @throws IOException if an error occurs while getting the screenshot
	 *
	 * precondition: element_state != null
	 * precondition: page_screenshot != null
	 */
	public static BufferedImage getElementScreenshot(ElementState element_state,
													 BufferedImage page_screenshot) throws IOException {
		assert element_state != null;
		assert page_screenshot != null;

		int width = element_state.getWidth();
		int height = element_state.getHeight();

		if ((element_state.getXLocation() + element_state.getWidth()) > page_screenshot.getWidth()) {
			width = page_screenshot.getWidth() - element_state.getXLocation() - 1;
		}

		if ((element_state.getYLocation() + element_state.getHeight()) > page_screenshot.getHeight()) {
			height = page_screenshot.getHeight() - element_state.getYLocation() - 1;
		}

		return page_screenshot.getSubimage(element_state.getXLocation(), element_state.getYLocation(), width, height);
	}

	/**
	 * Extracts a screenshot of an element from a full-page screenshot using Point and Dimension.
	 *
	 * @param element_location the location of the element
	 * @param element_size the size of the element
	 * @param page_screenshot the full-page screenshot to extract from
	 * @return the cropped screenshot of the element
	 * @throws IOException if an error occurs while getting the screenshot
	 *
	 * precondition: element_location != null
	 * precondition: element_size != null
	 * precondition: page_screenshot != null
	 */
	public static BufferedImage getElementScreenshot(Point element_location,
													 Dimension element_size,
													 BufferedImage page_screenshot) throws IOException {
		assert element_location != null;
		assert element_size != null;
		assert page_screenshot != null;

		int width = element_size.getWidth();
		int height = element_size.getHeight();

		if ((element_location.getX() + element_size.getWidth()) > page_screenshot.getWidth()) {
			width = page_screenshot.getWidth() - element_location.getX() - 1;
		}

		if ((element_location.getY() + element_size.getHeight()) > page_screenshot.getHeight()) {
			height = page_screenshot.getHeight() - element_location.getY() - 1;
		}

		return page_screenshot.getSubimage(element_location.getX(), element_location.getY(), width, height);
	}
}
