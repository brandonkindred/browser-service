package com.looksee.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.ConstructorBinding;

/**
 * Configuration properties for BrowserStack integration.
 *
 * This allows applications to configure BrowserStack using either:
 * - browserstack.* properties (e.g., browserstack.access-key)
 * - Environment variables (e.g., BROWSERSTACK_ACCESS_KEY)
 *
 * When browserstack.access-key is configured, BrowserStack will be used
 * instead of the default Selenium and Appium URL-based connections.
 *
 * For desktop browsers: os, osVersion, browser, browserVersion apply.
 * For mobile (Appium): deviceName, realMobile, and os/osVersion apply.
 * BrowserStack determines the platform (Android/iOS) from the BrowserType
 * passed to getMobileConnection.
 */
@ConfigurationProperties(prefix = "browserstack")
@ConstructorBinding
public class BrowserStackProperties {

    /**
     * BrowserStack username.
     */
    private final String username;

    /**
     * BrowserStack access key. This is the activation trigger for BrowserStack integration.
     */
    private final String accessKey;

    /**
     * Target operating system (e.g., "Windows", "OS X").
     */
    private final String os;

    /**
     * Target OS version (e.g., "11", "Ventura").
     */
    private final String osVersion;

    /**
     * Browser name override (e.g., "Chrome", "Firefox").
     * When null, the BrowserType from getConnection is used.
     */
    private final String browser;

    /**
     * Browser version (e.g., "latest", "120.0").
     */
    private final String browserVersion;

    /**
     * BrowserStack project name for organizing tests.
     */
    private final String project;

    /**
     * BrowserStack build name for grouping test runs.
     */
    private final String build;

    /**
     * BrowserStack session name.
     */
    private final String name;

    /**
     * BrowserStack device name for mobile testing (e.g., "Samsung Galaxy S23", "iPhone 15").
     * Used with Appium connections on BrowserStack.
     */
    private final String deviceName;

    /**
     * Whether to use a real mobile device on BrowserStack (as opposed to an emulator/simulator).
     * Default is true, since BrowserStack's primary value is real device testing.
     */
    private final boolean realMobile;

    /**
     * Whether to enable BrowserStack Local for testing internal/staging sites.
     * Default is false.
     */
    private final boolean local;

    /**
     * Whether to enable BrowserStack debug mode for visual logs.
     * Default is true.
     */
    private final boolean debug;

    /**
     * Connection timeout in milliseconds.
     * Default is 30000 (30 seconds).
     */
    private final int connectionTimeout;

    /**
     * Maximum number of retry attempts.
     * Default is 3.
     */
    private final int maxRetries;

    public BrowserStackProperties(String username, String accessKey, String os, String osVersion,
                                  String browser, String browserVersion, String project,
                                  String build, String name, String deviceName, Boolean realMobile,
                                  Boolean local, Boolean debug,
                                  Integer connectionTimeout, Integer maxRetries) {
        this.username = username;
        this.accessKey = accessKey;
        this.os = os;
        this.osVersion = osVersion;
        this.browser = browser;
        this.browserVersion = browserVersion;
        this.project = project;
        this.build = build;
        this.name = name;
        this.deviceName = deviceName;
        this.realMobile = realMobile != null ? realMobile : true;
        this.local = local != null ? local : false;
        this.debug = debug != null ? debug : true;
        this.connectionTimeout = connectionTimeout != null ? connectionTimeout : 30000;
        this.maxRetries = maxRetries != null ? maxRetries : 3;
    }

    public String getUsername() {
        return username;
    }

    public String getAccessKey() {
        return accessKey;
    }

    public String getOs() {
        return os;
    }

    public String getOsVersion() {
        return osVersion;
    }

    public String getBrowser() {
        return browser;
    }

    public String getBrowserVersion() {
        return browserVersion;
    }

    public String getProject() {
        return project;
    }

    public String getBuild() {
        return build;
    }

    public String getName() {
        return name;
    }

    public String getDeviceName() {
        return deviceName;
    }

    public boolean isRealMobile() {
        return realMobile;
    }

    public boolean isLocal() {
        return local;
    }

    public boolean isDebug() {
        return debug;
    }

    public int getConnectionTimeout() {
        return connectionTimeout;
    }

    public int getMaxRetries() {
        return maxRetries;
    }
}
