package com.looksee.models;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

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
import org.openqa.selenium.By;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.Point;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

public class MobileDeviceTest {

    @TempDir
    File tempDir;

    private MobileDevice device;

    interface MockMobileDriver extends WebDriver, JavascriptExecutor, TakesScreenshot {}

    @Mock
    private MockMobileDriver driver;

    @Mock
    private WebElement mockElement;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        when(driver.executeScript(contains("innerWidth"), any())).thenReturn("375");
        when(driver.executeScript(contains("innerHeight"), any())).thenReturn("812");
        device = new MobileDevice(driver, "android");
    }

    @Test
    public void testConstructorWithDriver() {
        assertNotNull(device.getDriver());
        assertEquals("android", device.getPlatformName());
        assertEquals(0, device.getYScrollOffset());
        assertEquals(0, device.getXScrollOffset());
        assertNotNull(device.getViewportSize());
    }

    @Test
    public void testNavigateTo() {
        when(driver.executeScript("return document.readyState")).thenReturn("complete");
        device.navigateTo("http://example.com");
        verify(driver).get("http://example.com");
    }

    @Test
    public void testClose() {
        device.close();
        verify(driver).quit();
    }

    @Test
    public void testCloseWithException() {
        doThrow(new RuntimeException("error")).when(driver).quit();
        assertDoesNotThrow(() -> device.close());
    }

    @Test
    public void testGetViewportScreenshot() throws IOException {
        BufferedImage img = new BufferedImage(375, 812, BufferedImage.TYPE_INT_RGB);
        File tempFile = new File(tempDir, "mobile_screenshot.png");
        ImageIO.write(img, "png", tempFile);

        when(driver.getScreenshotAs(OutputType.FILE)).thenReturn(tempFile);
        BufferedImage result = device.getViewportScreenshot();
        assertNotNull(result);
        assertEquals(375, result.getWidth());
    }

    @Test
    public void testGetFullPageScreenshotFallsBackToViewport() throws IOException {
        BufferedImage img = new BufferedImage(375, 812, BufferedImage.TYPE_INT_RGB);
        File tempFile = new File(tempDir, "full_screenshot.png");
        ImageIO.write(img, "png", tempFile);

        when(driver.getScreenshotAs(OutputType.FILE)).thenReturn(tempFile);
        BufferedImage result = device.getFullPageScreenshot();
        assertNotNull(result);
    }

    @Test
    public void testGetElementScreenshot() throws IOException {
        BufferedImage img = new BufferedImage(50, 30, BufferedImage.TYPE_INT_RGB);
        File tempFile = new File(tempDir, "elem_screenshot.png");
        ImageIO.write(img, "png", tempFile);

        when(mockElement.getScreenshotAs(OutputType.FILE)).thenReturn(tempFile);
        BufferedImage result = device.getElementScreenshot(mockElement);
        assertNotNull(result);
        assertEquals(50, result.getWidth());
    }

    @Test
    public void testFindWebElementByXpath() {
        when(driver.findElement(By.xpath("//div"))).thenReturn(mockElement);
        WebElement result = device.findWebElementByXpath("//div");
        assertEquals(mockElement, result);
    }

    @Test
    public void testFindElement() {
        when(driver.findElement(By.xpath("//button"))).thenReturn(mockElement);
        WebElement result = device.findElement("//button");
        assertEquals(mockElement, result);
    }

    @Test
    public void testIsDisplayed() {
        when(driver.findElement(By.xpath("//p"))).thenReturn(mockElement);
        when(mockElement.isDisplayed()).thenReturn(true);
        assertTrue(device.isDisplayed("//p"));
    }

    @Test
    public void testExtractAttributes() {
        List<String> attrs = new ArrayList<>();
        attrs.add("class::btn");
        attrs.add("id::submit");
        when(driver.executeScript(contains("attributes"), eq(mockElement))).thenReturn(attrs);

        Map<String, String> result = device.extractAttributes(mockElement);
        assertNotNull(result);
        assertEquals("[btn]", result.get("class"));
    }

    @Test
    public void testRemoveElement() {
        device.removeElement("overlay");
        verify(driver).executeScript(contains("getElementsByClassName"));
    }

    @Test
    public void testScrollToElement() {
        when(driver.executeScript(contains("pageXOffset"))).thenReturn("0,200");
        device.scrollToElement(mockElement);
        verify(driver).executeScript("arguments[0].scrollIntoView({block: 'center'});", mockElement);
    }

    @Test
    public void testScrollToBottomOfPage() {
        when(driver.executeScript(contains("pageXOffset"))).thenReturn("0,500");
        device.scrollToBottomOfPage();
        verify(driver).executeScript("window.scrollTo(0, document.body.scrollHeight)");
    }

    @Test
    public void testScrollToTopOfPage() {
        when(driver.executeScript(contains("pageXOffset"))).thenReturn("0,0");
        device.scrollToTopOfPage();
        verify(driver).executeScript("window.scrollTo(0, 0)");
    }

    @Test
    public void testScrollDownPercent() {
        when(driver.executeScript(contains("pageXOffset"))).thenReturn("0,100");
        device.scrollDownPercent(0.5);
        verify(driver).executeScript("window.scrollBy(0, (window.innerHeight*0.5))");
    }

    @Test
    public void testScrollDownFull() {
        when(driver.executeScript(contains("pageXOffset"))).thenReturn("0,300");
        device.scrollDownFull();
        verify(driver).executeScript("window.scrollBy(0, window.innerHeight)");
    }

    @Test
    public void testGetViewportScrollOffset() {
        when(driver.executeScript(contains("pageXOffset"))).thenReturn("50,200");
        Point offset = device.getViewportScrollOffset();
        assertEquals(50, offset.getX());
        assertEquals(200, offset.getY());
    }

    @Test
    public void testGetViewportScrollOffsetNonString() {
        when(driver.executeScript(contains("pageXOffset"))).thenReturn(42);
        Point offset = device.getViewportScrollOffset();
        assertEquals(0, offset.getX());
        assertEquals(0, offset.getY());
    }

    @Test
    public void testWaitForPageToLoad() {
        when(driver.executeScript("return document.readyState")).thenReturn("complete");
        assertDoesNotThrow(() -> device.waitForPageToLoad());
    }

    @Test
    public void testGetSource() {
        when(driver.getPageSource()).thenReturn("<html>mobile</html>");
        assertEquals("<html>mobile</html>", device.getSource());
    }

    @Test
    public void testIs503Error() {
        when(driver.getPageSource()).thenReturn("<html>503 Service Temporarily Unavailable</html>");
        assertTrue(device.is503Error());
    }

    @Test
    public void testIs503ErrorFalse() {
        when(driver.getPageSource()).thenReturn("<html>OK</html>");
        assertFalse(device.is503Error());
    }

    @Test
    public void testNoArgsConstructor() {
        MobileDevice empty = new MobileDevice();
        assertNull(empty.getDriver());
        assertNull(empty.getPlatformName());
    }

    @Test
    public void testNavigateToWithWaitException() {
        when(driver.executeScript("return document.readyState")).thenThrow(new RuntimeException("timeout"));
        assertDoesNotThrow(() -> device.navigateTo("http://example.com"));
        verify(driver).get("http://example.com");
    }

    @Test
    public void testIsDisplayedFalse() {
        when(driver.findElement(By.xpath("//p"))).thenReturn(mockElement);
        when(mockElement.isDisplayed()).thenReturn(false);
        assertFalse(device.isDisplayed("//p"));
    }

    @Test
    public void testExtractAttributesEmpty() {
        List<String> attrs = new ArrayList<>();
        when(driver.executeScript(contains("attributes"), eq(mockElement))).thenReturn(attrs);

        Map<String, String> result = device.extractAttributes(mockElement);
        assertTrue(result.isEmpty());
    }

    @Test
    public void testExtractAttributesSinglePart() {
        List<String> attrs = new ArrayList<>();
        attrs.add("data-only");
        when(driver.executeScript(contains("attributes"), eq(mockElement))).thenReturn(attrs);

        Map<String, String> result = device.extractAttributes(mockElement);
        assertTrue(result.isEmpty());
    }

    @Test
    public void testExtractAttributesDuplicateKey() {
        List<String> attrs = new ArrayList<>();
        attrs.add("class::first");
        attrs.add("class::second");
        when(driver.executeScript(contains("attributes"), eq(mockElement))).thenReturn(attrs);

        Map<String, String> result = device.extractAttributes(mockElement);
        assertEquals("[first]", result.get("class"));
    }

    @Test
    public void testExtractAttributesWithSpecialCharsInName() {
        List<String> attrs = new ArrayList<>();
        attrs.add("data-val::test");
        attrs.add("aria-label::hello world");
        when(driver.executeScript(contains("attributes"), eq(mockElement))).thenReturn(attrs);

        Map<String, String> result = device.extractAttributes(mockElement);
        assertEquals("[test]", result.get("data-val"));
        assertEquals("[hello, world]", result.get("aria-label"));
    }

    @Test
    public void testGetterSetter() {
        device.setYScrollOffset(100);
        assertEquals(100, device.getYScrollOffset());

        device.setXScrollOffset(50);
        assertEquals(50, device.getXScrollOffset());

        device.setPlatformName("ios");
        assertEquals("ios", device.getPlatformName());
    }

    @Test
    public void testGetViewportSizeIsSetInConstructor() {
        Dimension size = device.getViewportSize();
        assertNotNull(size);
        assertEquals(375, size.getWidth());
        assertEquals(812, size.getHeight());
    }

    @Test
    public void testSetViewportSize() {
        Dimension newSize = new Dimension(390, 844);
        device.setViewportSize(newSize);
        assertEquals(390, device.getViewportSize().getWidth());
    }

    @Test
    public void testSetDriver() {
        MockMobileDriver newDriver = mock(MockMobileDriver.class);
        device.setDriver(newDriver);
        assertEquals(newDriver, device.getDriver());
    }
}
