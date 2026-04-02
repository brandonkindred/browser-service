package com.looksee.models;

import com.looksee.browsing.MobileFactory;
import com.looksee.utils.HtmlUtils;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.imageio.ImageIO;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.openqa.selenium.By;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.Point;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Manages an Appium mobile device session and provides methods for interacting
 * with mobile browsers. This is the mobile counterpart of {@link Browser},
 * allowing Appium to evolve independently from Selenium.
 *
 * <p>Uses native Appium/WebDriver screenshot capabilities instead of
 * Shutterbug/AShot (which are desktop-only). Does not support mouse actions
 * since mobile devices use touch interactions.
 *
 * <p><b>Class Invariants:</b>
 * <ul>
 *   <li>invariant: platformName is not null after parameterized construction</li>
 *   <li>invariant: driver is not null after parameterized construction</li>
 *   <li>invariant: viewportSize is not null after parameterized construction</li>
 *   <li>invariant: yScrollOffset >= 0</li>
 *   <li>invariant: xScrollOffset >= 0</li>
 * </ul>
 */
@NoArgsConstructor
@Getter
@Setter
public class MobileDevice {

	private static Logger log = LoggerFactory.getLogger(MobileDevice.class);
	private WebDriver driver = null;
	private String platformName;
	private long yScrollOffset;
	private long xScrollOffset;
	private Dimension viewportSize;
	private static final String JS_GET_VIEWPORT_WIDTH = "var width = undefined; if (window.innerWidth) {width = window.innerWidth;} else if (document.documentElement && document.documentElement.clientWidth) {width = document.documentElement.clientWidth;} else { var b = document.getElementsByTagName('body')[0]; if (b.clientWidth) {width = b.clientWidth;}};return width;";
	private static final String JS_GET_VIEWPORT_HEIGHT = "var height = undefined;  if (window.innerHeight) {height = window.innerHeight;}  else if (document.documentElement && document.documentElement.clientHeight) {height = document.documentElement.clientHeight;}  else { var b = document.getElementsByTagName('body')[0]; if (b.clientHeight) {height = b.clientHeight;}};return height;";

	/**
	 * Constructor for {@link MobileDevice} that dispatches to {@link MobileFactory}
	 * for driver creation.
	 *
	 * @param platformType the mobile platform ("android", "ios")
	 * @param serverUrl the URL of the Appium server
	 *
	 * precondition: platformType != null
	 * precondition: serverUrl != null
	 */
	public MobileDevice(String platformType, URL serverUrl) {
		assert platformType != null;
		assert serverUrl != null;

		this.setPlatformName(platformType);
		this.driver = MobileFactory.createDriver(platformType, serverUrl);

		setYScrollOffset(0);
		setXScrollOffset(0);
		setViewportSize(getViewportDimensions(driver));
	}

	/**
	 * Constructor for {@link MobileDevice} that accepts a pre-built WebDriver.
	 *
	 * @param driver the WebDriver instance (AndroidDriver or IOSDriver)
	 * @param platformName the platform name ("android", "ios")
	 *
	 * precondition: driver != null
	 * precondition: platformName != null
	 */
	public MobileDevice(WebDriver driver, String platformName) {
		assert driver != null;
		assert platformName != null;

		this.driver = driver;
		this.setPlatformName(platformName);
		setYScrollOffset(0);
		setXScrollOffset(0);
		setViewportSize(getViewportDimensions(driver));
	}

	/**
	 * Gets the current {@link WebDriver driver}
	 *
	 * @return the current {@link WebDriver driver}
	 */
	public WebDriver getDriver() {
		return this.driver;
	}

	/**
	 * Navigates to a given url and waits for the readyState to be complete
	 *
	 * @param url the URL to navigate to
	 *
	 * precondition: url != null
	 */
	public void navigateTo(String url) {
		assert url != null;

		getDriver().get(url);

		try {
			waitForPageToLoad();
		} catch (Exception e) {
		}
	}

	/**
	 * Closes the mobile session.
	 */
	public void close() {
		try {
			driver.quit();
		} catch (Exception e) {
			log.debug("Exception occurred when closing mobile session: " + e.getMessage());
		}
	}

	// ==================== Screenshots ====================

