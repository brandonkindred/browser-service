package com.looksee.utils;

import com.looksee.models.ElementState;
import com.looksee.models.Form;
import com.looksee.models.PageState;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.NoArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility class for processing and analyzing web page states and their elements.
 *
 * This class provides static utility methods for processing page states and elements.
 * All methods are guaranteed to:
 * - Not modify their input parameters
 * - Return new collection instances rather than references to internal state
 * - For PageState inputs, return Forms containing only Elements from that PageState
 * - For any returned Form, include only Elements with XPaths that are descendants of the form Element's XPath
 */
@NoArgsConstructor
public class PageUtils {
	@SuppressWarnings("unused")
	private static Logger log = LoggerFactory.getLogger(PageUtils.class);

	/**
	 * Extracts all forms including the child inputs and associated labels.
	 * @param page the page state
	 *
	 * @return a set of forms
	 * @throws Exception if an error occurs
	 *
	 * precondition: page != null
	 */
	public static Set<Form> extractAllForms(PageState page) throws Exception {
		assert page != null;
		
		Set<Form> form_list = new HashSet<Form>();
		
		//filter all elements that aren't the main form element
		List<ElementState> forms = page.getElements()
										.parallelStream()
										.filter(element -> {
											String xpath = element.getXpath();
											int last_idx = xpath.lastIndexOf("/");
											if(last_idx+5 <= xpath.length()) {
												return element.getXpath().substring(last_idx, last_idx+5).contains("/form");
											}
											else {
												return false;
											}
										})
										.collect(Collectors.toList());
		
		for(ElementState form_element : forms){
			List<ElementState> form_elements = page.getElements()
													.parallelStream()
													.filter(element -> element.getXpath().contains(form_element.getXpath()))
													.filter(element -> !element.getXpath().equals(form_element.getXpath()))
													.collect(Collectors.toList());
		
			List<ElementState> input_elements =  form_elements.parallelStream()
															.filter(element -> element.getName().contentEquals("input"))
															.collect(Collectors.toList());
			
			ElementState submit_element = FormUtils.findFormSubmitButton(form_elements);
			Form form = new Form(form_element,
								input_elements,
								submit_element,
								"Form #"+(forms.size()+1));
			
			form_list.add(form);
		}
		
		return form_list;
	}
}
