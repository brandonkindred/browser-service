package com.looksee.browser;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URL;
import javax.imageio.ImageIO;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

/**
 * Manages an Appium mobile device session and provides mobile-only methods for interacting with
 * mobile browsers (screenshots). This is the mobile counterpart of {@link Browser}, allowing Appium
 * to evolve independently from Selenium. Shared navigation, scrolling, and DOM inspection logic
 * lives on {@link AbstractWebDriverSession}.
 *
 * <p>Uses native Appium/WebDriver screenshot capabilities instead of Shutterbug/AShot (which are
 * desktop-only). Does not support mouse actions since mobile devices use touch interactions.
 *
 * <p><b>Class Invariants:</b>
 *
 * <ul>
 *   <li>invariant: platformName is not null after parameterized construction
 *   <li>invariant: driver is not null after parameterized construction
 *   <li>invariant: viewportSize is not null after parameterized construction
 *   <li>invariant: yScrollOffset >= 0
 *   <li>invariant: xScrollOffset >= 0
 * </ul>
 */
@NoArgsConstructor
@Getter
@Setter
public class MobileDevice extends AbstractWebDriverSession {

  private String platformName;

  /**
   * Constructor for {@link MobileDevice} that dispatches to {@link MobileFactory} for driver
   * creation.
   *
   * @param platformType the mobile platform ("android", "ios")
   * @param serverUrl the URL of the Appium server
   *     <p>precondition: platformType != null precondition: serverUrl != null
   */
  public MobileDevice(String platformType, URL serverUrl) {
    assert platformType != null;
    assert serverUrl != null;

    this.setPlatformName(platformType);
    this.setDriver(MobileFactory.createDriver(platformType, serverUrl));
    initViewportState();
  }

  /**
   * Constructor for {@link MobileDevice} that accepts a pre-built WebDriver.
   *
   * @param driver the WebDriver instance (AndroidDriver or IOSDriver)
   * @param platformName the platform name ("android", "ios")
   *     <p>precondition: driver != null precondition: platformName != null
   */
  public MobileDevice(WebDriver driver, String platformName) {
    super(driver);
    assert platformName != null;
    this.setPlatformName(platformName);
  }

  // ==================== Screenshots ====================

  /**
   * Takes a viewport screenshot using the native Appium screenshot capability.
   *
   * @return BufferedImage of the viewport
   * @throws IOException if an error occurs while getting the screenshot
   */
  public BufferedImage getViewportScreenshot() throws IOException {
    return ImageIO.read(((TakesScreenshot) getDriver()).getScreenshotAs(OutputType.FILE));
  }

  /**
   * Takes a full-page screenshot. On mobile, this falls back to a viewport screenshot since
   * Shutterbug/AShot are not compatible with Appium drivers.
   *
   * @return BufferedImage of the viewport
   * @throws IOException if an error occurs while getting the screenshot
   */
  public BufferedImage getFullPageScreenshot() throws IOException {
    return getViewportScreenshot();
  }

  /**
   * Takes a screenshot of a specific WebElement using Appium's native element screenshot
   * capability.
   *
   * @param element the element to capture
   * @return the screenshot
   * @throws IOException if an error occurs while getting the screenshot
   *     <p>precondition: element != null
   */
  public BufferedImage getElementScreenshot(WebElement element) throws IOException {
    assert element != null;
    return ImageIO.read(element.getScreenshotAs(OutputType.FILE));
  }
}
