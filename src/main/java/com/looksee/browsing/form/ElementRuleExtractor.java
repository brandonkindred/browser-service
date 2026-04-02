package com.looksee.browsing.form;

import com.looksee.models.Element;
import com.looksee.models.rules.Clickable;
import com.looksee.models.rules.Rule;
import com.looksee.models.rules.RuleFactory;
import com.looksee.services.RuleService;
import java.util.ArrayList;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Extracts rules for input {@link Element}s
 * @author brand
 *
 */
@Service
public class ElementRuleExtractor {

	@Autowired
	private RuleService rule_service;

	/**
	 * Extracts rules for input {@link Element}s
	 *
	 * @param elem the element
	 * @return the rules
	 *
	 * precondition: elem != null
	 */
	public List<Rule> extractInputRules(Element elem){
		assert elem != null;
		List<Rule> rules = new ArrayList<Rule>();

		for(String attr : elem.getAttributes().keySet()){
			Rule rule = RuleFactory.build(attr.toLowerCase(), elem.getAttributes().get(attr));
			
			if(rule != null){
				rules.add(rule_service.save(rule));
			}
		}

		return rules;
	}

	/**
	 * Extracts rules for mouse actions
	 *
	 * @param pageElement the page element
	 * @return the rules
	 *
	 * precondition: pageElement != null
	 */
	public List<Rule> extractMouseRules(Element pageElement) {
		assert pageElement != null;
		List<Rule> rules = new ArrayList<Rule>();

		//iterate over possible mouse actions.
		//if an element action interaction causes change
			//then add the appropriate rule to the list
		Rule clickable = new Clickable();
		rules.add(clickable);
		return rules;
	}
}
