package com.looksee.browser;

import com.looksee.browser.enums.Action;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Constructs {@linkplain Actions} provided by Selenium
 *
 */
public class ActionFactory {
	@SuppressWarnings("unused")
	private static Logger log = LoggerFactory.getLogger(ActionFactory.class);

	private Actions builder;
	
	/**
	 * Creates a new action factory
	 *
	 * @param driver the web driver
	 *
	 * precondition: driver != null
	 */
	public ActionFactory(WebDriver driver){
		assert driver != null;
		builder = new Actions(driver);
	}

	/**
	 * Executes an action on a web element
	 * @param elem the web element to perform the action on
	 * @param input the input to send to the web element
	 * @param action the action to perform
	 * @throws WebDriverException if the action cannot be performed
	 *
	 * precondition: elem != null
	 * precondition: input != null
	 * precondition: action != null
	 */
	public void execAction(WebElement elem, String input, Action action) throws WebDriverException{
		assert elem != null;
		assert input != null;
		assert action != null;
		if(Action.CLICK.equals(action)){
			builder.click(elem);
		}
		else if(Action.CLICK_AND_HOLD.equals(action)){
			builder.clickAndHold(elem);
		}
		//Context click clicks select/options box
		else if(Action.CONTEXT_CLICK.equals(action)){
			builder.contextClick(elem);
		}
		else if(Action.DOUBLE_CLICK.equals(action)){
			builder.doubleClick(elem);
		}
		/*else if("dragAndDrop".equals(action)){
			//builder.dragAndDrop(source, target);
		}
		else if("keyDown".equals(action)){
			//builder.keyDown();
		}
		else if("keyUp".equals(action)){
			//builder.keyUp(theKey);
		}
		*/
		else if(Action.RELEASE.equals(action)){
			builder.release(elem);
		}
		else if(Action.SEND_KEYS.equals(action)){
			//builder.sendKeys(elem, Keys.chord(Keys.CONTROL, Keys.ALT, Keys.DELETE));
			builder.sendKeys(elem, input);
		}
		else if(Action.MOUSE_OVER.equals(action)){
			builder.moveToElement(elem);
		}

		builder.build().perform();
	}
}
