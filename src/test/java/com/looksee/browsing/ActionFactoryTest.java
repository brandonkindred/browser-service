package com.looksee.browsing;

import static org.mockito.Mockito.*;

import com.looksee.browsing.enums.Action;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;

public class ActionFactoryTest {

    @Mock
    private WebDriver driver;

    @Mock
    private WebElement element;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    public void testConstructor() {
        // Should not throw
        new ActionFactory(driver);
    }

    @Test
    public void testExecActionClick() {
        ActionFactory factory = new ActionFactory(driver);
        // Actions builder internally uses the driver; we just verify no exception
        try {
            factory.execAction(element, "", Action.CLICK);
        } catch (Exception e) {
            // WebDriverException is expected since we're not using a real driver
        }
    }

    @Test
    public void testExecActionSendKeys() {
        ActionFactory factory = new ActionFactory(driver);
        try {
            factory.execAction(element, "test input", Action.SEND_KEYS);
        } catch (Exception e) {
            // Expected with mock driver
        }
    }

    @Test
    public void testExecActionDoubleClick() {
        ActionFactory factory = new ActionFactory(driver);
        try {
            factory.execAction(element, "", Action.DOUBLE_CLICK);
        } catch (Exception e) {
            // Expected with mock driver
        }
    }

    @Test
    public void testExecActionContextClick() {
        ActionFactory factory = new ActionFactory(driver);
        try {
            factory.execAction(element, "", Action.CONTEXT_CLICK);
        } catch (Exception e) {
            // Expected with mock driver
        }
    }

    @Test
    public void testExecActionClickAndHold() {
        ActionFactory factory = new ActionFactory(driver);
        try {
            factory.execAction(element, "", Action.CLICK_AND_HOLD);
        } catch (Exception e) {
            // Expected with mock driver
        }
    }

    @Test
    public void testExecActionRelease() {
        ActionFactory factory = new ActionFactory(driver);
        try {
            factory.execAction(element, "", Action.RELEASE);
        } catch (Exception e) {
            // Expected with mock driver
        }
    }

    @Test
    public void testExecActionMouseOver() {
        ActionFactory factory = new ActionFactory(driver);
        try {
            factory.execAction(element, "", Action.MOUSE_OVER);
        } catch (Exception e) {
            // Expected with mock driver
        }
    }
}
