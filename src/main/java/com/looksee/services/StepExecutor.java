package com.looksee.services;

import com.looksee.browsing.ActionFactory;
import com.looksee.models.Browser;
import com.looksee.models.ElementState;
import com.looksee.models.PageState;
import com.looksee.models.enums.Action;
import com.looksee.models.journeys.LandingStep;
import com.looksee.models.journeys.LoginStep;
import com.looksee.models.journeys.SimpleStep;
import com.looksee.models.journeys.Step;
import com.looksee.utils.BrowserUtils;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.MoveTargetOutOfBoundsException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Executes a step
 */
@Service
public class StepExecutor {
	@SuppressWarnings("unused")
	private static Logger log = LoggerFactory.getLogger(StepExecutor.class);
	
	/**
	 * Executes a step
	 *
	 * @param browser the {@link Browser} to execute the step on
	 * @param step the {@link Step} to execute
	 *
	 * @throws Exception if an error occurs
	 */
	public void execute(Browser browser, Step step) throws Exception {
		assert browser != null;
		assert step != null;

		try {
			if(step instanceof SimpleStep) {
				SimpleStep simple_step = (SimpleStep)step;
				ElementState element = simple_step.getElementState();
				WebElement web_element = browser.findElement(element.getXpath());
				browser.scrollToElementCentered(web_element);
				//web_element.click();
				//ActionFactory action_factory = new ActionFactory(browser.getDriver());
				//action_factory.execAction(web_element, "", simple_step.getAction());
				((JavascriptExecutor)browser.getDriver()).executeScript("arguments[0].click();", web_element);
			}
			else if(step instanceof LoginStep) {
				LoginStep login_step = (LoginStep)step;
				WebElement username_element = browser.getDriver().findElement(By.xpath(login_step.getUsernameElement().getXpath()));
				ActionFactory action_factory = new ActionFactory(browser.getDriver());
				action_factory.execAction(username_element, login_step.getTestUser().getUsername(), Action.SEND_KEYS);
				
				WebElement password_element = browser.getDriver().findElement(By.xpath(login_step.getPasswordElement().getXpath()));
				action_factory.execAction(password_element, login_step.getTestUser().getPassword(), Action.SEND_KEYS);
	
				WebElement submit_element = browser.getDriver().findElement(By.xpath(login_step.getSubmitElement().getXpath()));
				action_factory.execAction(submit_element, "", Action.CLICK);

				//TimingUtils.pauseThread(5000L);
			}
			else if(step instanceof LandingStep) {
				PageState initial_page = step.getStartPage();
				String sanitized_url = BrowserUtils.sanitizeUrl(initial_page.getUrl(), initial_page.isSecured());
				browser.navigateTo(sanitized_url);
			}
			else {
				log.warn("Unknown step type during execution = " + step.getKey());
			}
		}
		catch(MoveTargetOutOfBoundsException e) {
			browser.getViewportScrollOffset();
			log.warn("MOVE TO TARGET EXCEPTION FOR ELEMENT = "+e.getMessage());
			log.warn("============================================================");;
			log.warn("URL = "+browser.getDriver().getCurrentUrl());
			log.warn("browser dimension = "+browser.getViewportSize());
			log.warn("browser offset = "+browser.getXScrollOffset()+" , "+browser.getYScrollOffset());
			log.warn("============================================================");;
			throw e;
		}
		catch(Exception e){
			log.warn("error occurred while performing steps...."+e.getMessage());
			throw e;
		}
	}
}
