package com.looksee.browsing;

import com.looksee.browsing.enums.MobileAction;
import io.appium.java_client.TouchAction;
import io.appium.java_client.touch.WaitOptions;
import io.appium.java_client.touch.offset.ElementOption;
import io.appium.java_client.touch.offset.PointOption;
import io.appium.java_client.PerformsTouchActions;
import java.time.Duration;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.WebElement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Constructs and executes touch actions for Appium mobile drivers.
 * This is the mobile counterpart of {@link ActionFactory}.
 */
public class MobileActionFactory {

	private static Logger log = LoggerFactory.getLogger(MobileActionFactory.class);

	private final PerformsTouchActions driver;

	/**
	 * Creates a new mobile action factory.
	 *
	 * @param driver the Appium driver (must implement PerformsTouchActions)
	 *
	 * precondition: driver != null
	 */
	public MobileActionFactory(WebDriver driver) {
		assert driver != null;

		if (!(driver instanceof PerformsTouchActions)) {
			throw new IllegalArgumentException("Driver must implement PerformsTouchActions for mobile actions");
		}
		this.driver = (PerformsTouchActions) driver;
	}

	/**
	 * Executes a mobile touch action on an element.
	 *
	 * @param elem the element to perform the action on
	 * @param input the input to send (used with SEND_KEYS)
	 * @param action the mobile action to perform
	 * @throws WebDriverException if the action cannot be performed
	 *
	 * precondition: elem != null
	 * precondition: input != null
	 * precondition: action != null
	 */
	@SuppressWarnings("rawtypes")
	public void execAction(WebElement elem, String input, MobileAction action) throws WebDriverException {
		assert elem != null;
		assert input != null;
		assert action != null;

		switch (action) {
			case TAP:
				new TouchAction(driver)
						.tap(ElementOption.element(elem))
						.perform();
				break;

			case DOUBLE_TAP:
				new TouchAction(driver)
						.tap(ElementOption.element(elem))
						.perform();
				new TouchAction(driver)
						.tap(ElementOption.element(elem))
						.perform();
				break;

			case LONG_PRESS:
				new TouchAction(driver)
						.longPress(ElementOption.element(elem))
						.release()
						.perform();
				break;

			case SEND_KEYS:
				elem.sendKeys(input);
				break;

			case SWIPE_UP:
				swipeFromElement(elem, SwipeDirection.UP);
				break;

			case SWIPE_DOWN:
				swipeFromElement(elem, SwipeDirection.DOWN);
				break;

			case SWIPE_LEFT:
				swipeFromElement(elem, SwipeDirection.LEFT);
				break;

			case SWIPE_RIGHT:
				swipeFromElement(elem, SwipeDirection.RIGHT);
				break;

			case SCROLL_UP:
				swipeScreen(SwipeDirection.UP);
				break;

			case SCROLL_DOWN:
				swipeScreen(SwipeDirection.DOWN);
				break;

			default:
				log.warn("Unsupported mobile action: {}", action);
				break;
		}
	}

	/**
	 * Performs a tap action on an element.
	 *
	 * @param elem the element to tap
	 *
	 * precondition: elem != null
	 */
	@SuppressWarnings("rawtypes")
	public void tap(WebElement elem) {
		assert elem != null;
		new TouchAction(driver)
				.tap(ElementOption.element(elem))
				.perform();
	}

	/**
	 * Performs a long press action on an element.
	 *
	 * @param elem the element to long press
	 *
	 * precondition: elem != null
	 */
	@SuppressWarnings("rawtypes")
	public void longPress(WebElement elem) {
		assert elem != null;
		new TouchAction(driver)
				.longPress(ElementOption.element(elem))
				.release()
				.perform();
	}

	/**
	 * Performs a swipe gesture on the screen.
	 *
	 * @param direction the direction to swipe
	 */
	@SuppressWarnings("rawtypes")
	public void swipeScreen(SwipeDirection direction) {
		Dimension size = ((WebDriver) driver).manage().window().getSize();
		int centerX = size.width / 2;
		int centerY = size.height / 2;

		int startX, startY, endX, endY;

		switch (direction) {
			case UP:
				startX = centerX;
				startY = (int) (size.height * 0.7);
				endX = centerX;
				endY = (int) (size.height * 0.3);
				break;
			case DOWN:
				startX = centerX;
				startY = (int) (size.height * 0.3);
				endX = centerX;
				endY = (int) (size.height * 0.7);
				break;
			case LEFT:
				startX = (int) (size.width * 0.8);
				startY = centerY;
				endX = (int) (size.width * 0.2);
				endY = centerY;
				break;
			case RIGHT:
				startX = (int) (size.width * 0.2);
				startY = centerY;
				endX = (int) (size.width * 0.8);
				endY = centerY;
				break;
			default:
				return;
		}

		new TouchAction(driver)
				.press(PointOption.point(startX, startY))
				.waitAction(WaitOptions.waitOptions(Duration.ofMillis(500)))
				.moveTo(PointOption.point(endX, endY))
				.release()
				.perform();
	}

	/**
	 * Performs a swipe gesture starting from an element.
	 *
	 * @param elem the element to start the swipe from
	 * @param direction the direction to swipe
	 *
	 * precondition: elem != null
	 */
	@SuppressWarnings("rawtypes")
	public void swipeFromElement(WebElement elem, SwipeDirection direction) {
		assert elem != null;

		int offsetX = 0;
		int offsetY = 0;
		int swipeDistance = 300;

		switch (direction) {
			case UP:    offsetY = -swipeDistance; break;
			case DOWN:  offsetY = swipeDistance;  break;
			case LEFT:  offsetX = -swipeDistance; break;
			case RIGHT: offsetX = swipeDistance;  break;
		}

		new TouchAction(driver)
				.press(ElementOption.element(elem))
				.waitAction(WaitOptions.waitOptions(Duration.ofMillis(500)))
				.moveTo(PointOption.point(offsetX, offsetY))
				.release()
				.perform();
	}

	/**
	 * Direction for swipe gestures.
	 */
	public enum SwipeDirection {
		UP, DOWN, LEFT, RIGHT
	}
}
