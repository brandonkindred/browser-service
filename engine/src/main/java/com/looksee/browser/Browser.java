package com.looksee.browser;

import com.assertthat.selenium_shutterbug.core.Capture;
import com.assertthat.selenium_shutterbug.core.Shutterbug;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import javax.imageio.ImageIO;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.openqa.selenium.Alert;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.NoAlertPresentException;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.Point;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;
import ru.yandex.qatools.ashot.AShot;
import ru.yandex.qatools.ashot.shooting.ShootingStrategies;

/**
 * Manages a Selenium browser session and provides desktop-only methods for interacting with the
 * browser (screenshots, mouse, alerts, GDPR/Drift removers). Shared navigation, scrolling, and DOM
 * inspection logic lives on {@link AbstractWebDriverSession}.
 *
 * <p>For static utility operations, see:
 *
 * <ul>
 *   <li>{@link com.looksee.browser.utils.HtmlUtils} — HTML parsing and cleaning
 *   <li>{@link com.looksee.browser.utils.CssUtils} — CSS property extraction
 *   <li>{@link com.looksee.utils.ScreenshotUtils} — element screenshot extraction from images
 *   <li>{@link com.looksee.utils.ElementUtils} — label finding, coordinate calculations
 *   <li>{@link com.looksee.browser.utils.NetworkUtils} — URL reading with SSL/GZIP support
 *   <li>{@link BrowserFactory} — WebDriver and Browser creation
 * </ul>
 *
 * <p><b>Class Invariants:</b>
 *
 * <ul>
 *   <li>invariant: browserName is not null after parameterized construction
 *   <li>invariant: driver is not null after parameterized construction
 *   <li>invariant: viewportSize is not null after parameterized construction
 *   <li>invariant: yScrollOffset >= 0
 *   <li>invariant: xScrollOffset >= 0
 * </ul>
 */
@NoArgsConstructor
@Getter
@Setter
public class Browser extends AbstractWebDriverSession {

  private String browserName;

  /**
   * Constructor for {@link Browser} that dispatches to {@link BrowserFactory} for driver creation.
   *
   * @param browser the name of the browser to use (chrome, firefox)
   * @param hub_node_url the url of the selenium hub node
   * @throws MalformedURLException if the url is malformed
   *     <p>precondition: hub_node_url != null precondition: browser != null
   */
  public Browser(String browser, URL hub_node_url) throws MalformedURLException {
    assert browser != null;
    assert hub_node_url != null;

    this.setBrowserName(browser);
    this.setDriver(BrowserFactory.createDriver(browser, hub_node_url));
    initViewportState();
  }

  /**
   * Constructor for {@link Browser} that accepts a pre-built WebDriver.
   *
   * @param driver the WebDriver instance
   * @param browserName the name of the browser
   *     <p>precondition: driver != null precondition: browserName != null
   */
  public Browser(WebDriver driver, String browserName) {
    super(driver);
    assert browserName != null;
    this.setBrowserName(browserName);
  }

  // ==================== Screenshots ====================

  /**
   * Takes a viewport-only screenshot.
   *
   * @return BufferedImage of the viewport
   * @throws IOException if an error occurs while getting the screenshot
   *     <p>precondition: driver != null
   */
  public BufferedImage getViewportScreenshot() throws IOException {
    return ImageIO.read(((TakesScreenshot) getDriver()).getScreenshotAs(OutputType.FILE));
  }

  /**
   * Takes a full-page screenshot using Shutterbug (basic scroll capture).
   *
   * @return BufferedImage of the full page
   * @throws IOException if an error occurs while getting the screenshot
   */
  public BufferedImage getFullPageScreenshot() throws IOException {
    return Shutterbug.shootPage(getDriver(), Capture.FULL_SCROLL).getImage();
  }

  /**
   * Takes a full-page screenshot using AShot with viewport pasting strategy.
   *
   * @return the full page screenshot
   * @throws IOException if an error occurs while getting the screenshot
   */
  public BufferedImage getFullPageScreenshotAshot() throws IOException {
    ru.yandex.qatools.ashot.Screenshot screenshot =
        new AShot()
            .shootingStrategy(ShootingStrategies.viewportPasting(1000))
            .takeScreenshot(getDriver());
    return screenshot.getImage();
  }

  /**
   * Takes a full-page screenshot using Shutterbug with scroll pause. Works best in Chrome.
   *
   * @return the full page screenshot
   * @throws IOException if an error occurs while getting the screenshot
   */
  public BufferedImage getFullPageScreenshotShutterbug() throws IOException {
    return Shutterbug.shootPage(getDriver(), Capture.FULL, 1000, true).getImage();
  }

  /**
   * Takes a screenshot of a specific WebElement.
   *
   * @param element the element to get a screenshot of
   * @return the screenshot
   * @throws Exception if an error occurs while getting the screenshot
   *     <p>precondition: element != null
   */
  public BufferedImage getElementScreenshot(WebElement element) throws Exception {
    assert element != null;
    return Shutterbug.shootElementVerticallyCentered(getDriver(), element).getImage();
  }

  // ==================== DOM Manipulation ====================

  /** Remove Drift.com chat app widget from the DOM. */
  public void removeDriftChat() {
    ((JavascriptExecutor) getDriver())
        .executeScript(
            "var element=document.getElementById(\"drift-frame-chat\");if(typeof(element)!='undefined' && element != null){document.getElementById(\"drift-frame-chat\").remove();document.getElementById(\"drift-frame-controller\").remove();}");
  }

  /** Remove GDPR modal by id "gdprModal". */
  public void removeGDPRmodals() {
    ((JavascriptExecutor) getDriver())
        .executeScript(
            "var element=document.getElementById(\"gdprModal\");if(typeof(element)!='undefined' && element != null){element.remove();}	");
  }

  /** Remove GDPR element by id "gdpr". */
  public void removeGDPR() {
    ((JavascriptExecutor) getDriver())
        .executeScript(
            "var element=document.getElementById(\"gdpr\");if(typeof(element)!='undefined' && element != null){element.remove();} ");
  }

  // ==================== Scrolling ====================

  /**
   * Scrolls to an element using xpath navigation hints and offset tracking.
   *
   * @param xpath the xpath of the element to scroll to
   * @param elem the element to scroll to
   *     <p>precondition: xpath != null precondition: elem != null
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
   * Scrolls to an element centered in the viewport.
   *
   * @param element the element to scroll to
   *     <p>precondition: element != null
   */
  public void scrollToElementCentered(WebElement element) {
    assert element != null;
    ((JavascriptExecutor) getDriver())
        .executeScript("arguments[0].scrollIntoView({block: 'center'});", element);

    getViewportScrollOffset();
  }

  // ==================== Mouse & Alerts ====================

  /** Moves the mouse out of the frame to a non-interactive position. */
  public void moveMouseOutOfFrame() {
    try {
      Actions mouseMoveAction =
          new Actions(getDriver())
              .moveByOffset(
                  -(getViewportSize().getWidth() / 3), -(getViewportSize().getHeight() / 3));
      mouseMoveAction.build().perform();
    } catch (Exception e) {
    }
  }

  /**
   * Moves the mouse to a specific point.
   *
   * @param point the point to move the mouse to
   *     <p>precondition: point != null
   */
  public void moveMouseToNonInteractive(Point point) {
    assert point != null;
    try {
      Actions mouseMoveAction = new Actions(getDriver()).moveByOffset(point.getX(), point.getY());
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
      return getDriver().switchTo().alert();
    } catch (NoAlertPresentException Ex) {
      return null;
    }
  }
}
