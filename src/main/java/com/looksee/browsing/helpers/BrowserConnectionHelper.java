package com.looksee.browsing.helpers;

import com.looksee.browsing.BrowserFactory;
import com.looksee.browsing.MobileFactory;
import com.looksee.config.BrowserStackProperties;
import com.looksee.models.Browser;
import com.looksee.models.MobileDevice;
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
	 * The index of the Appium server for round-robin selection
	 */
	private static int APPIUM_SERVER_IDX = 0;

	private static String[] APPIUM_URLS;

	/**
	 * Whether BrowserStack is enabled as the connection provider
	 */
	private static boolean browserStackEnabled = false;

	/**
	 * The BrowserStack hub URL
	 */
	private static String browserStackHubUrl = null;

	/**
	 * The BrowserStack configuration properties
	 */
	private static BrowserStackProperties browserStackProperties = null;

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
	 * Sets the Appium server URLs for mobile driver connections
	 * @param urls the Appium server URLs
	 *
	 * precondition: urls != null
	 */
	public static void setConfiguredAppiumUrls(String[] urls) {
		assert urls != null;
		APPIUM_URLS = urls;
	}

	/**
	 * Configures BrowserStack as the connection provider.
	 * When set, {@link #getConnection} will use BrowserStack instead of the
	 * default Selenium URL-based round-robin.
	 *
	 * @param hubUrl the BrowserStack hub URL
	 * @param properties the BrowserStack configuration properties
	 *
	 * precondition: hubUrl != null
	 * precondition: properties != null
	 */
	public static void setBrowserStackConfig(String hubUrl, BrowserStackProperties properties) {
		assert hubUrl != null;
		assert properties != null;

		browserStackHubUrl = hubUrl;
		browserStackProperties = properties;
		browserStackEnabled = true;
	}

	/**
	 * Clears the BrowserStack configuration, reverting to the default
	 * Selenium URL-based connection.
	 */
	public static void clearBrowserStackConfig() {
		browserStackEnabled = false;
		browserStackHubUrl = null;
		browserStackProperties = null;
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

		if (browserStackEnabled) {
			URL server_url = new URL(browserStackHubUrl);
			return BrowserFactory.createBrowserStackBrowser(browser.toString(), server_url, browserStackProperties);
		}

		URL server_url = null;

		if (environment.equals(BrowserEnvironment.DISCOVERY)
				&& ("chrome".equalsIgnoreCase(browser.toString())
					|| "firefox".equalsIgnoreCase(browser.toString()))) {
			server_url = new URL("https://" + HUB_URLS[SELENIUM_HUB_IDX % HUB_URLS.length] + "/wd/hub");
			SELENIUM_HUB_IDX++;
		}

		return BrowserFactory.createBrowser(browser.toString(), server_url);
	}

	/**
	 * Creates a {@link MobileDevice} connection via Appium
	 *
	 * @param browser the mobile browser type (ANDROID, IOS)
	 * @param environment the environment to connect to
	 *
	 * @return the mobile device connection
	 *
	 * precondition: browser != null
	 * precondition: browser.isMobile()
	 * precondition: environment != null
	 *
	 * @throws MalformedURLException if the url is malformed
	 * @throws IllegalStateException if Appium URLs are not configured
	 */
    @Retry(name="webdriver")
	public static MobileDevice getMobileConnection(BrowserType browser, BrowserEnvironment environment)
			throws MalformedURLException
    {
		assert browser != null;
		assert browser.isMobile();
		assert environment != null;

		if (APPIUM_URLS == null || APPIUM_URLS.length == 0) {
			throw new IllegalStateException(
				"Appium URLs not configured. Set appium.urls property.");
		}

		URL server_url = new URL("http://" + APPIUM_URLS[APPIUM_SERVER_IDX % APPIUM_URLS.length] + "/wd/hub");
		APPIUM_SERVER_IDX++;

		return MobileFactory.createMobileDevice(browser.toString(), server_url);
	}
}
