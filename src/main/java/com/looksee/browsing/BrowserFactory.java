package com.looksee.browsing;

import com.looksee.browser.config.BrowserStackProperties;
import com.looksee.browser.Browser;
import java.net.MalformedURLException;
import java.net.URL;
import org.openqa.selenium.ImmutableCapabilities;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.openqa.selenium.remote.UnreachableBrowserException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Factory for creating WebDriver instances and Browser objects.
 * Encapsulates browser-specific configuration and driver creation logic.
 */
public final class BrowserFactory {

	private static Logger log = LoggerFactory.getLogger(BrowserFactory.class);

	private BrowserFactory() {
		// Utility class — prevent instantiation
	}

	/**
	 * Creates a WebDriver instance for the specified browser type.
	 *
	 * @param browserType the browser type ("chrome", "firefox")
	 * @param hubUrl the URL of the Selenium hub node
	 * @return the created WebDriver
	 * @throws MalformedURLException if the URL is malformed
	 * @throws UnreachableBrowserException if the browser is unreachable
	 * @throws WebDriverException if an error occurs while creating the driver
	 *
	 * precondition: browserType != null
	 * precondition: hubUrl != null
	 */
	public static WebDriver createDriver(String browserType, URL hubUrl)
			throws MalformedURLException, UnreachableBrowserException, WebDriverException {
		assert browserType != null;
		assert hubUrl != null;

		if ("chrome".equals(browserType)) {
			return openWithChrome(hubUrl);
		} else if ("firefox".equals(browserType)) {
			return openWithFirefox(hubUrl);
		}

		throw new WebDriverException("Unsupported browser type: " + browserType);
	}

	/**
	 * Creates a fully configured Browser instance.
	 *
	 * @param browserType the browser type ("chrome", "firefox")
	 * @param hubUrl the URL of the Selenium hub node
	 * @return the created Browser
	 * @throws MalformedURLException if the URL is malformed
	 *
	 * precondition: browserType != null
	 * precondition: hubUrl != null
	 */
	public static Browser createBrowser(String browserType, URL hubUrl) throws MalformedURLException {
		assert browserType != null;
		assert hubUrl != null;

		WebDriver driver = createDriver(browserType, hubUrl);
		return new Browser(driver, browserType);
	}

	/**
	 * Opens a new Chrome browser window with headless configuration.
	 *
	 * @param hub_node_url the url of the selenium hub node
	 * @return Chrome web driver
	 * @throws MalformedURLException if the url is malformed
	 * @throws UnreachableBrowserException if the browser is unreachable
	 * @throws WebDriverException if an error occurs while opening the browser
	 *
	 * precondition: hub_node_url != null
	 */
	public static WebDriver openWithChrome(URL hub_node_url)
			throws MalformedURLException, UnreachableBrowserException, WebDriverException {
		assert hub_node_url != null;

		ChromeOptions chrome_options = new ChromeOptions();
		chrome_options.addArguments("user-agent=LookseeBot");
		chrome_options.addArguments("window-size=1920,1080");
		chrome_options.addArguments("--remote-allow-origins=*");
		chrome_options.addArguments("--headless=new");

		log.debug("Requesting chrome remote driver from hub");
		RemoteWebDriver driver = new RemoteWebDriver(hub_node_url, chrome_options);

		return driver;
	}

	/**
	 * Opens a new Firefox browser window.
	 *
	 * @param hub_node_url the url of the selenium hub node
	 * @return Firefox web driver
	 * @throws MalformedURLException if the url is malformed
	 *
	 * precondition: hub_node_url != null
	 */
	public static WebDriver openWithFirefox(URL hub_node_url)
			throws MalformedURLException, UnreachableBrowserException {
		assert hub_node_url != null;

		ImmutableCapabilities capabilities = new ImmutableCapabilities("browserName", "firefox");

		RemoteWebDriver driver = new RemoteWebDriver(hub_node_url, capabilities);
		driver.manage().window().maximize();

		return driver;
	}

	/**
	 * Creates a fully configured Browser instance using BrowserStack.
	 *
	 * @param browserType the browser type ("chrome", "firefox")
	 * @param hubUrl the BrowserStack hub URL
	 * @param properties the BrowserStack configuration properties
	 * @return the created Browser
	 * @throws MalformedURLException if the URL is malformed
	 *
	 * precondition: browserType != null
	 * precondition: hubUrl != null
	 * precondition: properties != null
	 */
	public static Browser createBrowserStackBrowser(String browserType, URL hubUrl,
			BrowserStackProperties properties) throws MalformedURLException {
		assert browserType != null;
		assert hubUrl != null;
		assert properties != null;

		WebDriver driver = createBrowserStackDriver(browserType, hubUrl, properties);
		return new Browser(driver, browserType);
	}

	/**
	 * Creates a RemoteWebDriver configured for BrowserStack.
	 * Credentials are passed via capabilities rather than embedded in the URL.
	 * Headless mode is omitted since BrowserStack manages the display environment.
	 */
	private static WebDriver createBrowserStackDriver(String browserType, URL hubUrl,
			BrowserStackProperties properties) {
		DesiredCapabilities caps = new DesiredCapabilities();

		// BrowserStack credentials
		caps.setCapability("browserstack.user", properties.getUsername());
		caps.setCapability("browserstack.key", properties.getAccessKey());

		// BrowserStack settings
		caps.setCapability("browserstack.debug", String.valueOf(properties.isDebug()));
		caps.setCapability("browserstack.local", String.valueOf(properties.isLocal()));

		// Optional capabilities
		if (properties.getOs() != null) {
			caps.setCapability("os", properties.getOs());
		}
		if (properties.getOsVersion() != null) {
			caps.setCapability("os_version", properties.getOsVersion());
		}
		if (properties.getBrowserVersion() != null) {
			caps.setCapability("browser_version", properties.getBrowserVersion());
		}
		if (properties.getProject() != null) {
			caps.setCapability("project", properties.getProject());
		}
		if (properties.getBuild() != null) {
			caps.setCapability("build", properties.getBuild());
		}
		if (properties.getName() != null) {
			caps.setCapability("name", properties.getName());
		}

		// Browser-specific configuration
		String browser = properties.getBrowser() != null ? properties.getBrowser() : browserType;
		caps.setCapability("browser", capitalize(browser));

		if ("chrome".equalsIgnoreCase(browserType)) {
			ChromeOptions chromeOptions = new ChromeOptions();
			chromeOptions.addArguments("user-agent=LookseeBot");
			chromeOptions.addArguments("window-size=1920,1080");
			chromeOptions.addArguments("--remote-allow-origins=*");
			// Headless mode is omitted — BrowserStack manages the display environment
			caps.setCapability(ChromeOptions.CAPABILITY, chromeOptions);
		}

		log.debug("Creating BrowserStack RemoteWebDriver for browser: {}", browser);
		return new RemoteWebDriver(hubUrl, caps);
	}

	private static String capitalize(String str) {
		if (str == null || str.isEmpty()) {
			return str;
		}
		return str.substring(0, 1).toUpperCase() + str.substring(1).toLowerCase();
	}
}
