package com.looksee.models;


import com.looksee.models.enums.AlertChoice;
import org.openqa.selenium.Alert;
import org.openqa.selenium.NoAlertPresentException;
import org.openqa.selenium.UnhandledAlertException;
import org.openqa.selenium.WebDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Represents an Alert or Confirmation pop-up that is triggered by javascript within the page
 * 
 */
public class PageAlert extends LookseeObject {
	private static Logger log = LoggerFactory.getLogger(PageAlert.class);

	private String message;
	
	/**
	 * Constructor
	 * @param message message of the alert
	 *
	 * precondition: message != null
	 */
	public PageAlert(String message){
		assert message != null : "message must not be null";

		this.message = message;
		this.setKey(generateKey());
	}
	
	/**
	 * Performs a choice on the alert
	 * @param driver {@link WebDriver} to perform the choice on
	 * @param choice {@link AlertChoice} to perform
	 *
	 * precondition: driver != null
	 * precondition: choice != null
	 */
	public void performChoice(WebDriver driver, AlertChoice choice){
		assert driver != null : "driver must not be null";
		assert choice != null : "choice must not be null";

		try{
			Alert alert = driver.switchTo().alert();
			if(AlertChoice.ACCEPT.equals(choice)){
				alert.accept();
			}
			else{
				alert.dismiss();
			}
		}
		catch(NoAlertPresentException nae){
			log.warn( "Alert not present");
		}
	}
	
	/**
	 * Retrieves the message of the alert
	 * @return message of the alert
	 * @throws UnhandledAlertException if the alert is not present
	 */
	public String getMessage() throws UnhandledAlertException{
		return this.message; 
	}
	
	/**
	 * Retrieves the message of an alert
	 * @param alert {@link Alert} to retrieve the message from
	 * @return message of the alert
	 * @throws UnhandledAlertException if the alert is not present
	 *
	 * precondition: alert != null
	 */
	public static String getMessage(Alert alert) throws UnhandledAlertException{
		assert alert != null : "alert must not be null";

		return alert.getText(); 
	}
	
	/**
	 * Checks if two {@link PageAlert}s are equal
	 * @param o {@link Object} to compare to
	 * @return true if the {@link PageAlert}s are equal, false otherwise
	 */
	@Override
	public boolean equals(Object o){
		if (this == o) return true;
        if (!(o instanceof PageAlert)) return false;
        
        PageAlert that = (PageAlert)o;
        
        return this.message.equals(that.getMessage());
	}

	/**
	 * Clones the {@link PageAlert}
	 * @return cloned {@link PageAlert}
	 */
	@Override
	public PageAlert clone() {
		return new PageAlert(this.getMessage());
	}

	/**
	 * Generates a key for the {@link PageAlert}
	 * @return key for the {@link PageAlert}
	 */
	@Override
	public String generateKey() {
		return "alert"+org.apache.commons.codec.digest.DigestUtils.sha256Hex(this.getMessage());
	}
}
