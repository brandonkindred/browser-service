package com.looksee.config;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import com.looksee.browsing.helpers.BrowserConnectionHelper;

/**
 * Configuration class for Selenium WebDriver settings.
 * Only created when selenium.urls property is configured.
 * 
 * This configuration uses SeleniumProperties to load values from either:
 * - application.properties: selenium.urls, selenium.connectionTimeout, etc.
 * - Environment variables: SELENIUM_URLS, SELENIUM_CONNECTION_TIMEOUT, etc.
 */
@Configuration
@EnableConfigurationProperties({SeleniumProperties.class})
@ConditionalOnProperty(name = "selenium.urls")
public class SeleniumConfiguration {
    
    private static final Logger log = LoggerFactory.getLogger(SeleniumConfiguration.class);
    
    private final SeleniumProperties seleniumProperties;
    
    /**
     * Constructor for SeleniumConfiguration
     * 
     * @param seleniumProperties the Selenium configuration properties
     */
    public SeleniumConfiguration(SeleniumProperties seleniumProperties) {
        this.seleniumProperties = seleniumProperties;
        log.info("🔧 SeleniumConfiguration loaded with URLs: {}", seleniumProperties.getUrls());
    }
    
    /**
     * Initialize the BrowserConnectionHelper with configured selenium URLs
     */
    @PostConstruct
    public void initializeSeleniumUrls() {
        if (seleniumProperties.getUrls() != null && !seleniumProperties.getUrls().trim().isEmpty()) {
            String[] urls = seleniumProperties.getUrlsArray();
            
            // Validate URLs
            if (urls.length == 0) {
                log.warn("Selenium URLs configured but empty after parsing: {}", seleniumProperties.getUrls());
                return;
            }
            
            // Trim whitespace from URLs
            for (int i = 0; i < urls.length; i++) {
                urls[i] = urls[i].trim();
            }
            
            log.info("Configuring BrowserConnectionHelper with {} Selenium URL(s)", urls.length);
            for (int i = 0; i < urls.length; i++) {
                log.info("  URL {}: {}", i + 1, urls[i]);
            }
            
            BrowserConnectionHelper.setConfiguredSeleniumUrls(urls);
            
            log.info("✅ Selenium WebDriver configuration completed successfully");
            log.info("   Connection timeout: {}ms", seleniumProperties.getConnectionTimeout());
            log.info("   Max retries: {}", seleniumProperties.getMaxRetries());
            log.info("   Implicit wait enabled: {}", seleniumProperties.isImplicitWaitEnabled());
            if (seleniumProperties.isImplicitWaitEnabled()) {
                log.info("   Implicit wait timeout: {}ms", seleniumProperties.getImplicitWaitTimeout());
            }
        } else {
            log.warn("SeleniumConfiguration created but no valid URLs provided");
        }
    }
    
    /**
     * Gets the configured SeleniumProperties
     * @return the selenium properties
     */
    public SeleniumProperties getSeleniumProperties() {
        return seleniumProperties;
    }
    
    /**
     * Diagnostic bean to log Selenium configuration status during application startup.
     * This helps identify Selenium configuration issues.
     */
    @Bean
    public ApplicationListener<ApplicationReadyEvent> seleniumDiagnosticListener(Environment environment) {
        return event -> {
            log.info("=== Selenium Configuration Diagnostic ===");
            
            String urls = environment.getProperty("selenium.urls");
            String connectionTimeout = environment.getProperty("selenium.connectionTimeout");
            String maxRetries = environment.getProperty("selenium.maxRetries");
            String implicitWaitEnabled = environment.getProperty("selenium.implicitWaitEnabled");
            String implicitWaitTimeout = environment.getProperty("selenium.implicitWaitTimeout");
            
            log.info("selenium.urls: {}", urls != null ? (urls.isEmpty() ? "<EMPTY>" : urls) : "<NULL>");
            log.info("selenium.connectionTimeout: {} (default: 30000ms)", 
                     connectionTimeout != null ? connectionTimeout : "<DEFAULT>");
            log.info("selenium.maxRetries: {} (default: 3)", 
                     maxRetries != null ? maxRetries : "<DEFAULT>");
            log.info("selenium.implicitWaitEnabled: {} (default: true)", 
                     implicitWaitEnabled != null ? implicitWaitEnabled : "<DEFAULT>");
            log.info("selenium.implicitWaitTimeout: {} (default: 10000ms)", 
                     implicitWaitTimeout != null ? implicitWaitTimeout : "<DEFAULT>");
            
            if (urls != null && !urls.trim().isEmpty()) {
                String[] urlArray = urls.split(",");
                log.info("✅ Selenium configuration ENABLED with {} URL(s)", urlArray.length);
                for (int i = 0; i < urlArray.length; i++) {
                    log.info("   Hub {}: {}", i + 1, urlArray[i].trim());
                }
                log.info("✅ BrowserConnectionHelper will use configured Selenium hubs");
            } else {
                log.warn("⚠️  Selenium URLs not configured - Browser automation may not work");
                log.warn("💡 To enable Selenium, set: SELENIUM_URLS or selenium.urls property");
            }
            
            log.info("=== End Selenium Configuration Diagnostic ===");
        };
    }
} 