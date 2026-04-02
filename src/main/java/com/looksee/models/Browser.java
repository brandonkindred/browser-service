package com.looksee.models;

import com.assertthat.selenium_shutterbug.core.Capture;
import com.assertthat.selenium_shutterbug.core.Shutterbug;
import com.looksee.browsing.BrowserFactory;
import com.looksee.utils.HtmlUtils;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.MalformedURLException;
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
import org.openqa.selenium.Alert;
import org.openqa.selenium.By;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.NoAlertPresentException;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.Point;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.retry.annotation.Retryable;
import ru.yandex.qatools.ashot.AShot;
import ru.yandex.qatools.ashot.shooting.ShootingStrategies;

/**
 * Manages a Selenium browser session and provides methods for interacting with the browser.
 * Handles navigation, scrolling, screenshots, element interaction, and DOM manipulation.
 *
 * <p>For static utility operations, see:
 * <ul>
 *   <li>{@link HtmlUtils} — HTML parsing and cleaning</li>
 *   <li>{@link com.looksee.utils.CssUtils} — CSS property extraction</li>
 *   <li>{@link com.looksee.utils.ScreenshotUtils} — element screenshot extraction from images</li>
 *   <li>{@link com.looksee.utils.ElementUtils} — label finding, coordinate calculations</li>
 *   <li>{@link com.looksee.utils.NetworkUtils} — URL reading with SSL/GZIP support</li>
 *   <li>{@link BrowserFactory} — WebDriver and Browser creation</li>
 * </ul>
 *
 * <p><b>Class Invariants:</b>
 * <ul>
 *   <li>invariant: browserName is not null after parameterized construction</li>
 *   <li>invariant: driver is not null after parameterized construction</li>
 *   <li>invariant: viewportSize is not null after parameterized construction</li>
 *   <li>invariant: yScrollOffset >= 0</li>
 *   <li>invariant: xScrollOffset >= 0</li>
 * </ul>
 */
@NoArgsConstructor
@Getter
@Setter
public class Browser {

	private static Logger log = LoggerFactory.getLogger(Browser.class);
	private WebDriver driver = null;
	private String browserName;
	private long yScrollOffset;
	private long xScrollOffset;
	private Dimension viewportSize;
	private static final String JS_GET_VIEWPORT_WIDTH = "var width = undefined; if (window.innerWidth) {width = window.innerWidth;} else if (document.documentElement && document.documentElement.clientWidth) {width = document.documentElement.clientWidth;} else { var b = document.getElementsByTagName('body')[0]; if (b.clientWidth) {width = b.clientWidth;}};return width;";
	private static final String JS_GET_VIEWPORT_HEIGHT = "var height = undefined;  if (window.innerHeight) {height = window.innerHeight;}  else if (document.documentElement && document.documentElement.clientHeight) {height = document.documentElement.clientHeight;}  else { var b = document.getElementsByTagName('body')[0]; if (b.clientHeight) {height = b.clientHeight;}};return height;";

	/**
	 * Constructor for {@link Browser} that dispatches to {@link BrowserFactory}
	 * for driver creation.
	 *
	 * @param browser the name of the browser to use (chrome, firefox)
	 * @param hub_node_url the url of the selenium hub node
	 *
	 * @throws MalformedURLException if the url is malformed
	 *
	 * precondition: hub_node_url != null
	 * precondition: browser != null
	 */
	public Browser(String browser, URL hub_node_url) throws MalformedURLException {
		assert browser != null;
		assert hub_node_url != null;

		this.setBrowserName(browser);
		this.driver = BrowserFactory.createDriver(browser, hub_node_url);

		setYScrollOffset(0);
		setXScrollOffset(0);
		setViewportSize(getViewportSize(driver));
	}

	/**
	 * Constructor for {@link Browser} that accepts a pre-built WebDriver.
	 *
	 * @param driver the WebDriver instance
	 * @param browserName the name of the browser
	 *
	 * precondition: driver != null
	 * precondition: browserName != null
	 */
	public Browser(WebDriver driver, String browserName) {
		assert driver != null;
		assert browserName != null;

		this.driver = driver;
		this.setBrowserName(browserName);
		setYScrollOffset(0);
		setXScrollOffset(0);
		setViewportSize(getViewportSize(driver));
	}

	/**
	 * Gets the current {@link WebDriver driver}
	 *
	 * @return the current {@link WebDriver driver}
	 *
	 * precondition: driver != null
	 */
	public WebDriver getDriver() {
		return this.driver;
	}

	/**
	 * Navigates to a given url and waits for the readyState to be complete
	 *
	 * @param url the {@link URL}
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
	 * Closes the browser opened by the current driver.
	 */
	public void close() {
		try {
			driver.quit();
		} catch (Exception e) {
			log.debug("Unknown exception occurred when closing browser" + e.getMessage());
		}
	}

	// ==================== Screenshots ====================

	/**
	 * Takes a viewport-only screenshot.
	 *
	 * @return BufferedImage of the viewport
	 * @throws IOException if an error occurs while getting the screenshot
	 *
	 * precondition: driver != null
	 */
	public BufferedImage getViewportScreenshot() throws IOException {
		return ImageIO.read(((TakesScreenshot) driver).getScreenshotAs(OutputType.FILE));
	}

