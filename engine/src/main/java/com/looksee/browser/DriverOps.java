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

  /** Returns the underlying WebDriver for this session. */
  WebDriver getDriver();

  /** Navigates to {@code url} and waits for document ready (best-effort). */
  void navigateTo(String url);

  /** Blocks until {@code document.readyState} is {@code complete}. */
  void waitForPageToLoad();

  /** Quits the underlying driver, swallowing quit failures. */
  void close();

  /** Finds an element by XPath. */
  WebElement findWebElementByXpath(String xpath);

  /** Finds an element by XPath. */
  WebElement findElement(String xpath) throws WebDriverException;

  /** Returns whether the element at {@code xpath} is displayed. */
  boolean isDisplayed(String xpath);

  /** Extracts HTML attributes from {@code element} into a name→value map. */
  Map<String, String> extractAttributes(WebElement element);

  /** Returns the current page HTML source. */
  String getSource();

  /** Returns whether the current page source looks like a 503 error page. */
  boolean is503Error();

  /** Scrolls {@code element} into view (centered). */
  void scrollToElement(WebElement element);

  /** Scrolls the viewport to the top of the page. */
  void scrollToTopOfPage();

  /** Scrolls the viewport to the bottom of the page. */
  void scrollToBottomOfPage();

  /** Scrolls down by {@code percent} of the viewport height. */
  void scrollDownPercent(double percent);

  /** Scrolls down by one full viewport height. */
  void scrollDownFull();

  /** Reads and caches the current page X/Y scroll offsets. */
  Point getViewportScrollOffset();
}
