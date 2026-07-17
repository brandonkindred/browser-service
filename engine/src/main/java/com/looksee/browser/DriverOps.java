package com.looksee.browser;

import java.util.Map;
import org.openqa.selenium.Point;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.WebElement;

/**
 * Shared DOM / navigation operations for desktop ({@link Browser}) and mobile ({@link
 * MobileDevice}) sessions. Screenshots and input stacks are intentionally not part of this
 * contract.
 */
public interface DriverOps {

  WebDriver getDriver();

  void navigateTo(String url);

  void waitForPageToLoad();

  void close();

  WebElement findWebElementByXpath(String xpath);

  WebElement findElement(String xpath) throws WebDriverException;

  boolean isDisplayed(String xpath);

  Map<String, String> extractAttributes(WebElement element);

  String getSource();

  boolean is503Error();

  void scrollToElement(WebElement element);

  void scrollToTopOfPage();

  void scrollToBottomOfPage();

  void scrollDownPercent(double percent);

  void scrollDownFull();

  Point getViewportScrollOffset();
}
