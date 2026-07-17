package com.looksee.browser;

import com.looksee.browser.utils.HtmlUtils;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.openqa.selenium.By;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.Point;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Shared WebDriver session state and DOM/navigation operations for {@link Browser} and {@link
 * MobileDevice}. Screenshots and input stacks remain on the concrete subclasses.
 */
@NoArgsConstructor
@Getter
@Setter
public abstract class AbstractWebDriverSession implements DriverOps {

  private static final Logger log = LoggerFactory.getLogger(AbstractWebDriverSession.class);

  protected static final String JS_GET_VIEWPORT_WIDTH =
      "var width = undefined; if (window.innerWidth) {width = window.innerWidth;} "
          + "else if (document.documentElement && document.documentElement.clientWidth) {"
          + "width = document.documentElement.clientWidth;} else { "
          + "var b = document.getElementsByTagName('body')[0]; "
          + "if (b.clientWidth) {width = b.clientWidth;}};return width;";
  protected static final String JS_GET_VIEWPORT_HEIGHT =
      "var height = undefined;  if (window.innerHeight) {height = window.innerHeight;}  "
          + "else if (document.documentElement && document.documentElement.clientHeight) {"
          + "height = document.documentElement.clientHeight;}  else { "
          + "var b = document.getElementsByTagName('body')[0]; "
          + "if (b.clientHeight) {height = b.clientHeight;}};return height;";
  private static final String JS_EXTRACT_ATTRIBUTES =
      "var items = []; for (index = 0; index < arguments[0].attributes.length; ++index) { "
          + "items.push(arguments[0].attributes[index].name + '::' "
          + "+ arguments[0].attributes[index].value) }; return items;";

  private WebDriver driver;
  private long scrollOffsetY;
  private long scrollOffsetX;
  private Dimension viewportSize;

  /**
   * Compatibility alias for {@link #getScrollOffsetX()}. Kept for engine JAR callers that used the
   * pre-DriverOps Lombok accessor names.
   */
  // CHECKSTYLE.SUPPRESS: AbbreviationAsWordInName for +1 lines
  public long getXScrollOffset() {
    return getScrollOffsetX();
  }

  /**
   * Compatibility alias for {@link #setScrollOffsetX(long)}. Kept for engine JAR callers that used
   * the pre-DriverOps Lombok accessor names.
   */
  // CHECKSTYLE.SUPPRESS: AbbreviationAsWordInName for +1 lines
  public void setXScrollOffset(long offset) {
    setScrollOffsetX(offset);
  }

  /**
   * Compatibility alias for {@link #getScrollOffsetY()}. Kept for engine JAR callers that used the
   * pre-DriverOps Lombok accessor names.
   */
  // CHECKSTYLE.SUPPRESS: AbbreviationAsWordInName for +1 lines
  public long getYScrollOffset() {
    return getScrollOffsetY();
  }

  /**
   * Compatibility alias for {@link #setScrollOffsetY(long)}. Kept for engine JAR callers that used
   * the pre-DriverOps Lombok accessor names.
   */
  // CHECKSTYLE.SUPPRESS: AbbreviationAsWordInName for +1 lines
  public void setYScrollOffset(long offset) {
    setScrollOffsetY(offset);
  }

  /** Used by tests and by subclasses that inject a pre-built driver. */
  protected AbstractWebDriverSession(WebDriver driver) {
    this.driver = driver;
    this.scrollOffsetY = 0;
    this.scrollOffsetX = 0;
  }

  /**
   * Assigns the WebDriver without going through an overridable setter. Subclasses that create the
   * driver themselves should call this before {@link #initViewportState()}.
   */
  protected final void bindDriver(WebDriver driver) {
    this.driver = driver;
  }

  /** Subclasses that build the driver themselves call this after assigning {@code driver}. */
  protected final void initViewportState() {
    setScrollOffsetY(0);
    setScrollOffsetX(0);
    setViewportSize(measureViewport(this.driver));
  }

  @Override
  public WebDriver getDriver() {
    return this.driver;
  }

  @Override
  @SuppressWarnings("PMD.AvoidCatchingGenericException")
  public void navigateTo(String url) {
    assert url != null;
    getDriver().get(url);
    try {
      waitForPageToLoad();
    } catch (Exception e) {
      // Best-effort wait: decorators/listeners may throw non-WebDriverException.
      log.debug("waitForPageToLoad during navigateTo failed: {}", e.toString());
    }
  }

  @Override
  @SuppressWarnings("PMD.AvoidCatchingGenericException")
  public void close() {
    try {
      driver.quit();
    } catch (Exception e) {
      // Swallow all quit failures (decorators/listeners included).
      log.debug("Exception occurred when closing session: " + e.getMessage());
    }
  }

