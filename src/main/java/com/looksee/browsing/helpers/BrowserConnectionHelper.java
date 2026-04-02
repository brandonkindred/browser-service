package com.looksee.browsing.helpers;

import com.looksee.browsing.BrowserFactory;
import com.looksee.models.Browser;
import com.looksee.browsing.enums.BrowserEnvironment;
import com.looksee.browsing.enums.BrowserType;
import io.github.resilience4j.retry.annotation.Retry;
import java.net.MalformedURLException;
import java.net.URL;
import lombok.NoArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A helper class for creating a {@link Browser} connection
 */
@NoArgsConstructor
@Retry(name="webdriver")
public class BrowserConnectionHelper {
	/**
	 * The logger for the {@link BrowserConnectionHelper} class
	 */
	@SuppressWarnings("unused")
	private static Logger log = LoggerFactory.getLogger(BrowserConnectionHelper.class);
	
	/**
	 * The index of the selenium hub
	 */
	private static int SELENIUM_HUB_IDX = 0;

	private static String[] HUB_URLS;
	
	/**
	 * Gets the selenium hub URLs, either from environment variable SELENIUM_URLS or fallback to hardcoded list
	 * @param urls the selenium hub URLs
	 *
	 * precondition: urls != null
	 */
	public static void setConfiguredSeleniumUrls(String[] urls) {
		assert urls != null;
		HUB_URLS=urls;
	}

	/**
	 * Creates a {@link Browser} connection
	 *
	 * @param browser the browser to connect to
	 * @param environment the environment to connect to
	 *
	 * @return the browser connection
	 *
	 * precondition: browser != null
	 * precondition: environment != null
	 *
	 * @throws MalformedURLException if the url is malformed
	 */
    @Retry(name="webdriver")
	public static Browser getConnection(BrowserType browser, BrowserEnvironment environment)
			throws MalformedURLException
    {
		assert browser != null;
		assert environment != null;

		URL hub_url = null;
		
		if(environment.equals(BrowserEnvironment.DISCOVERY) && "chrome".equalsIgnoreCase(browser.toString())){
			hub_url = new URL( "https://"+HUB_URLS[SELENIUM_HUB_IDX%HUB_URLS.length]+"/wd/hub");
		}
		else if(environment.equals(BrowserEnvironment.DISCOVERY) && "firefox".equalsIgnoreCase(browser.toString())){
			hub_url = new URL( "https://"+HUB_URLS[SELENIUM_HUB_IDX%HUB_URLS.length]+"/wd/hub");
		}
		SELENIUM_HUB_IDX++;

		return BrowserFactory.createBrowser(browser.toString(), hub_url);
	}
}
