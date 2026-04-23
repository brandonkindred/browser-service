package com.looksee.browser.config;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Configuration properties for Selenium WebDriver.
 *
 * <p>Plain Java POJO with no Spring annotations. When used in a Spring
 * application, bind it via a Spring {@code @Bean} method with
 * {@code @ConfigurationProperties(prefix = "selenium")}. Non-Spring consumers
 * can instantiate it directly and call the setters manually.
 */
@Getter
@Setter
@NoArgsConstructor
public class SeleniumProperties {

    /**
     * Comma-separated list of Selenium WebDriver hub URLs.
     * Example: "http://selenium-hub:4444/wd/hub,http://localhost:4444/wd/hub"
     */
    private String urls;

    /**
     * Connection timeout for WebDriver connections in milliseconds.
     * Default is 30000 (30 seconds).
     */
    private int connectionTimeout = 30000;

    /**
     * Maximum number of retry attempts for WebDriver connections.
     * Default is 3.
     */
    private int maxRetries = 3;

    /**
     * Whether to enable implicit waits for WebDriver.
     * Default is true.
     */
    private boolean implicitWaitEnabled = true;

    /**
     * Implicit wait timeout in milliseconds.
     * Default is 10000 (10 seconds).
     */
    private int implicitWaitTimeout = 10000;

    /**
     * Gets the WebDriver URLs as an array.
     * @return array of WebDriver URLs, or empty array if urls is null/empty
     */
    public String[] getUrlsArray() {
        if (urls == null || urls.trim().isEmpty()) {
            return new String[0];
        }
        return urls.split(",");
    }
}
