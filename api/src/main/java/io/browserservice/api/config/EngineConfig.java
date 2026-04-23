package io.browserservice.api.config;

import com.looksee.browser.config.AppiumProperties;
import com.looksee.browser.config.BrowserStackProperties;
import com.looksee.browser.config.SeleniumProperties;
import com.looksee.browser.helpers.BrowserConnectionHelper;
import jakarta.annotation.PostConstruct;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class EngineConfig {

    private final EngineProperties props;

    public EngineConfig(EngineProperties props) {
        this.props = props;
    }

    @Bean
    public SeleniumProperties seleniumProperties() {
        var sp = new SeleniumProperties();
        sp.setUrls(props.selenium().urls());
        sp.setConnectionTimeout(props.selenium().connectTimeoutMs());
        sp.setMaxRetries(props.selenium().maxRetries());
        sp.setImplicitWaitEnabled(props.selenium().implicitWaitEnabled());
        sp.setImplicitWaitTimeout(props.selenium().implicitWaitSeconds());
        return sp;
    }

    @Bean
    public AppiumProperties appiumProperties() {
        var ap = new AppiumProperties();
        ap.setUrls(props.appium().urls());
        ap.setPlatformName(props.appium().platform());
        ap.setDeviceName(props.appium().deviceName());
        ap.setConnectionTimeout(props.appium().connectTimeoutMs());
        ap.setMaxRetries(props.appium().maxRetries());
        return ap;
    }

    @Bean
    public BrowserStackProperties browserStackProperties() {
        var bs = new BrowserStackProperties();
        bs.setUsername(props.browserstack().username());
        bs.setAccessKey(props.browserstack().accessKey());
        bs.setOs(props.browserstack().os());
        bs.setOsVersion(props.browserstack().osVersion());
        bs.setBrowser(props.browserstack().browser());
        bs.setBrowserVersion(props.browserstack().browserVersion());
        bs.setProject(props.browserstack().project());
        bs.setBuild(props.browserstack().build());
        bs.setName(props.browserstack().name());
        bs.setDeviceName(props.browserstack().deviceName());
        bs.setRealMobile(props.browserstack().realMobile());
        bs.setLocal(props.browserstack().local());
        bs.setDebug(props.browserstack().debug());
        return bs;
    }

    @PostConstruct
    public void seedConnectionHelper() {
        String seleniumUrls = props.selenium().urls();
        if (seleniumUrls != null && !seleniumUrls.isBlank()) {
            BrowserConnectionHelper.setConfiguredSeleniumUrls(splitUrls(seleniumUrls));
        }

        String appiumUrls = props.appium().urls();
        if (appiumUrls != null && !appiumUrls.isBlank()) {
            BrowserConnectionHelper.setConfiguredAppiumUrls(splitUrls(appiumUrls));
        }

        if (props.browserstack().enabled()
                && props.browserstack().hubUrl() != null
                && !props.browserstack().hubUrl().isBlank()) {
            BrowserConnectionHelper.setBrowserStackConfig(
                    props.browserstack().hubUrl(), browserStackProperties());
        } else {
            BrowserConnectionHelper.clearBrowserStackConfig();
        }
    }

    private static String[] splitUrls(String commaSeparated) {
        return commaSeparated.split("\\s*,\\s*");
    }
}
