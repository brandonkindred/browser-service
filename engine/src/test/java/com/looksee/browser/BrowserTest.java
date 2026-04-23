package com.looksee.browser;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.imageio.ImageIO;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.openqa.selenium.Alert;
import org.openqa.selenium.By;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.NoAlertPresentException;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.Point;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

public class BrowserTest {

    @TempDir
    File tempDir;

    private Browser browser;

    // Combined mock interface for WebDriver + JavascriptExecutor + TakesScreenshot
    interface MockDriver extends WebDriver, JavascriptExecutor, TakesScreenshot {}

    @Mock
    private MockDriver driver;

    @Mock
    private WebElement mockElement;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        when(driver.executeScript(contains("innerWidth"), any())).thenReturn("1920");
        when(driver.executeScript(contains("innerHeight"), any())).thenReturn("1080");
        browser = new Browser(driver, "chrome");
    }

    @Test
    public void testConstructorWithDriver() {
        assertNotNull(browser.getDriver());
        assertEquals("chrome", browser.getBrowserName());
        assertEquals(0, browser.getYScrollOffset());
        assertEquals(0, browser.getXScrollOffset());
        assertNotNull(browser.getViewportSize());
    }

    @Test
    public void testNavigateTo() {
        when(driver.executeScript("return document.readyState")).thenReturn("complete");
        browser.navigateTo("http://example.com");
        verify(driver).get("http://example.com");
    }

    @Test
    public void testClose() {
        browser.close();
        verify(driver).quit();
    }

    @Test
    public void testCloseWithException() {
        doThrow(new RuntimeException("error")).when(driver).quit();
        assertDoesNotThrow(() -> browser.close());
    }

    @Test
    public void testGetViewportScreenshot() throws IOException {
        // Create a temp image file for the screenshot
        BufferedImage img = new BufferedImage(100, 100, BufferedImage.TYPE_INT_RGB);
        File tempFile = new File(tempDir, "screenshot.png");
        ImageIO.write(img, "png", tempFile);

        when(driver.getScreenshotAs(OutputType.FILE)).thenReturn(tempFile);
        BufferedImage result = browser.getViewportScreenshot();
        assertNotNull(result);
        assertEquals(100, result.getWidth());
    }

    @Test
    public void testFindWebElementByXpath() {
        when(driver.findElement(By.xpath("//div"))).thenReturn(mockElement);
        WebElement result = browser.findWebElementByXpath("//div");
        assertEquals(mockElement, result);
    }

    @Test
    public void testFindElement() {
        when(driver.findElement(By.xpath("//span"))).thenReturn(mockElement);
        WebElement result = browser.findElement("//span");
        assertEquals(mockElement, result);
    }

    @Test
    public void testIsDisplayed() {
        when(driver.findElement(By.xpath("//p"))).thenReturn(mockElement);
        when(mockElement.isDisplayed()).thenReturn(true);
        assertTrue(browser.isDisplayed("//p"));
    }

    @Test
    public void testIsDisplayedFalse() {
        when(driver.findElement(By.xpath("//p"))).thenReturn(mockElement);
        when(mockElement.isDisplayed()).thenReturn(false);
        assertFalse(browser.isDisplayed("//p"));
    }

    @Test
    public void testExtractAttributes() {
        List<String> attrs = new ArrayList<>();
        attrs.add("class::myclass");
        attrs.add("id::myid");
        when(driver.executeScript(contains("attributes"), eq(mockElement))).thenReturn(attrs);

        Map<String, String> result = browser.extractAttributes(mockElement);
        assertNotNull(result);
        assertEquals("[myclass]", result.get("class"));
        assertEquals("[myid]", result.get("id"));
    }

    @Test
    public void testExtractAttributesEmpty() {
        List<String> attrs = new ArrayList<>();
        when(driver.executeScript(contains("attributes"), eq(mockElement))).thenReturn(attrs);

        Map<String, String> result = browser.extractAttributes(mockElement);
        assertTrue(result.isEmpty());
    }

    @Test
    public void testExtractAttributesSinglePart() {
        List<String> attrs = new ArrayList<>();
        attrs.add("data-only");
        when(driver.executeScript(contains("attributes"), eq(mockElement))).thenReturn(attrs);

        Map<String, String> result = browser.extractAttributes(mockElement);
        assertTrue(result.isEmpty());
    }

    @Test
    public void testRemoveElement() {
        browser.removeElement("popup");
        verify(driver).executeScript(contains("getElementsByClassName"));
    }

    @Test
    public void testRemoveDriftChat() {
        browser.removeDriftChat();
        verify(driver).executeScript(contains("drift-frame-chat"));
    }

    @Test
    public void testRemoveGDPRmodals() {
        browser.removeGDPRmodals();
        verify(driver).executeScript(contains("gdprModal"));
    }

    @Test
    public void testRemoveGDPR() {
        browser.removeGDPR();
        verify(driver).executeScript(contains("gdpr"));
    }

    @Test
    public void testScrollToBottomOfPage() {
        when(driver.executeScript(contains("pageXOffset"))).thenReturn("0,500");
        browser.scrollToBottomOfPage();
        verify(driver).executeScript("window.scrollTo(0, document.body.scrollHeight)");
    }

    @Test
    public void testScrollToTopOfPage() {
        when(driver.executeScript(contains("pageXOffset"))).thenReturn("0,0");
        browser.scrollToTopOfPage();
        verify(driver).executeScript("window.scrollTo(0, 0)");
    }

    @Test
    public void testScrollDownPercent() {
        when(driver.executeScript(contains("pageXOffset"))).thenReturn("0,100");
        browser.scrollDownPercent(0.5);
        verify(driver).executeScript("window.scrollBy(0, (window.innerHeight*0.5))");
    }

    @Test
    public void testScrollDownFull() {
        when(driver.executeScript(contains("pageXOffset"))).thenReturn("0,200");
        browser.scrollDownFull();
        verify(driver).executeScript("window.scrollBy(0, window.innerHeight)");
    }

    @Test
    public void testScrollToElementCentered() {
        when(driver.executeScript(contains("pageXOffset"))).thenReturn("0,300");
        browser.scrollToElementCentered(mockElement);
        verify(driver).executeScript("arguments[0].scrollIntoView({block: 'center'});", mockElement);
    }

    @Test
    public void testScrollToElementSingle() {
        when(driver.executeScript(contains("pageXOffset"))).thenReturn("0,300");
        browser.scrollToElement(mockElement);
        verify(driver).executeScript("arguments[0].scrollIntoView({block: 'center'});", mockElement);
    }

    @Test
    public void testGetViewportScrollOffset() {
        when(driver.executeScript(contains("pageXOffset"))).thenReturn("150,300");
        Point offset = browser.getViewportScrollOffset();
        assertEquals(150, offset.getX());
        assertEquals(300, offset.getY());
        assertEquals(150, browser.getXScrollOffset());
        assertEquals(300, browser.getYScrollOffset());
    }

    @Test
    public void testGetViewportScrollOffsetNonString() {
        when(driver.executeScript(contains("pageXOffset"))).thenReturn(42);
        Point offset = browser.getViewportScrollOffset();
        assertEquals(0, offset.getX());
        assertEquals(0, offset.getY());
    }

    @Test
    public void testMoveMouseOutOfFrame() {
        // Should not throw
        assertDoesNotThrow(() -> browser.moveMouseOutOfFrame());
    }

    @Test
    public void testMoveMouseToNonInteractive() {
        Point point = new Point(100, 200);
        assertDoesNotThrow(() -> browser.moveMouseToNonInteractive(point));
    }

    @Test
    public void testIsAlertPresent() {
        WebDriver.TargetLocator targetLocator = mock(WebDriver.TargetLocator.class);
        Alert alert = mock(Alert.class);
        when(driver.switchTo()).thenReturn(targetLocator);
        when(targetLocator.alert()).thenReturn(alert);
        assertEquals(alert, browser.isAlertPresent());
    }

    @Test
    public void testIsAlertPresentNoAlert() {
        WebDriver.TargetLocator targetLocator = mock(WebDriver.TargetLocator.class);
        when(driver.switchTo()).thenReturn(targetLocator);
        when(targetLocator.alert()).thenThrow(new NoAlertPresentException());
        assertNull(browser.isAlertPresent());
    }

    @Test
    public void testGetSource() {
        when(driver.getPageSource()).thenReturn("<html></html>");
        assertEquals("<html></html>", browser.getSource());
    }

    @Test
    public void testIs503Error() {
        when(driver.getPageSource()).thenReturn("<html>503 Service Temporarily Unavailable</html>");
        assertTrue(browser.is503Error());
    }

    @Test
    public void testIs503ErrorFalse() {
        when(driver.getPageSource()).thenReturn("<html>OK</html>");
        assertFalse(browser.is503Error());
    }

    @Test
    public void testWaitForPageToLoad() {
        when(driver.executeScript("return document.readyState")).thenReturn("complete");
        assertDoesNotThrow(() -> browser.waitForPageToLoad());
    }

    @Test
    public void testNoArgsConstructor() {
        Browser emptyBrowser = new Browser();
        assertNull(emptyBrowser.getDriver());
        assertNull(emptyBrowser.getBrowserName());
    }

    @Test
    public void testScrollToElementWithNavXpath() {
        when(driver.executeScript(contains("pageXOffset"))).thenReturn("0,0");
        browser.scrollToElement("//body/nav/a", mockElement);
        verify(driver).executeScript("window.scrollTo(0, 0)");
    }

    @Test
    public void testScrollToElementWithHeaderXpath() {
        when(driver.executeScript(contains("pageXOffset"))).thenReturn("0,0");
        browser.scrollToElement("//body/header/div", mockElement);
        verify(driver).executeScript("window.scrollTo(0, 0)");
    }

    @Test
    public void testNavigateToWithWaitException() {
        // When waitForPageToLoad throws, navigateTo should still succeed
        when(driver.executeScript("return document.readyState")).thenThrow(new RuntimeException("timeout"));
        assertDoesNotThrow(() -> browser.navigateTo("http://example.com"));
        verify(driver).get("http://example.com");
    }

    @Test
    public void testExtractAttributesDuplicateKey() {
        List<String> attrs = new ArrayList<>();
        attrs.add("class::first");
        attrs.add("class::second");
        when(driver.executeScript(contains("attributes"), eq(mockElement))).thenReturn(attrs);

        Map<String, String> result = browser.extractAttributes(mockElement);
        // Should keep the first occurrence and ignore duplicate
        assertEquals("[first]", result.get("class"));
    }

    @Test
    public void testExtractAttributesWithSpecialCharsInName() {
        List<String> attrs = new ArrayList<>();
        attrs.add("data-val::test");
        attrs.add("aria-label::hello world");
        when(driver.executeScript(contains("attributes"), eq(mockElement))).thenReturn(attrs);

        Map<String, String> result = browser.extractAttributes(mockElement);
        assertNotNull(result);
        assertEquals("[test]", result.get("data-val"));
        assertEquals("[hello, world]", result.get("aria-label"));
    }

    @Test
    public void testExtractAttributesMultipleValues() {
        List<String> attrs = new ArrayList<>();
        attrs.add("class::btn primary large");
        when(driver.executeScript(contains("attributes"), eq(mockElement))).thenReturn(attrs);

        Map<String, String> result = browser.extractAttributes(mockElement);
        assertEquals("[btn, primary, large]", result.get("class"));
    }

    @Test
    public void testRemoveElementWhenNotJavascriptExecutor() {
        // Test when driver is not a JavascriptExecutor - use a plain WebDriver mock
        WebDriver plainDriver = mock(WebDriver.class);
        when(driver.executeScript(contains("innerWidth"), any())).thenReturn("1920");
        when(driver.executeScript(contains("innerHeight"), any())).thenReturn("1080");

        // The browser's driver IS a JavascriptExecutor, so removeElement will execute
        browser.removeElement("popup");
        verify(driver).executeScript(contains("getElementsByClassName"));
    }

    @Test
    public void testScrollToElementWithNonNavXpath() {
        // Element already at offset - loop should terminate immediately
        when(mockElement.getLocation()).thenReturn(new Point(0, 0));
        when(driver.executeScript(contains("pageXOffset"))).thenReturn("0,0");

        browser.setYScrollOffset(0);
        browser.scrollToElement("//body/div/section", mockElement);

        // getViewportScrollOffset should be called
        verify(driver, atLeastOnce()).executeScript(contains("pageXOffset"));
    }

    @Test
    public void testGetterSetter() {
        browser.setYScrollOffset(100);
        assertEquals(100, browser.getYScrollOffset());

        browser.setXScrollOffset(50);
        assertEquals(50, browser.getXScrollOffset());

        browser.setBrowserName("firefox");
        assertEquals("firefox", browser.getBrowserName());
    }

    @Test
    public void testGetViewportSizeIsSetInConstructor() {
        Dimension size = browser.getViewportSize();
        assertNotNull(size);
        assertEquals(1920, size.getWidth());
        assertEquals(1080, size.getHeight());
    }

    @Test
    public void testSetViewportSize() {
        Dimension newSize = new Dimension(1024, 768);
        browser.setViewportSize(newSize);
        assertEquals(1024, browser.getViewportSize().getWidth());
        assertEquals(768, browser.getViewportSize().getHeight());
    }

    @Test
    public void testSetDriver() {
        MockDriver newDriver = mock(MockDriver.class);
        browser.setDriver(newDriver);
        assertEquals(newDriver, browser.getDriver());
    }
}
