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
 * Configuration class for Appium mobile WebDriver settings.
 * Only created when appium.urls property is configured.
 */
@Configuration
@EnableConfigurationProperties({AppiumProperties.class})
@ConditionalOnProperty(name = "appium.urls")
public class AppiumConfiguration {

    private static final Logger log = LoggerFactory.getLogger(AppiumConfiguration.class);

    private final AppiumProperties appiumProperties;

    public AppiumConfiguration(AppiumProperties appiumProperties) {
        this.appiumProperties = appiumProperties;
        log.info("AppiumConfiguration loaded with URLs: {}", appiumProperties.getUrls());
    }

    @PostConstruct
    public void initializeAppiumUrls() {
        if (appiumProperties.getUrls() != null && !appiumProperties.getUrls().trim().isEmpty()) {
            String[] urls = appiumProperties.getUrlsArray();

            if (urls.length == 0) {
                log.warn("Appium URLs configured but empty after parsing: {}", appiumProperties.getUrls());
                return;
            }

            for (int i = 0; i < urls.length; i++) {
                urls[i] = urls[i].trim();
            }

            log.info("Configuring BrowserConnectionHelper with {} Appium URL(s)", urls.length);
            for (int i = 0; i < urls.length; i++) {
                log.info("  URL {}: {}", i + 1, urls[i]);
            }

            BrowserConnectionHelper.setConfiguredAppiumUrls(urls);

            log.info("Appium configuration completed successfully");
            log.info("   Platform: {}", appiumProperties.getPlatformName());
            log.info("   Device: {}", appiumProperties.getDeviceName());
            log.info("   Automation: {}", appiumProperties.getAutomationName());
            log.info("   Connection timeout: {}ms", appiumProperties.getConnectionTimeout());
            log.info("   Max retries: {}", appiumProperties.getMaxRetries());
        } else {
            log.warn("AppiumConfiguration created but no valid URLs provided");
        }
    }

    public AppiumProperties getAppiumProperties() {
        return appiumProperties;
    }

    @Bean
    public ApplicationListener<ApplicationReadyEvent> appiumDiagnosticListener(Environment environment) {
        return event -> {
            log.info("=== Appium Configuration Diagnostic ===");

            String urls = environment.getProperty("appium.urls");
            log.info("appium.urls: {}", urls != null ? (urls.isEmpty() ? "<EMPTY>" : urls) : "<NULL>");
            log.info("appium.platformName: {}", environment.getProperty("appium.platformName", "<DEFAULT>"));
            log.info("appium.deviceName: {}", environment.getProperty("appium.deviceName", "<DEFAULT>"));
            log.info("appium.automationName: {}", environment.getProperty("appium.automationName", "<DEFAULT>"));

            if (urls != null && !urls.trim().isEmpty()) {
                String[] urlArray = urls.split(",");
                log.info("Appium configuration ENABLED with {} URL(s)", urlArray.length);
                for (int i = 0; i < urlArray.length; i++) {
                    log.info("   Server {}: {}", i + 1, urlArray[i].trim());
                }
            } else {
                log.warn("Appium URLs not configured - Mobile automation not available");
            }

            log.info("=== End Appium Configuration Diagnostic ===");
        };
    }
}
