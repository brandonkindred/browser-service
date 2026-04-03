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
 * Configuration class for BrowserStack integration.
 * Only created when browserstack.access-key property is configured.
 *
 * When active, BrowserStack is used instead of the default Selenium and Appium URL-based connections.
 *
 * This configuration uses BrowserStackProperties to load values from either:
 * - application.properties: browserstack.access-key, browserstack.username, etc.
 * - Environment variables: BROWSERSTACK_ACCESS_KEY, BROWSERSTACK_USERNAME, etc.
 */
@Configuration
@EnableConfigurationProperties({BrowserStackProperties.class})
@ConditionalOnProperty(name = "browserstack.access-key")
public class BrowserStackConfiguration {

    private static final Logger log = LoggerFactory.getLogger(BrowserStackConfiguration.class);

    private static final String BROWSERSTACK_HUB_URL = "https://hub-cloud.browserstack.com/wd/hub";

    private final BrowserStackProperties browserStackProperties;

    public BrowserStackConfiguration(BrowserStackProperties browserStackProperties) {
        this.browserStackProperties = browserStackProperties;
        log.info("BrowserStackConfiguration loaded for user: {}", browserStackProperties.getUsername());
    }

    @PostConstruct
    public void initializeBrowserStack() {
        String username = browserStackProperties.getUsername();
        String accessKey = browserStackProperties.getAccessKey();

        if (accessKey == null || accessKey.trim().isEmpty()) {
            log.warn("BrowserStack access key is empty after parsing");
            return;
        }

        if (username == null || username.trim().isEmpty()) {
            log.warn("BrowserStack username is not configured. Set browserstack.username property.");
            return;
        }

        log.info("Configuring BrowserConnectionHelper with BrowserStack");
        log.info("  Hub URL: {}", BROWSERSTACK_HUB_URL);
        log.info("  Username: {}", username);
        log.info("  Access Key: {}...", accessKey.substring(0, Math.min(4, accessKey.length())));

        BrowserConnectionHelper.setBrowserStackConfig(BROWSERSTACK_HUB_URL, browserStackProperties);

        log.info("BrowserStack configuration completed successfully");
        log.info("   Project: {}", browserStackProperties.getProject());
        log.info("   Build: {}", browserStackProperties.getBuild());
        log.info("   OS: {} {}", browserStackProperties.getOs(), browserStackProperties.getOsVersion());
        log.info("   Device: {}", browserStackProperties.getDeviceName());
        log.info("   Real Mobile: {}", browserStackProperties.isRealMobile());
        log.info("   Local: {}", browserStackProperties.isLocal());
        log.info("   Debug: {}", browserStackProperties.isDebug());
        log.info("   Connection timeout: {}ms", browserStackProperties.getConnectionTimeout());
        log.info("   Max retries: {}", browserStackProperties.getMaxRetries());
    }

    public BrowserStackProperties getBrowserStackProperties() {
        return browserStackProperties;
    }

    @Bean
    public ApplicationListener<ApplicationReadyEvent> browserStackDiagnosticListener(Environment environment) {
        return event -> {
            log.info("=== BrowserStack Configuration Diagnostic ===");

            String accessKey = environment.getProperty("browserstack.access-key");
            String username = environment.getProperty("browserstack.username");

            log.info("browserstack.username: {}", username != null ? username : "<NULL>");
            log.info("browserstack.access-key: {}", accessKey != null ? "****" : "<NULL>");
            log.info("browserstack.os: {}", environment.getProperty("browserstack.os", "<DEFAULT>"));
            log.info("browserstack.os-version: {}", environment.getProperty("browserstack.os-version", "<DEFAULT>"));
            log.info("browserstack.project: {}", environment.getProperty("browserstack.project", "<DEFAULT>"));
            log.info("browserstack.build: {}", environment.getProperty("browserstack.build", "<DEFAULT>"));
            log.info("browserstack.device-name: {}", environment.getProperty("browserstack.device-name", "<DEFAULT>"));
            log.info("browserstack.real-mobile: {}", environment.getProperty("browserstack.real-mobile", "true"));
            log.info("browserstack.local: {}", environment.getProperty("browserstack.local", "false"));
            log.info("browserstack.debug: {}", environment.getProperty("browserstack.debug", "true"));

            if (accessKey != null && !accessKey.trim().isEmpty()
                    && username != null && !username.trim().isEmpty()) {
                log.info("BrowserStack configuration ENABLED");
                log.info("   BrowserConnectionHelper will use BrowserStack instead of Selenium/Appium hubs");
            } else {
                log.warn("BrowserStack partially configured - both username and access-key are required");
            }

            log.info("=== End BrowserStack Configuration Diagnostic ===");
        };
    }
}
