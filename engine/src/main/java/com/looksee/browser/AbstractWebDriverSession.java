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

@NoArgsConstructor
@Getter
@Setter
public abstract class AbstractWebDriverSession implements DriverOps {

  private static final Logger log = LoggerFactory.getLogger(AbstractWebDriverSession.class);

  protected static final String JS_GET_VIEWPORT_WIDTH =
      "var width = undefined; if (window.innerWidth) {width = window.innerWidth;} else if (document.documentElement && document.documentElement.clientWidth) {width = document.documentElement.clientWidth;} else { var b = document.getElementsByTagName('body')[0]; if (b.clientWidth) {width = b.clientWidth;}};return width;";
  protected static final String JS_GET_VIEWPORT_HEIGHT =
      "var height = undefined;  if (window.innerHeight) {height = window.innerHeight;}  else if (document.documentElement && document.documentElement.clientHeight) {height = document.documentElement.clientHeight;}  else { var b = document.getElementsByTagName('body')[0]; if (b.clientHeight) {height = b.clientHeight;}};return height;";

  private WebDriver driver = null;
  private long yScrollOffset;
  private long xScrollOffset;
  private Dimension viewportSize;

  /** Used by tests and by subclasses that inject a pre-built driver. */
  protected AbstractWebDriverSession(WebDriver driver) {
    assert driver != null;
    this.driver = driver;
    setYScrollOffset(0);
    setXScrollOffset(0);
    setViewportSize(measureViewport(driver));
  }

  /** Subclasses that build the driver themselves call this after assigning {@code driver}. */
  protected final void initViewportState() {
    setYScrollOffset(0);
    setXScrollOffset(0);
    setViewportSize(measureViewport(this.driver));
  }

  @Override
  public WebDriver getDriver() {
    return this.driver;
  }

  @Override
  public void navigateTo(String url) {
    assert url != null;
    getDriver().get(url);
    try {
      waitForPageToLoad();
    } catch (Exception e) {
    }
  }

  @Override
  public void close() {
    try {
      driver.quit();
    } catch (Exception e) {
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
    List<String> attribute_strings =
        (ArrayList<String>)
            ((JavascriptExecutor) driver)
                .executeScript(
                    "var items = []; for (index = 0; index < arguments[0].attributes.length; ++index) { items.push(arguments[0].attributes[index].name + '::' + arguments[0].attributes[index].value) }; return items;",
                    element);
    return loadAttributes(attribute_strings);
  }

  private Map<String, String> loadAttributes(List<String> attributeList) {
    Map<String, String> attributes_seen = new HashMap<>();
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

  /** Shared by desktop and mobile; not on {@link DriverOps}. */
  public void removeElement(String class_name) {
    assert class_name != null;
    if (this.getDriver() instanceof JavascriptExecutor) {
      JavascriptExecutor js = (JavascriptExecutor) driver;
      js.executeScript("return document.getElementsByClassName('" + class_name + "')[0].remove();");
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
    int x_offset = 0;
    int y_offset = 0;
    Object offset_obj =
        ((JavascriptExecutor) driver)
            .executeScript("return window.pageXOffset+','+window.pageYOffset;");
    if (offset_obj instanceof String) {
      String[] coord = ((String) offset_obj).split(",");
      x_offset = Integer.parseInt(coord[0]);
      y_offset = Integer.parseInt(coord[1]);
    }
    this.setXScrollOffset(x_offset);
    this.setYScrollOffset(y_offset);
    return new Point(x_offset, y_offset);
  }

  @Override
  public void waitForPageToLoad() {
    new WebDriverWait(driver, Duration.ofSeconds(30))
        .until(
            webDriver ->
                ((JavascriptExecutor) webDriver)
                    .executeScript("return document.readyState")
                    .equals("complete"));
  }

  @Override
  public String getSource() {
    return this.getDriver().getPageSource();
  }

  @Override
  public boolean is503Error() {
    return HtmlUtils.is503Error(this.getSource());
  }

  protected static Dimension measureViewport(WebDriver driver) {
    return new Dimension(extractViewportWidth(driver), extractViewportHeight(driver));
  }

  protected static int extractViewportWidth(WebDriver driver) {
    JavascriptExecutor js = (JavascriptExecutor) driver;
    return Integer.parseInt(js.executeScript(JS_GET_VIEWPORT_WIDTH, new Object[0]).toString());
  }

  protected static int extractViewportHeight(WebDriver driver) {
    JavascriptExecutor js = (JavascriptExecutor) driver;
    return Integer.parseInt(js.executeScript(JS_GET_VIEWPORT_HEIGHT, new Object[0]).toString());
  }
}
