package com.looksee.browser;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

public class AbstractWebDriverSessionTest {

  interface MockDriver extends WebDriver, JavascriptExecutor, TakesScreenshot {}

  /** Concrete subclass for testing the abstract base in isolation. */
  static final class TestSession extends AbstractWebDriverSession {
    TestSession(WebDriver driver) {
      super(driver);
    }
  }

  @Mock private MockDriver driver;
  @Mock private WebElement mockElement;

  private DriverOps session;

  @BeforeEach
  void setUp() {
    MockitoAnnotations.openMocks(this);
    when(driver.executeScript(anyString(), any(Object[].class)))
        .thenAnswer(
            inv -> {
              String script = inv.getArgument(0);
              if (script.contains("innerWidth")) return "1920";
              if (script.contains("innerHeight")) return "1080";
              return null;
            });
    when(driver.executeScript(anyString()))
        .thenAnswer(
            inv -> {
              String script = inv.getArgument(0);
              if (script.contains("innerWidth")) return "1920";
              if (script.contains("innerHeight")) return "1080";
              if (script.contains("pageXOffset")) return "10,20";
              if (script.contains("readyState")) return "complete";
              return null;
            });
    session = new TestSession(driver);
  }

  @Test
  void getDriverReturnsInjectedDriver() {
    assertSame(driver, session.getDriver());
  }

  @Test
  void navigateToCallsDriverGet() {
    when(driver.executeScript("return document.readyState")).thenReturn("complete");
    session.navigateTo("http://example.com");
    verify(driver).get("http://example.com");
  }

  @Test
  void closeQuitsDriver() {
    session.close();
    verify(driver).quit();
  }

  @Test
  void closeSwallowsQuitExceptions() {
    doThrow(new RuntimeException("boom")).when(driver).quit();
    assertDoesNotThrow(() -> session.close());
  }

  @Test
  void findElementByXpath() {
    when(driver.findElement(By.xpath("//div"))).thenReturn(mockElement);
    assertSame(mockElement, session.findElement("//div"));
  }

  @Test
  void getSourceReturnsPageSource() {
    when(driver.getPageSource()).thenReturn("<html/>");
    assertEquals("<html/>", session.getSource());
  }

  @Test
  void scrollToTopExecutesScrollScript() {
    session.scrollToTopOfPage();
    verify(driver).executeScript(contains("scrollTo(0, 0)"));
  }

  @Test
  void getViewportScrollOffsetParsesJsResult() {
    when(driver.executeScript(contains("pageXOffset"))).thenReturn("12,34");
    var point = session.getViewportScrollOffset();
    assertEquals(12, point.getX());
    assertEquals(34, point.getY());
  }

  @Test
  void extractAttributesParsesJsList() {
    List<String> attrs = new ArrayList<>();
    attrs.add("id::main");
    attrs.add("class::foo bar");
    when(driver.executeScript(contains("attributes"), eq(mockElement))).thenReturn(attrs);
    Map<String, String> map = session.extractAttributes(mockElement);
    assertTrue(map.containsKey("id"));
    assertTrue(map.containsKey("class"));
  }
}
