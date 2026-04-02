package com.looksee.browsing;

import com.looksee.models.Element;
import com.looksee.models.ElementState;
import com.looksee.models.enums.Action;
import com.looksee.utils.TimingUtils;
import java.util.NoSuchElementException;
import java.util.Random;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.Point;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;


/**
 * Provides methods for crawling web pages using Selenium
 */
@Component
public class Crawler {
	private static Logger log = LoggerFactory.getLogger(Crawler.class);

	
	/**
	 * Executes the given element action pair such that
	 * the action is executed against the element
	 *
	 * @param action {@link Action} to perform
	 * @param elem {@link Element} to perform the action on
	 * @param driver {@link WebDriver} to perform the action on
	 * @throws NoSuchElementException if the element is not found
	 * 
	 * precondition: action != null
	 * precondition: elem != null
	 * precondition: driver != null
	 * 
	 * @throws NoSuchElementException if the element is not found
	 */
	public static void performAction(Action action, com.looksee.models.Element elem, WebDriver driver) throws NoSuchElementException{
		assert action != null;
		assert elem != null;
		assert driver != null;
		ActionFactory actionFactory = new ActionFactory(driver);
		WebElement element = driver.findElement(By.xpath(elem.getXpath()));
		actionFactory.execAction(element, "", action);
		TimingUtils.pauseThread(500L);
	}

	/**
	 * Executes the given element action pair such that
	 * the action is executed against the element
	 *
	 * @param action {@link Action} to perform
	 * @param elem {@link Element} to perform the action on
	 * @param driver {@link WebDriver} to perform the action on
	 * @param location {@link Point} to perform the action at
	 * @throws NoSuchElementException if the element is not found
	 * 
	 * precondition: action != null
	 * precondition: elem != null
	 * precondition: driver != null
	 * precondition: location != null
	 */
	public static void performAction(Action action, com.looksee.models.Element elem, WebDriver driver, Point location) throws NoSuchElementException{
		assert action != null;
		assert elem != null;
		assert driver != null;
		assert location != null;
		ActionFactory actionFactory = new ActionFactory(driver);
		WebElement element = driver.findElement(By.xpath(elem.getXpath()));
		actionFactory.execAction(element, "", action);
		TimingUtils.pauseThread(500L);
	}
	
	/**
	 * Scrolls down the page by a given distance
	 * 
	 * @param driver {@link WebDriver} to scroll
	 * @param distance distance to scroll
	 * 
	 * precondition: driver != null
	 * precondition: distance > 0
	 */
	public static void scrollDown(WebDriver driver, int distance)
    {
        assert driver != null;
        ((JavascriptExecutor)driver).executeScript("scroll(0,"+ distance +");");
    }
	
	/**
	 * Generates a random location within an element but not within child elements
	 * 
	 * @param web_element {@link WebElement} to generate a random location within
	 * @param child_element {@link ElementState} to check for child elements
	 * @return {@link Point} with random location within the element but not within child elements
	 * 
	 * precondition: web_element != null
	 * precondition: child_element != null
	 * 
	 */
	public static Point generateRandomLocationWithinElementButNotWithinChildElements(WebElement web_element, ElementState child_element) {
		assert web_element != null;
		assert child_element != null;
		
		Point elem_location = web_element.getLocation();

		int left_lower_x = 0;
		int left_upper_x = child_element.getXLocation()- elem_location.getX();
		int right_lower_x = (child_element.getXLocation() - elem_location.getX()) + child_element.getWidth();
		int right_upper_x = web_element.getSize().getWidth();
		
		int top_lower_y = 0;
		int top_upper_y = child_element.getYLocation() - elem_location.getY();
		int bottom_lower_y = child_element.getYLocation() - elem_location.getY() + child_element.getHeight();
		int bottom_upper_y = web_element.getSize().getHeight();
		
		int x_coord = 0;
		int y_coord = 0;
		
		if(left_lower_x != left_upper_x && left_upper_x > 0){
			x_coord = new Random().nextInt(left_upper_x);
		}
		else {
			int difference = right_upper_x - right_lower_x;
			int x_offset = 0;
			if(difference == 0){
				x_offset = new Random().nextInt(right_upper_x);
			}
			else{
				x_offset = new Random().nextInt(difference);
			}
			x_coord = right_lower_x + x_offset;
		}
		
		if(top_lower_y != top_upper_y && top_upper_y > 0){
			y_coord = new Random().nextInt(top_upper_y);
		}
		else {
			int difference = bottom_upper_y - bottom_lower_y;
			int y_offset = 0;
			if(difference == 0){
				y_offset = new Random().nextInt(bottom_upper_y);
			}
			else{
				y_offset = new Random().nextInt(bottom_upper_y - bottom_lower_y);
			}
			y_coord = bottom_lower_y + y_offset;
		}

		return new Point(x_coord, y_coord);
	}
}
