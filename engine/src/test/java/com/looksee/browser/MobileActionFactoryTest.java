package com.looksee.browser;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.looksee.browser.enums.MobileAction;
import io.appium.java_client.PerformsTouchActions;
import io.appium.java_client.TouchAction;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.remote.RemoteWebElement;

public class MobileActionFactoryTest {

    // Mock driver that implements both WebDriver and PerformsTouchActions
    interface MockAppiumDriver extends WebDriver, PerformsTouchActions {}

    @Mock
    private MockAppiumDriver appiumDriver;

    @Mock
    private RemoteWebElement element;

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
        when(element.getId()).thenReturn("element-42");
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
    public void testExecActionTap() {
        MobileActionFactory factory = new MobileActionFactory(appiumDriver);
        factory.execAction(element, "", MobileAction.TAP);
        verify(appiumDriver).performTouchAction(any());
    }

    @Test
    public void testExecActionDoubleTapPerformsTwice() {
        MobileActionFactory factory = new MobileActionFactory(appiumDriver);
        factory.execAction(element, "", MobileAction.DOUBLE_TAP);
        verify(appiumDriver, org.mockito.Mockito.times(2)).performTouchAction(any());
    }

    @Test
    public void testExecActionLongPress() {
        MobileActionFactory factory = new MobileActionFactory(appiumDriver);
        factory.execAction(element, "", MobileAction.LONG_PRESS);
        verify(appiumDriver).performTouchAction(any());
    }

    @Test
    public void testExecActionSendKeys() {
        MobileActionFactory factory = new MobileActionFactory(appiumDriver);
        factory.execAction(element, "hello", MobileAction.SEND_KEYS);
        verify(element).sendKeys("hello");
    }

    @Test
    public void testExecActionSwipeUp() {
        MobileActionFactory factory = new MobileActionFactory(appiumDriver);
        factory.execAction(element, "", MobileAction.SWIPE_UP);
        verify(appiumDriver).performTouchAction(any());
    }

    @Test
    public void testExecActionSwipeDown() {
        MobileActionFactory factory = new MobileActionFactory(appiumDriver);
        factory.execAction(element, "", MobileAction.SWIPE_DOWN);
        verify(appiumDriver).performTouchAction(any());
    }

    @Test
    public void testExecActionSwipeLeft() {
        MobileActionFactory factory = new MobileActionFactory(appiumDriver);
        factory.execAction(element, "", MobileAction.SWIPE_LEFT);
        verify(appiumDriver).performTouchAction(any());
    }

    @Test
    public void testExecActionSwipeRight() {
        MobileActionFactory factory = new MobileActionFactory(appiumDriver);
        factory.execAction(element, "", MobileAction.SWIPE_RIGHT);
        verify(appiumDriver).performTouchAction(any());
    }

    @Test
    public void testExecActionScrollUp() {
        MobileActionFactory factory = new MobileActionFactory(appiumDriver);
        factory.execAction(element, "", MobileAction.SCROLL_UP);
        verify(appiumDriver).performTouchAction(any());
    }

    @Test
    public void testExecActionScrollDown() {
        MobileActionFactory factory = new MobileActionFactory(appiumDriver);
        factory.execAction(element, "", MobileAction.SCROLL_DOWN);
        verify(appiumDriver).performTouchAction(any());
    }

    @Test
    public void testExecActionUnknownLogsAndSkips() {
        MobileActionFactory factory = new MobileActionFactory(appiumDriver);
        assertDoesNotThrow(() -> factory.execAction(element, "", MobileAction.UNKNOWN));
        verify(appiumDriver, org.mockito.Mockito.never()).performTouchAction(any());
    }

    @Test
    public void testTapDirectly() {
        MobileActionFactory factory = new MobileActionFactory(appiumDriver);
        factory.tap(element);
        verify(appiumDriver).performTouchAction(any());
    }

    @Test
    public void testLongPressDirectly() {
        MobileActionFactory factory = new MobileActionFactory(appiumDriver);
        factory.longPress(element);
        verify(appiumDriver).performTouchAction(any());
    }

    @Test
    public void testSwipeScreenAllDirections() {
        MobileActionFactory factory = new MobileActionFactory(appiumDriver);
        for (MobileActionFactory.SwipeDirection dir : MobileActionFactory.SwipeDirection.values()) {
            factory.swipeScreen(dir);
        }
        verify(appiumDriver, org.mockito.Mockito.times(4)).performTouchAction(any());
    }

    @Test
    public void testSwipeFromElementAllDirections() {
        MobileActionFactory factory = new MobileActionFactory(appiumDriver);
        for (MobileActionFactory.SwipeDirection dir : MobileActionFactory.SwipeDirection.values()) {
            factory.swipeFromElement(element, dir);
        }
        verify(appiumDriver, org.mockito.Mockito.times(4)).performTouchAction(any());
    }

    @Test
    public void testSwipeDirectionValues() {
        org.junit.jupiter.api.Assertions.assertEquals(4, MobileActionFactory.SwipeDirection.values().length);
        assertNotNull(MobileActionFactory.SwipeDirection.UP);
        assertNotNull(MobileActionFactory.SwipeDirection.DOWN);
        assertNotNull(MobileActionFactory.SwipeDirection.LEFT);
        assertNotNull(MobileActionFactory.SwipeDirection.RIGHT);
    }

    @Test
    public void testTouchActionConstructableWithMock() {
        // Sanity check that our mock implements PerformsTouchActions
        org.junit.jupiter.api.Assertions.assertNotNull(new TouchAction<>(appiumDriver));
    }
}
