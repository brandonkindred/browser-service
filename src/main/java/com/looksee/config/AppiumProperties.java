package com.looksee.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.ConstructorBinding;

/**
 * Configuration properties for Appium mobile WebDriver.
 *
 * This allows applications to configure Appium using either:
 * - appium.* properties (e.g., appium.urls)
 * - Environment variables (e.g., APPIUM_URLS)
 */
@ConfigurationProperties(prefix = "appium")
@ConstructorBinding
public class AppiumProperties {

    /**
     * Comma-separated list of Appium server URLs.
     * Example: "appium-server-1:4723,appium-server-2:4723"
     */
    private final String urls;

    /**
     * Target platform name (e.g., "Android" or "iOS").
     */
    private final String platformName;

    /**
     * Target device name (e.g., "Pixel 6", "iPhone 14", "Android Emulator").
     */
    private final String deviceName;

    /**
     * Browser name for mobile web testing (e.g., "Chrome" for Android, "Safari" for iOS).
     * Leave null/empty for native app testing when {@code app} is set.
     */
    private final String browserName;

    /**
     * Automation engine name (e.g., "UiAutomator2" for Android, "XCUITest" for iOS).
     */
    private final String automationName;

    /**
     * Target platform version (e.g., "13.0").
     */
    private final String platformVersion;

    /**
     * Path or URL to the application (.apk/.ipa) for native app testing.
     * Leave null/empty for mobile web testing.
     */
    private final String app;

    /**
     * Connection timeout for Appium connections in milliseconds.
     * Default is 60000 (60 seconds) since mobile sessions take longer to start.
     */
    private final int connectionTimeout;

    /**
     * Maximum number of retry attempts for Appium connections.
     * Default is 3.
     */
    private final int maxRetries;

    public AppiumProperties(String urls, String platformName, String deviceName,
                            String browserName, String automationName, String platformVersion,
                            String app, Integer connectionTimeout, Integer maxRetries) {
        this.urls = urls;
        this.platformName = platformName;
        this.deviceName = deviceName;
        this.browserName = browserName;
        this.automationName = automationName;
        this.platformVersion = platformVersion;
        this.app = app;
        this.connectionTimeout = connectionTimeout != null ? connectionTimeout : 60000;
        this.maxRetries = maxRetries != null ? maxRetries : 3;
    }

    public String getUrls() {
        return urls;
    }

    public String[] getUrlsArray() {
        if (urls == null || urls.trim().isEmpty()) {
            return new String[0];
        }
        return urls.split(",");
    }

    public String getPlatformName() {
        return platformName;
    }

    public String getDeviceName() {
        return deviceName;
    }

    public String getBrowserName() {
        return browserName;
    }

    public String getAutomationName() {
        return automationName;
    }

    public String getPlatformVersion() {
        return platformVersion;
    }

    public String getApp() {
        return app;
    }

    public int getConnectionTimeout() {
        return connectionTimeout;
    }

    public int getMaxRetries() {
        return maxRetries;
    }
}