	/**
	 * Takes a full-page screenshot using Shutterbug (basic scroll capture).
	 *
	 * @return BufferedImage of the full page
	 * @throws IOException if an error occurs while getting the screenshot
	 */
	public BufferedImage getFullPageScreenshot() throws IOException {
		return Shutterbug.shootPage(driver, Capture.FULL_SCROLL).getImage();
	}

	/**
	 * Takes a full-page screenshot using AShot with viewport pasting strategy.
	 *
	 * @return the full page screenshot
	 * @throws IOException if an error occurs while getting the screenshot
	 */
	public BufferedImage getFullPageScreenshotAshot() throws IOException {
		ru.yandex.qatools.ashot.Screenshot screenshot = new AShot().shootingStrategy(ShootingStrategies.viewportPasting(1000)).takeScreenshot(driver);
		return screenshot.getImage();
	}

	/**
	 * Takes a full-page screenshot using Shutterbug with scroll pause.
	 * Works best in Chrome.
	 *
	 * @return the full page screenshot
	 * @throws IOException if an error occurs while getting the screenshot
	 */
	@Retryable
	public BufferedImage getFullPageScreenshotShutterbug() throws IOException {
		return Shutterbug.shootPage(driver, Capture.FULL, 1000, true).getImage();
	}

	/**
	 * Takes a screenshot of a specific WebElement.
	 *
	 * @param element the element to get a screenshot of
	 * @return the screenshot
	 * @throws Exception if an error occurs while getting the screenshot
	 *
	 * precondition: element != null
	 */
	public BufferedImage getElementScreenshot(WebElement element) throws Exception {
		assert element != null;
		return Shutterbug.shootElementVerticallyCentered(driver, element).getImage();
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
	 * @param element the element to check
	 * @return {@code true} if the element is displayed, {@code false} otherwise
	 *
	 * precondition: element != null
	 */
	public boolean isDisplayed(ElementState element) {
		assert element != null;
		WebElement web_element = driver.findElement(By.xpath(element.getXpath()));
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
	 * Removes element from browser DOM by class name.
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

	/**
	 * Remove Drift.com chat app widget from the DOM.
	 */
	public void removeDriftChat() {
		((JavascriptExecutor) driver).executeScript("var element=document.getElementById(\"drift-frame-chat\");if(typeof(element)!='undefined' && element != null){document.getElementById(\"drift-frame-chat\").remove();document.getElementById(\"drift-frame-controller\").remove();}");
	}

	/**
	 * Remove GDPR modal by id "gdprModal".
	 */
	public void removeGDPRmodals() {
		((JavascriptExecutor) driver).executeScript("var element=document.getElementById(\"gdprModal\");if(typeof(element)!='undefined' && element != null){element.remove();}	");
	}

	/**
	 * Remove GDPR element by id "gdpr".
	 */
	public void removeGDPR() {
		((JavascriptExecutor) driver).executeScript("var element=document.getElementById(\"gdpr\");if(typeof(element)!='undefined' && element != null){element.remove();} ");
	}

	// ==================== Scrolling ====================

	/**
	 * Scrolls to an element using xpath navigation hints and offset tracking.
	 *
	 * @param xpath the xpath of the element to scroll to
	 * @param elem the element to scroll to
	 *
	 * precondition: xpath != null
	 * precondition: elem != null
	 */
	public void scrollToElement(String xpath, WebElement elem) {
		assert xpath != null;
		assert elem != null;

		if (xpath.contains("nav") || xpath.startsWith("//body/header")) {
			scrollToTopOfPage();
			return;
		}

		Point element_offset = elem.getLocation();
		while (this.getYScrollOffset() != element_offset.getY()) {
			scrollDownFull();
		}

		getViewportScrollOffset();
	}

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
	 * Scrolls to an element centered in the viewport.
	 *
	 * @param element the element to scroll to
	 *
	 * precondition: element != null
	 */
	public void scrollToElementCentered(WebElement element) {
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

	// ==================== Mouse & Alerts ====================

	/**
	 * Moves the mouse out of the frame to a non-interactive position.
	 */
	public void moveMouseOutOfFrame() {
		try {
			Actions mouseMoveAction = new Actions(driver).moveByOffset(-(getViewportSize().getWidth() / 3), -(getViewportSize().getHeight() / 3));
			mouseMoveAction.build().perform();
		} catch (Exception e) {
		}
	}

	/**
	 * Moves the mouse to a specific point.
	 *
	 * @param point the point to move the mouse to
	 *
	 * precondition: point != null
	 */
	public void moveMouseToNonInteractive(Point point) {
		assert point != null;
		try {
			Actions mouseMoveAction = new Actions(driver).moveByOffset(point.getX(), point.getY());
			mouseMoveAction.build().perform();
		} catch (Exception e) {
		}
	}

	/**
	 * Checks if an alert is present.
	 *
	 * @return {@link Alert} if present, otherwise {@code null}
	 */
	public Alert isAlertPresent() {
		try {
			return driver.switchTo().alert();
		} catch (NoAlertPresentException Ex) {
			return null;
		}
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
	private static Dimension getViewportSize(WebDriver driver) {
		int width = extractViewportWidth(driver);
		int height = extractViewportHeight(driver);
		return new Dimension(width, height);
	}

	/**
	 * Extracts the page Y scroll offset.
	 *
	 * @param driver the driver
	 * @return the Y offset
	 */
	private static long extractYOffset(WebDriver driver) {
		JavascriptExecutor executor = (JavascriptExecutor) driver;
		return (Long) executor.executeScript("return window.pageYOffset;");
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