	/**
	 * Takes a viewport screenshot using the native Appium screenshot capability.
	 *
	 * @return BufferedImage of the viewport
	 * @throws IOException if an error occurs while getting the screenshot
	 */
	public BufferedImage getViewportScreenshot() throws IOException {
		return ImageIO.read(((TakesScreenshot) driver).getScreenshotAs(OutputType.FILE));
	}

	/**
	 * Takes a full-page screenshot. On mobile, this falls back to a viewport
	 * screenshot since Shutterbug/AShot are not compatible with Appium drivers.
	 *
	 * @return BufferedImage of the viewport
	 * @throws IOException if an error occurs while getting the screenshot
	 */
	public BufferedImage getFullPageScreenshot() throws IOException {
		return getViewportScreenshot();
	}

	/**
	 * Takes a screenshot of a specific WebElement using Appium's native
	 * element screenshot capability.
	 *
	 * @param element the element to capture
	 * @return the screenshot
	 * @throws IOException if an error occurs while getting the screenshot
	 *
	 * precondition: element != null
	 */
	public BufferedImage getElementScreenshot(WebElement element) throws IOException {
		assert element != null;
		return ImageIO.read(element.getScreenshotAs(OutputType.FILE));
	}

	// ==================== Element Finding ====================

	/**
	 * Finds page element by xpath.
	 *
	 * @param xpath the xpath to find the element at
	 * @return {@link WebElement} located at the provided xpath
	 *
	 * precondition: xpath != null
	 * precondition: !xpath.isEmpty()
	 */
	public WebElement findWebElementByXpath(String xpath) {
		assert xpath != null;
		assert !xpath.isEmpty();

		return driver.findElement(By.xpath(xpath));
	}

	/**
	 * Finds an element by xpath.
	 *
	 * @param xpath the xpath to find the element at
	 * @return the element
	 *
	 * precondition: xpath != null
	 * precondition: !xpath.isEmpty()
	 */
	public WebElement findElement(String xpath) throws WebDriverException {
		assert xpath != null;
		assert !xpath.isEmpty();
		return getDriver().findElement(By.xpath(xpath));
	}

	/**
	 * Checks if an element is displayed.
	 *
	 * @param xpath the XPath of the element to check
	 * @return {@code true} if the element is displayed, {@code false} otherwise
	 *
	 * precondition: xpath != null
	 * precondition: !xpath.isEmpty()
	 */
	public boolean isDisplayed(String xpath) {
		assert xpath != null;
		assert !xpath.isEmpty();
		WebElement web_element = driver.findElement(By.xpath(xpath));
		return web_element.isDisplayed();
	}

	// ==================== Attribute Extraction ====================

	/**
	 * Extracts all attributes from a given {@link WebElement}.
	 *
	 * @param element {@link WebElement} to have attributes loaded for
	 * @return the attributes
	 *
	 * precondition: element != null
	 */
	@SuppressWarnings("unchecked")
	public Map<String, String> extractAttributes(WebElement element) {
		assert element != null;
		List<String> attribute_strings = (ArrayList<String>) ((JavascriptExecutor) driver).executeScript("var items = []; for (index = 0; index < arguments[0].attributes.length; ++index) { items.push(arguments[0].attributes[index].name + '::' + arguments[0].attributes[index].value) }; return items;", element);
		return loadAttributes(attribute_strings);
	}

	/**
	 * Loads attributes for this element into a map.
	 *
	 * @param attributeList the list of attributes to load
	 * @return the attributes
	 */
	private Map<String, String> loadAttributes(List<String> attributeList) {
		Map<String, String> attributes_seen = new HashMap<String, String>();
		for (int i = 0; i < attributeList.size(); i++) {
			String[] attributes = attributeList.get(i).split("::");

			if (attributes.length > 1) {
				String attribute_name = attributes[0].trim().replace("\'", "'");
				String[] attributeVals = attributes[1].split(" ");

				if (!attributes_seen.containsKey(attribute_name)) {
					attributes_seen.put(attribute_name, Arrays.asList(attributeVals).toString());
				}
			}
		}

		return attributes_seen;
	}

	// ==================== DOM Manipulation ====================

	/**
	 * Removes element from DOM by class name.
	 *
	 * @param class_name the class name of the element to remove
	 *
	 * precondition: class_name != null
	 */
	public void removeElement(String class_name) {
		assert class_name != null;

		JavascriptExecutor js;
		if (this.getDriver() instanceof JavascriptExecutor) {
			js = (JavascriptExecutor) driver;
			js.executeScript("return document.getElementsByClassName('" + class_name + "')[0].remove();");
		}
	}