  @Override
  public WebElement findWebElementByXpath(String xpath) {
    assert xpath != null;
    assert !xpath.isEmpty();
    return driver.findElement(By.xpath(xpath));
  }

  @Override
  public WebElement findElement(String xpath) throws WebDriverException {
    assert xpath != null;
    assert !xpath.isEmpty();
    return getDriver().findElement(By.xpath(xpath));
  }

  @Override
  public boolean isDisplayed(String xpath) {
    assert xpath != null;
    assert !xpath.isEmpty();
    return driver.findElement(By.xpath(xpath)).isDisplayed();
  }

  @Override
  @SuppressWarnings("unchecked")
  public Map<String, String> extractAttributes(WebElement element) {
    assert element != null;
    List<String> attributeStrings =
        (ArrayList<String>)
            ((JavascriptExecutor) driver).executeScript(JS_EXTRACT_ATTRIBUTES, element);
    return loadAttributes(attributeStrings);
  }

  private Map<String, String> loadAttributes(List<String> attributeList) {
    Map<String, String> attributesSeen = new HashMap<>();
    for (String attributeEntry : attributeList) {
      String[] attributes = attributeEntry.split("::");
      if (attributes.length > 1) {
        String attributeName = attributes[0].trim().replace("\'", "'");
        String[] attributeVals = attributes[1].split(" ");
        if (!attributesSeen.containsKey(attributeName)) {
          attributesSeen.put(attributeName, Arrays.asList(attributeVals).toString());
        }
      }
    }
    return attributesSeen;
  }

  /** Shared by desktop and mobile; not on {@link DriverOps}. */
  public void removeElement(String className) {
    assert className != null;
    if (this.getDriver() instanceof JavascriptExecutor) {
      JavascriptExecutor js = (JavascriptExecutor) driver;
      js.executeScript("return document.getElementsByClassName('" + className + "')[0].remove();");
    }
  }

  @Override
  public void scrollToElement(WebElement element) {
    assert element != null;
    ((JavascriptExecutor) driver)
        .executeScript("arguments[0].scrollIntoView({block: 'center'});", element);
    getViewportScrollOffset();
  }

  @Override
  public void scrollToBottomOfPage() {
    ((JavascriptExecutor) driver).executeScript("window.scrollTo(0, document.body.scrollHeight)");
    getViewportScrollOffset();
  }

  @Override
  public void scrollToTopOfPage() {
    ((JavascriptExecutor) driver).executeScript("window.scrollTo(0, 0)");
    getViewportScrollOffset();
  }

  @Override
  public void scrollDownPercent(double percent) {
    ((JavascriptExecutor) driver)
        .executeScript("window.scrollBy(0, (window.innerHeight*" + percent + "))");
    getViewportScrollOffset();
  }

  @Override
  public void scrollDownFull() {
    ((JavascriptExecutor) driver).executeScript("window.scrollBy(0, window.innerHeight)");
    getViewportScrollOffset();
  }

  @Override
  public Point getViewportScrollOffset() {
    int offsetX = 0;
    int offsetY = 0;
    Object offsetObj =
        ((JavascriptExecutor) driver)
            .executeScript("return window.pageXOffset+','+window.pageYOffset;");
    if (offsetObj instanceof String) {
      String[] coord = ((String) offsetObj).split(",");
      offsetX = Integer.parseInt(coord[0]);
      offsetY = Integer.parseInt(coord[1]);
    }
    this.setScrollOffsetX(offsetX);
    this.setScrollOffsetY(offsetY);
    return new Point(offsetX, offsetY);
  }

  @Override
  public void waitForPageToLoad() {
    new WebDriverWait(driver, Duration.ofSeconds(30))
        .until(
            webDriver ->
                "complete"
                    .equals(
                        ((JavascriptExecutor) webDriver)
                            .executeScript("return document.readyState")));
  }

  @Override
  public String getSource() {
    return this.getDriver().getPageSource();
  }

  @Override
  public boolean is503Error() {
    return HtmlUtils.is503Error(this.getSource());
  }

  /** Measures the current viewport size via JS. */
  protected static Dimension measureViewport(WebDriver driver) {
    return new Dimension(extractViewportWidth(driver), extractViewportHeight(driver));
  }

  /** Extracts viewport width via JS. */
  protected static int extractViewportWidth(WebDriver driver) {
    JavascriptExecutor js = (JavascriptExecutor) driver;
    return Integer.parseInt(js.executeScript(JS_GET_VIEWPORT_WIDTH, new Object[0]).toString());
  }

  /** Extracts viewport height via JS. */
  protected static int extractViewportHeight(WebDriver driver) {
    JavascriptExecutor js = (JavascriptExecutor) driver;
    return Integer.parseInt(js.executeScript(JS_GET_VIEWPORT_HEIGHT, new Object[0]).toString());
  }
}
