package com.looksee.browsing;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

/**
 * Test class for BrowserConnectionHelper
 */
public class BrowserConnectionHelperTest {
    
    private String originalSeleniumUrls;
    
    @BeforeEach
    public void setUp() {
        // Store the original environment variable value
        originalSeleniumUrls = System.getenv("SELENIUM_URLS");
    }
    
    @AfterEach
    public void tearDown() {
        // Restore the original environment variable value
        if (originalSeleniumUrls != null) {
            System.setProperty("SELENIUM_URLS", originalSeleniumUrls);
        } else {
            System.clearProperty("SELENIUM_URLS");
        }
    }
}