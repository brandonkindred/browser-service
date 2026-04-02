package com.looksee.utils;

import com.looksee.models.ElementState;
import com.looksee.models.enums.FormType;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility class for classifying form types.
 */
public class FormUtils {
	private static Logger log = LoggerFactory.getLogger(FormUtils.class);

	/**
	 * Classifies a form based on its attributes.
	 * @param form_tag the form tag
	 * @param form_elements the form elements
	 * @return the form type
	 *
	 * precondition: form_tag != null
	 * precondition: form_elements != null
	 */
	public static FormType classifyForm(ElementState form_tag, List<ElementState> form_elements) {
		assert form_tag != null;
		assert form_elements != null;

		Map<String, String> attributes = form_tag.getAttributes();
		for(String attr: attributes.keySet()){
			String vals = attributes.get(attr);
			if(vals.contains("register") || (vals.contains("sign") && vals.contains("up"))){
				log.warn("Identified REGISTRATION form");
				return FormType.REGISTRATION;
			}
			else if(vals.contains("login") || (vals.contains("sign") && vals.contains("in"))){
				log.warn("Identified LOGIN form");
				return FormType.LOGIN;
			}
			else if(vals.contains("search")){
				log.warn("Identified SEARCH form");
				return FormType.SEARCH;
			}
			else if(vals.contains("reset") && vals.contains("password")){
				log.warn("Identified PASSWORD RESET form");
				return FormType.PASSWORD_RESET;
			}
			else if(vals.contains("payment") || vals.contains("credit")){
				log.warn("Identified PAYMENT form");
				return FormType.PAYMENT;
			}
		}
		
		return FormType.LEAD;
	}

	/**
	 * locates and returns the form submit button
	 * 
	 * @param nested_elements the nested elements
	 * @return the form submit button
	 *
	 * precondition: nested_elements != null
	 * precondition: user_id != null
	 * precondition: !user_id.isEmpty()
	 * precondition: form_elem != null
	 * precondition: browser != null;
	 */
	public static ElementState findFormSubmitButton(List<ElementState> nested_elements) {
		assert nested_elements != null;

		Map<String, String> attributes = new HashMap<>();
		for(ElementState elem : nested_elements){
			attributes = elem.getAttributes();
			if(elem.getAllText().toLowerCase().contains("sign in")){
				return elem;
			}
			for(String attribute : attributes.values()){
				if(attribute.toLowerCase().contains("submit")){
					return elem;
				}
			}
		}
		return null;
	}
}
