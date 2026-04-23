package com.looksee.browser.config;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Configuration properties for Appium mobile WebDriver.
 *
 * <p>Plain Java POJO with no Spring annotations. When used in a Spring
 * application, bind it via a Spring {@code @Bean} method with
 * {@code @ConfigurationProperties(prefix = "appium")}. Non-Spring consumers
 * can instantiate it directly and call the setters manually.
 */
@Getter
@Setter
@NoArgsConstructor
public class AppiumProperties {

    /**
     * Comma-separated list of Appium server URLs.
     * Example: "appium-server-1:4723,appium-server-2:4723"
     */
    private String urls;

    /**
     * Target platform name (e.g., "Android" or "iOS").
     */
    private String platformName;

    /**
     * Target device name (e.g., "Pixel 6", "iPhone 14", "Android Emulator").
     */
    private String deviceName;

    /**
     * Browser name for mobile web testing (e.g., "Chrome" for Android, "Safari" for iOS).
     * Leave null/empty for native app testing when {@code app} is set.
     */
    private String browserName;

    /**
     * Automation engine name (e.g., "UiAutomator2" for Android, "XCUITest" for iOS).
     */
    private String automationName;

    /**
     * Target platform version (e.g., "13.0").
     */
    private String platformVersion;

    /**
     * Path or URL to the application (.apk/.ipa) for native app testing.
     * Leave null/empty for mobile web testing.
     */
    private String app;

    /**
     * Connection timeout for Appium connections in milliseconds.
     * Default is 60000 (60 seconds) since mobile sessions take longer to start.
     */
    private int connectionTimeout = 60000;

    /**
     * Maximum number of retry attempts for Appium connections.
     * Default is 3.
     */
    private int maxRetries = 3;

    /**
     * Gets the Appium server URLs as an array.
     * @return array of Appium server URLs, or empty array if urls is null/empty
     */
    public String[] getUrlsArray() {
        if (urls == null || urls.trim().isEmpty()) {
            return new String[0];
        }
        return urls.split(",");
    }
}
