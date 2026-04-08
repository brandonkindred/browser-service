package com.looksee.browser;

import com.looksee.browser.config.BrowserStackProperties;
import com.looksee.browser.MobileDevice;
import io.appium.java_client.android.AndroidDriver;
import io.appium.java_client.ios.IOSDriver;
import java.net.URL;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Factory for creating Appium-based mobile WebDriver instances and MobileDevice objects.
 * Encapsulates mobile-specific configuration and driver creation logic.
 *
 * <p>This class is the mobile counterpart of {@link BrowserFactory} and allows
 * Appium to evolve independently from Selenium.
 */
public final class MobileFactory {

	private static Logger log = LoggerFactory.getLogger(MobileFactory.class);

	private MobileFactory() {
		// Utility class — prevent instantiation
	}

	/**
	 * Creates a WebDriver instance for the specified mobile platform.
	 *
	 * @param platformType the mobile platform type ("android", "ios")
	 * @param serverUrl the URL of the Appium server
	 * @return the created WebDriver
	 * @throws WebDriverException if an error occurs or the platform is unsupported
	 *
	 * precondition: platformType != null
	 * precondition: serverUrl != null
	 */
	@SuppressWarnings("rawtypes")
	public static WebDriver createDriver(String platformType, URL serverUrl)
			throws WebDriverException {
		assert platformType != null;
		assert serverUrl != null;

		switch (platformType.toLowerCase()) {
			case "android":  return openWithAndroid(serverUrl);
			case "ios":      return openWithIOS(serverUrl);
			default:
				throw new WebDriverException("Unsupported mobile platform type: " + platformType);
		}
	}

	/**
	 * Creates a fully configured MobileDevice instance.
	 *
	 * @param platformType the mobile platform type ("android", "ios")
	 * @param serverUrl the URL of the Appium server
	 * @return the created MobileDevice
	 *
	 * precondition: platformType != null
	 * precondition: serverUrl != null
	 */
	public static MobileDevice createMobileDevice(String platformType, URL serverUrl) {
		assert platformType != null;
		assert serverUrl != null;

		WebDriver driver = createDriver(platformType, serverUrl);
		return new MobileDevice(driver, platformType);
	}

	/**
	 * Opens a new Android mobile web session via Appium using UiAutomator2 and Chrome.
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
	 * Opens a new iOS mobile web session via Appium using XCUITest and Safari.
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

	/**
	 * Creates a fully configured MobileDevice instance using BrowserStack.
	 * BrowserStack runs Appium tests on real devices in the cloud.
	 *
	 * @param platformType the mobile platform type ("android", "ios")
	 * @param hubUrl the BrowserStack hub URL
	 * @param properties the BrowserStack configuration properties
	 * @return the created MobileDevice
	 *
	 * precondition: platformType != null
	 * precondition: hubUrl != null
	 * precondition: properties != null
	 */
	public static MobileDevice createBrowserStackMobileDevice(String platformType, URL hubUrl,
			BrowserStackProperties properties) {
		assert platformType != null;
		assert hubUrl != null;
		assert properties != null;

		WebDriver driver = createBrowserStackMobileDriver(platformType, hubUrl, properties);
		return new MobileDevice(driver, platformType);
	}

	/**
	 * Creates a RemoteWebDriver configured for BrowserStack mobile testing.
	 * Uses RemoteWebDriver (not AndroidDriver/IOSDriver) since BrowserStack
	 * handles the Appium server-side; the client just needs to send capabilities.
	 */
	private static WebDriver createBrowserStackMobileDriver(String platformType, URL hubUrl,
			BrowserStackProperties properties) {
		DesiredCapabilities caps = new DesiredCapabilities();

		// BrowserStack credentials
		caps.setCapability("browserstack.user", properties.getUsername());
		caps.setCapability("browserstack.key", properties.getAccessKey());

		// BrowserStack settings
		caps.setCapability("browserstack.debug", String.valueOf(properties.isDebug()));
		caps.setCapability("browserstack.local", String.valueOf(properties.isLocal()));
		caps.setCapability("realMobile", String.valueOf(properties.isRealMobile()));

		// Platform-specific capabilities
		switch (platformType.toLowerCase()) {
			case "android":
				caps.setCapability("platformName", "Android");
				caps.setCapability("browserName", "Chrome");
				caps.setCapability("device", properties.getDeviceName() != null
						? properties.getDeviceName() : "Samsung Galaxy S23");
				break;
			case "ios":
				caps.setCapability("platformName", "iOS");
				caps.setCapability("browserName", "Safari");
				caps.setCapability("device", properties.getDeviceName() != null
						? properties.getDeviceName() : "iPhone 15");
				break;
			default:
				throw new WebDriverException("Unsupported mobile platform type: " + platformType);
		}

		// OS version if specified
		if (properties.getOsVersion() != null) {
			caps.setCapability("os_version", properties.getOsVersion());
		}

		// Optional capabilities
		if (properties.getProject() != null) {
			caps.setCapability("project", properties.getProject());
		}
		if (properties.getBuild() != null) {
			caps.setCapability("build", properties.getBuild());
		}
		if (properties.getName() != null) {
			caps.setCapability("name", properties.getName());
		}

		log.debug("Creating BrowserStack mobile RemoteWebDriver for platform: {}", platformType);
		return new RemoteWebDriver(hubUrl, caps);
	}
}
