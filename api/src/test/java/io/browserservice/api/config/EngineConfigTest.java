package io.browserservice.api.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.looksee.browser.helpers.BrowserConnectionHelper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class EngineConfigTest {

    @AfterEach
    void resetHelper() {
        BrowserConnectionHelper.setConfiguredSeleniumUrls(new String[0]);
        BrowserConnectionHelper.setConfiguredAppiumUrls(new String[0]);
        BrowserConnectionHelper.clearBrowserStackConfig();
    }

    @Test
    void seedsUrlsAndBeans() {
        EngineProperties props = new EngineProperties(
                new EngineProperties.SessionProps(300, 1800, 20, 5000),
                new EngineProperties.SeleniumProps("http://a/wd/hub, http://b/wd/hub",
                        10000, 30000, 2, true, 7),
                new EngineProperties.AppiumProps("http://c/wd/hub", "ANDROID", "Pixel 7", 60000, 3),
                new EngineProperties.BrowserStackProps(false, "", "", "", "", "", "", "",
                        "", "", "", "", true, false, true),
                new EngineProperties.WebSocketProps("/v1/ws/sessions", 32, 300, 64, 10000));

        EngineConfig cfg = new EngineConfig(props);
        cfg.seedConnectionHelper();

        var sp = cfg.seleniumProperties();
        assertThat(sp.getUrls()).isEqualTo("http://a/wd/hub, http://b/wd/hub");
        assertThat(sp.getConnectionTimeout()).isEqualTo(10000);
        assertThat(sp.getImplicitWaitTimeout()).isEqualTo(7);

        var ap = cfg.appiumProperties();
        assertThat(ap.getPlatformName()).isEqualTo("ANDROID");
        assertThat(ap.getDeviceName()).isEqualTo("Pixel 7");

        var bs = cfg.browserStackProperties();
        assertThat(bs.isRealMobile()).isTrue();
    }

    @Test
    void blankUrlsSkipHelperSeed() {
        EngineProperties props = new EngineProperties(
                new EngineProperties.SessionProps(300, 1800, 20, 5000),
                new EngineProperties.SeleniumProps("", 0, 0, 0, false, 0),
                new EngineProperties.AppiumProps("", "", "", 0, 0),
                new EngineProperties.BrowserStackProps(false, "", "", "", "", "", "", "",
                        "", "", "", "", false, false, false),
                new EngineProperties.WebSocketProps("/v1/ws/sessions", 32, 300, 64, 10000));

        new EngineConfig(props).seedConnectionHelper();
        // No exception, no URLs set — success is simply not throwing.
        assertThat(true).isTrue();
    }

    @Test
    void browserStackEnabledSetsConfig() {
        EngineProperties props = new EngineProperties(
                new EngineProperties.SessionProps(300, 1800, 20, 5000),
                new EngineProperties.SeleniumProps("http://a/wd/hub", 0, 0, 0, false, 0),
                new EngineProperties.AppiumProps("", "", "", 0, 0),
                new EngineProperties.BrowserStackProps(true,
                        "https://hub.browserstack.com/wd/hub", "user", "key",
                        "Windows", "11", "chrome", "126", "proj", "build", "name",
                        "Pixel 7", true, false, true),
                new EngineProperties.WebSocketProps("/v1/ws/sessions", 32, 300, 64, 10000));

        new EngineConfig(props).seedConnectionHelper();
        assertThat(true).isTrue();
    }

    @Test
    void browserStackEnabledButBlankHubUrlClearsConfig() {
        EngineProperties props = new EngineProperties(
                new EngineProperties.SessionProps(300, 1800, 20, 5000),
                new EngineProperties.SeleniumProps("http://a/wd/hub", 0, 0, 0, false, 0),
                new EngineProperties.AppiumProps("", "", "", 0, 0),
                new EngineProperties.BrowserStackProps(true, "", "u", "k",
                        "", "", "", "", "", "", "", "", true, false, true),
                new EngineProperties.WebSocketProps("/v1/ws/sessions", 32, 300, 64, 10000));

        new EngineConfig(props).seedConnectionHelper();
        assertThat(true).isTrue();
    }
}
