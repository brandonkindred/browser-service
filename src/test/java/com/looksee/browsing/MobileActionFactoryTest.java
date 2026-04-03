package com.looksee.browsing;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.looksee.browsing.enums.MobileAction;
import io.appium.java_client.PerformsTouchActions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

public class MobileActionFactoryTest {

    // Mock driver that implements both WebDriver and PerformsTouchActions
    interface MockAppiumDriver extends WebDriver, PerformsTouchActions {}

    @Mock
    private MockAppiumDriver appiumDriver;

    @Mock
    private WebElement element;

    @Mock
    private WebDriver.Options options;

    @Mock
    private WebDriver.Window window;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        when(appiumDriver.manage()).thenReturn(options);
        when(options.window()).thenReturn(window);
        when(window.getSize()).thenReturn(new Dimension(375, 812));
    }

    @Test
    public void testConstructorWithAppiumDriver() {
        MobileActionFactory factory = new MobileActionFactory(appiumDriver);
        assertNotNull(factory);
    }

    @Test
    public void testConstructorWithNonAppiumDriverThrows() {
        WebDriver regularDriver = mock(WebDriver.class);
        assertThrows(IllegalArgumentException.class, () -> new MobileActionFactory(regularDriver));
    }

    @Test
    public void testExecActionSendKeys() {
        MobileActionFactory factory = new MobileActionFactory(appiumDriver);
        factory.execAction(element, "hello", MobileAction.SEND_KEYS);
        verify(element).sendKeys("hello");
    }

    @Test
    public void testSwipeDirectionValues() {
        assertEquals(4, MobileActionFactory.SwipeDirection.values().length);
        assertNotNull(MobileActionFactory.SwipeDirection.UP);
        assertNotNull(MobileActionFactory.SwipeDirection.DOWN);
        assertNotNull(MobileActionFactory.SwipeDirection.LEFT);
        assertNotNull(MobileActionFactory.SwipeDirection.RIGHT);
    }
}
