package com.looksee.browser.config;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Configuration properties for BrowserStack integration.
 *
 * <p>Plain Java POJO with no Spring annotations. When used in a Spring
 * application, bind it via a Spring {@code @Bean} method with
 * {@code @ConfigurationProperties(prefix = "browserstack")}. Non-Spring
 * consumers can instantiate it directly and call the setters manually.
 *
 * <p>When {@code accessKey} is configured, BrowserStack is used instead of
 * the default Selenium and Appium URL-based connections.
 *
 * <p>For desktop browsers: {@code os}, {@code osVersion}, {@code browser},
 * {@code browserVersion} apply. For mobile (Appium):
 * {@code deviceName}, {@code realMobile}, and {@code os}/{@code osVersion}
 * apply. BrowserStack determines the platform (Android/iOS) from the
 * {@code BrowserType} passed to {@code getMobileConnection}.
 */
@Getter
@Setter
@NoArgsConstructor
public class BrowserStackProperties {

    /**
     * BrowserStack username.
     */
    private String username;

    /**
     * BrowserStack access key. This is the activation trigger for BrowserStack integration.
     */
    private String accessKey;

    /**
     * Target operating system (e.g., "Windows", "OS X").
     */
    private String os;

    /**
     * Target OS version (e.g., "11", "Ventura").
     */
    private String osVersion;

    /**
     * Browser name override (e.g., "Chrome", "Firefox").
     * When null, the BrowserType from getConnection is used.
     */
    private String browser;

    /**
     * Browser version (e.g., "latest", "120.0").
     */
    private String browserVersion;

    /**
     * BrowserStack project name for organizing tests.
     */
    private String project;

    /**
     * BrowserStack build name for grouping test runs.
     */
    private String build;

    /**
     * BrowserStack session name.
     */
    private String name;

    /**
     * BrowserStack device name for mobile testing (e.g., "Samsung Galaxy S23", "iPhone 15").
     * Used with Appium connections on BrowserStack.
     */
    private String deviceName;

    /**
     * Whether to use a real mobile device on BrowserStack (as opposed to an emulator/simulator).
     * Default is true, since BrowserStack's primary value is real device testing.
     */
    private boolean realMobile = true;

    /**
     * Whether to enable BrowserStack Local for testing internal/staging sites.
     * Default is false.
     */
    private boolean local = false;

    /**
     * Whether to enable BrowserStack debug mode for visual logs.
     * Default is true.
     */
    private boolean debug = true;

    /**
     * Connection timeout in milliseconds.
     * Default is 30000 (30 seconds).
     */
    private int connectionTimeout = 30000;

    /**
     * Maximum number of retry attempts.
     * Default is 3.
     */
    private int maxRetries = 3;
}
