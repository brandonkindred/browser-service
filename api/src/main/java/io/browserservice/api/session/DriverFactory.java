package io.browserservice.api.session;

import com.looksee.browser.Browser;
import com.looksee.browser.MobileDevice;
import com.looksee.browser.enums.BrowserType;

public interface DriverFactory {
  Browser createDesktop(BrowserType type);

  MobileDevice createMobile(BrowserType type);
}
