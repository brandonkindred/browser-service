package com.looksee.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.ConstructorBinding;

/**
 * Configuration properties for Selenium WebDriver.
 * 
 * This allows applications to configure Selenium URLs using either:
 * - selenium.* properties (e.g., selenium.urls)
 * - Environment variables (e.g., SELENIUM_URLS)
 */
@ConfigurationProperties(prefix = "selenium")
@ConstructorBinding
public class SeleniumProperties {

    /**
     * Comma-separated list of Selenium WebDriver hub URLs.
     * Example: "http://selenium-hub:4444/wd/hub,http://localhost:4444/wd/hub"
     */
    private final String urls;

    /**
     * Connection timeout for WebDriver connections in milliseconds.
     * Default is 30000 (30 seconds).
     */
    private final int connectionTimeout;

    /**
     * Maximum number of retry attempts for WebDriver connections.
     * Default is 3.
     */
    private final int maxRetries;

    /**
     * Whether to enable implicit waits for WebDriver.
     * Default is true.
     */
    private final boolean implicitWaitEnabled;

    /**
     * Implicit wait timeout in milliseconds.
     * Default is 10000 (10 seconds).
     */
    private final int implicitWaitTimeout;

    /**
     * Constructor for SeleniumProperties
     * 
     * @param urls comma-separated list of Selenium WebDriver hub URLs
     * @param connectionTimeout connection timeout in milliseconds (defaults to 30000)
     * @param maxRetries maximum number of retry attempts (defaults to 3)
     * @param implicitWaitEnabled whether to enable implicit waits (defaults to true)
     * @param implicitWaitTimeout implicit wait timeout in milliseconds (defaults to 10000)
     */
    public SeleniumProperties(String urls, Integer connectionTimeout, Integer maxRetries, 
                             Boolean implicitWaitEnabled, Integer implicitWaitTimeout) {
        this.urls = urls;
        this.connectionTimeout = connectionTimeout != null ? connectionTimeout : 30000;
        this.maxRetries = maxRetries != null ? maxRetries : 3;
        this.implicitWaitEnabled = implicitWaitEnabled != null ? implicitWaitEnabled : true;
        this.implicitWaitTimeout = implicitWaitTimeout != null ? implicitWaitTimeout : 10000;
    }

    /**
     * Gets the Selenium WebDriver hub URLs
     * @return comma-separated list of URLs
     */
    public String getUrls() {
        return urls;
    }

    /**
     * Gets the WebDriver URLs as an array
     * @return array of WebDriver URLs, or empty array if urls is null/empty
     */
    public String[] getUrlsArray() {
        if (urls == null || urls.trim().isEmpty()) {
            return new String[0];
        }
        return urls.split(",");
    }

    /**
     * Gets the connection timeout
     * @return connection timeout in milliseconds
     */
    public int getConnectionTimeout() {
        return connectionTimeout;
    }

    /**
     * Gets the maximum number of retry attempts
     * @return maximum retry attempts
     */
    public int getMaxRetries() {
        return maxRetries;
    }

    /**
     * Checks if implicit waits are enabled
     * @return true if implicit waits are enabled, false otherwise
     */
    public boolean isImplicitWaitEnabled() {
        return implicitWaitEnabled;
    }

    /**
     * Gets the implicit wait timeout
     * @return implicit wait timeout in milliseconds
     */
    public int getImplicitWaitTimeout() {
        return implicitWaitTimeout;
    }
} 