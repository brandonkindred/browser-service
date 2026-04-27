package io.browserservice.api.session;

import com.looksee.browser.Browser;
import com.looksee.browser.MobileDevice;
import com.looksee.browser.enums.BrowserEnvironment;
import com.looksee.browser.enums.BrowserType;
import com.looksee.browser.helpers.BrowserConnectionHelper;
import io.browserservice.api.error.UpstreamUnavailableException;
import org.springframework.stereotype.Component;

@Component
public class BrowserConnectionHelperDriverFactory implements DriverFactory {

  @Override
  public Browser createDesktop(BrowserType type, BrowserEnvironment env) {
    try {
      return BrowserConnectionHelper.getConnection(type, env);
    } catch (Throwable e) {
      throw new UpstreamUnavailableException(
          "could not allocate desktop session: " + e.getMessage(), e);
    }
  }

  @Override
  public MobileDevice createMobile(BrowserType type, BrowserEnvironment env) {
    try {
      return BrowserConnectionHelper.getMobileConnection(type, env);
    } catch (Throwable e) {
      throw new UpstreamUnavailableException(
          "could not allocate mobile session: " + e.getMessage(), e);
    }
  }
}