	// ==================== Scrolling ====================

	/**
	 * Scrolls to center an element in the viewport.
	 *
	 * @param element the element to scroll to
	 *
	 * precondition: element != null
	 */
	public void scrollToElement(WebElement element) {
		assert element != null;

		((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView({block: 'center'});", element);
		getViewportScrollOffset();
	}

	/**
	 * Scrolls to the bottom of the page.
	 */
	public void scrollToBottomOfPage() {
		((JavascriptExecutor) driver)
				.executeScript("window.scrollTo(0, document.body.scrollHeight)");
		getViewportScrollOffset();
	}

	/**
	 * Scrolls to position (0,0).
	 */
	public void scrollToTopOfPage() {
		((JavascriptExecutor) driver)
				.executeScript("window.scrollTo(0, 0)");
		getViewportScrollOffset();
	}

	/**
	 * Scrolls down a percentage of the viewport height.
	 *
	 * @param percent the percentage to scroll down
	 */
	public void scrollDownPercent(double percent) {
		((JavascriptExecutor) driver)
				.executeScript("window.scrollBy(0, (window.innerHeight*" + percent + "))");
		getViewportScrollOffset();
	}

	/**
	 * Scrolls down the full viewport height.
	 */
	public void scrollDownFull() {
		((JavascriptExecutor) driver)
				.executeScript("window.scrollBy(0, window.innerHeight)");
		getViewportScrollOffset();
	}

	// ==================== Viewport State ====================

	/**
	 * Retrieves the x and y scroll offset of the viewport.
	 *
	 * @return {@link Point} containing offsets
	 */
	public Point getViewportScrollOffset() {
		int x_offset = 0;
		int y_offset = 0;

		Object offset_obj = ((JavascriptExecutor) driver).executeScript("return window.pageXOffset+','+window.pageYOffset;");
		if (offset_obj instanceof String) {
			String offset_str = (String) offset_obj;
			String[] coord = offset_str.split(",");
			x_offset = Integer.parseInt(coord[0]);
			y_offset = Integer.parseInt(coord[1]);
		}

		this.setXScrollOffset(x_offset);
		this.setYScrollOffset(y_offset);

		return new Point(x_offset, y_offset);
	}

	/**
	 * Waits for the document ready state to be complete.
	 */
	public void waitForPageToLoad() {
		new WebDriverWait(driver, 30L).until(
				webDriver -> ((JavascriptExecutor) webDriver)
						.executeScript("return document.readyState")
						.equals("complete"));
	}

	// ==================== Page Source & Error Checking ====================

	/**
	 * Retrieves the HTML source from the current page.
	 *
	 * @return HTML source
	 */
	public String getSource() {
		return this.getDriver().getPageSource();
	}

	/**
	 * Checks if the current page is a 503 error.
	 *
	 * @return {@code true} if the page contains a 503 error, {@code false} otherwise
	 */
	public boolean is503Error() {
		return HtmlUtils.is503Error(this.getSource());
	}

	// ==================== Private Helpers ====================

	/**
	 * Gets the viewport size.
	 *
	 * @param driver the driver
	 * @return the viewport size
	 */
	private static Dimension getViewportDimensions(WebDriver driver) {
		int width = extractViewportWidth(driver);
		int height = extractViewportHeight(driver);
		return new Dimension(width, height);
	}

	/**
	 * Extracts the viewport width.
	 *
	 * @param driver the driver
	 * @return the viewport width
	 */
	private static int extractViewportWidth(WebDriver driver) {
		JavascriptExecutor js = (JavascriptExecutor) driver;
		int viewportWidth = Integer.parseInt(js.executeScript(JS_GET_VIEWPORT_WIDTH, new Object[0]).toString());
		return viewportWidth;
	}

	/**
	 * Extracts the viewport height.
	 *
	 * @param driver the driver
	 * @return the viewport height
	 */
	private static int extractViewportHeight(WebDriver driver) {
		JavascriptExecutor js = (JavascriptExecutor) driver;
		int result = Integer.parseInt(js.executeScript(JS_GET_VIEWPORT_HEIGHT, new Object[0]).toString());
		return result;
	}
}
