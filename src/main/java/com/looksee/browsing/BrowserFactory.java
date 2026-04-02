package com.looksee.browsing;

import com.looksee.models.Browser;
import io.appium.java_client.android.AndroidDriver;
import io.appium.java_client.ios.IOSDriver;
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

		switch (browserType.toLowerCase()) {
			case "chrome":   return openWithChrome(hubUrl);
			case "firefox":  return openWithFirefox(hubUrl);
			case "android":  return openWithAndroid(hubUrl);
			case "ios":      return openWithIOS(hubUrl);
			default:
				throw new WebDriverException("Unsupported browser type: " + browserType);
		}
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
	 * Opens a new Android browser session via Appium using UiAutomator2 and Chrome.
	 *
	 * @param appiumUrl the URL of the Appium server
	 * @return Android web driver
	 * @throws WebDriverException if an error occurs while creating the driver
	 *
	 * precondition: appiumUrl != null
	 */
	@SuppressWarnings("rawtypes")
	public static WebDriver openWithAndroid(URL appiumUrl) throws WebDriverException {
		assert appiumUrl != null;

		DesiredCapabilities caps = new DesiredCapabilities();
		caps.setCapability("platformName", "Android");
		caps.setCapability("automationName", "UiAutomator2");
		caps.setCapability("browserName", "Chrome");
		caps.setCapability("deviceName", "Android Emulator");

		log.debug("Requesting Android driver from Appium server");
		return new AndroidDriver(appiumUrl, caps);
	}

	/**
	 * Opens a new iOS browser session via Appium using XCUITest and Safari.
	 *
	 * @param appiumUrl the URL of the Appium server
	 * @return iOS web driver
	 * @throws WebDriverException if an error occurs while creating the driver
	 *
	 * precondition: appiumUrl != null
	 */
	@SuppressWarnings("rawtypes")
	public static WebDriver openWithIOS(URL appiumUrl) throws WebDriverException {
		assert appiumUrl != null;

		DesiredCapabilities caps = new DesiredCapabilities();
		caps.setCapability("platformName", "iOS");
		caps.setCapability("automationName", "XCUITest");
		caps.setCapability("browserName", "Safari");
		caps.setCapability("deviceName", "iPhone Simulator");

		log.debug("Requesting iOS driver from Appium server");
		return new IOSDriver(appiumUrl, caps);
	}
}
