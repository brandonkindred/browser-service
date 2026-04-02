package com.looksee.utils;

import com.looksee.models.ElementState;
import java.util.HashSet;
import java.util.Set;
import org.openqa.selenium.Alert;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.NoAlertPresentException;
import org.openqa.selenium.Point;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedCondition;
import org.openqa.selenium.support.ui.WebDriverWait;

/**
 * Utility class for element-related operations such as label finding, outlining,
 * alert handling, and coordinate calculations.
 */
public final class ElementUtils {

	private ElementUtils() {
		// Utility class — prevent instantiation
	}

	/**
	 * Finds a label element matching a given for-attribute id.
	 *
	 * @param elements the elements to search
	 * @param for_id the id to search for
	 * @return the matching label, or null if not found
	 *
	 * precondition: elements != null
	 * precondition: for_id != null
	 */
	public static ElementState findLabelFor(Set<ElementState> elements, String for_id) {
		assert elements != null;
		assert for_id != null;

		for (ElementState elem : elements) {
			if (elem.getName().equals("label")) {
				if (elem.getAttribute("for").contains(for_id)) {
					return elem;
				}
			}
		}

		return null;
	}

	/**
	 * Finds label elements matching the given for-attribute ids.
	 *
	 * @param elements the elements to search
	 * @param for_ids the ids to search for
	 * @return the matching labels
	 *
	 * precondition: elements != null
	 * precondition: for_ids != null
	 */
	public static Set<ElementState> findLabelsFor(Set<ElementState> elements, String[] for_ids) {
		assert elements != null;
		assert for_ids != null;

		Set<ElementState> labels = new HashSet<ElementState>();
		for (ElementState elem : elements) {
			if (elem.getName().equals("label")) {
				for (String id : for_ids) {
					if (elem.getAttributes().get("for").contains(id)) {
						labels.add(elem);
					}
				}
			}
		}

		return labels;
	}

	/**
	 * Outlines an element with a yellow border via JavaScript.
	 *
	 * @param page_element the element to outline
	 * @param driver the driver to use
	 *
	 * precondition: page_element != null
	 * precondition: driver != null
	 */
	public static void outlineElement(ElementState page_element, WebDriver driver) {
		assert page_element != null;
		assert driver != null;

		WebElement element = driver.findElement(By.xpath(page_element.getXpath()));
		((JavascriptExecutor) driver).executeScript("arguments[0].style.border='2px solid yellow'", element);
	}

	/**
	 * Waits for and accepts a JavaScript alert.
	 *
	 * @param driver the driver to use
	 * @param wait the wait to use
	 *
	 * precondition: driver != null
	 */
	public static void acceptAlert(WebDriver driver, WebDriverWait wait) {
		assert driver != null;

		if (wait == null) {
			wait = new WebDriverWait(driver, 5);
		}
		try {
			Alert alert = wait.until(new ExpectedCondition<Alert>() {
				public Alert apply(WebDriver driver) {
					try {
						return driver.switchTo().alert();
					} catch (NoAlertPresentException e) {
						return null;
					}
				}
			});
			alert.accept();
		} catch (TimeoutException e) {
		}
	}

	/**
	 * Calculates element position relative to the current viewport.
	 *
	 * @param element the element
	 * @param x_offset the x scroll offset
	 * @param y_offset the y scroll offset
	 * @return the viewport-relative coordinates
	 *
	 * precondition: element != null
	 */
	public static Point getLocationInViewport(ElementState element, int x_offset, int y_offset) {
		assert element != null;

		int y_coord = element.getYLocation() - y_offset;
		int x_coord = element.getXLocation() - x_offset;

		return new Point(x_coord, y_coord);
	}

	/**
	 * Calculates the adjusted Y coordinate relative to scroll offset.
	 *
	 * @param y_offset the y offset
	 * @param location the location
	 * @return the y coordinate
	 */
	public static int calculateYCoordinate(int y_offset, Point location) {
		if ((location.getY() - y_offset) >= 0) {
			return location.getY() - y_offset;
		}
		return y_offset;
	}

	/**
	 * Calculates the adjusted X coordinate relative to scroll offset.
	 *
	 * @param x_offset the x offset
	 * @param location the location
	 * @return the x coordinate
	 */
	public static int calculateXCoordinate(int x_offset, Point location) {
		if ((location.getX() - x_offset) >= 0) {
			return location.getX() - x_offset;
		}
		return x_offset;
	}
}
