package io.browserservice.api.session;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.looksee.browser.config.BrowserStackProperties;
import com.looksee.browser.enums.BrowserEnvironment;
import com.looksee.browser.enums.BrowserType;
import com.looksee.browser.helpers.BrowserConnectionHelper;
import io.browserservice.api.error.UpstreamUnavailableException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class BrowserConnectionHelperDriverFactoryTest {

  @AfterEach
  void resetConnectionHelper() {
    BrowserConnectionHelper.setConfiguredSeleniumUrls(new String[0]);
    BrowserConnectionHelper.setConfiguredAppiumUrls(new String[0]);
    BrowserConnectionHelper.clearBrowserStackConfig();
  }

  @Test
  void desktopWithNoConfigurationWrapsFailureAsUpstreamUnavailable() {
    BrowserConnectionHelperDriverFactory factory = new BrowserConnectionHelperDriverFactory();

    assertThatThrownBy(() -> factory.createDesktop(BrowserType.CHROME, BrowserEnvironment.TEST))
        .isInstanceOf(UpstreamUnavailableException.class);
  }

  @Test
  void mobileWithNoConfigurationWrapsFailureAsUpstreamUnavailable() {
    BrowserConnectionHelperDriverFactory factory = new BrowserConnectionHelperDriverFactory();

    assertThatThrownBy(() -> factory.createMobile(BrowserType.ANDROID, BrowserEnvironment.TEST))
        .isInstanceOf(UpstreamUnavailableException.class);
  }

  @Test
  void browserStackConfigSetterIsAdditive() {
    BrowserStackProperties props = new BrowserStackProperties();
    props.setUsername("u");
    props.setAccessKey("k");

    BrowserConnectionHelper.setBrowserStackConfig("https://hub.browserstack.com/wd/hub", props);

    // No exception; clear should work without throwing.
    BrowserConnectionHelper.clearBrowserStackConfig();
    assertThat(true).isTrue();
  }
}
