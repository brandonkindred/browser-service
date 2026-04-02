package com.looksee.browsing.form;

import com.looksee.models.ActionOLD;
import com.looksee.models.Element;
import com.looksee.models.LookseeObject;
import com.looksee.models.rules.Rule;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.neo4j.core.schema.Node;

/**
 * Defines a complex element grouping of input and label for a field contained within a form.
 * Also contains list of rules which can be enforced on the object
 */
@Node
@Getter
@Setter
public class FormField extends LookseeObject{
	
	private List<Rule> rules;
	private Element formField;
	
	/**
	 * Constructor
	 */
	public FormField() {
		rules = new ArrayList<>();
	}
	
	/**
	 * Constructs new FormField
	 *
	 * @param formField combo element defining the input grouping for this FormField
	 *
	 * precondition: formField != null
	 */
	public FormField(Element formField){
		assert formField != null;
		this.formField = formField;
		this.rules = new ArrayList<Rule>();
		setKey(generateKey());
	}
	
	/**
	 * Generates a key for the form field
	 *
	 * @return the key
	 */
	@Override
	public String generateKey() {
		return formField.getKey();
	}

	/**
	 * Constructs new FormField
	 *
	 * @param formField combo element defining the input grouping for this FormField
	 * @param rules list of {@link Rule} defined on this FormField
	 *
	 * precondition: formField != null
	 * precondition: rules != null
	 */
	public FormField(Element formField, List<Rule> rules){
		assert formField != null;
		assert rules != null;
		this.formField = formField;
		this.rules = rules;
	}
	
	/**
	 * Adds a rule
	 *
	 * @param rule Rule to be added
	 * @return true if the rule was added successfully, otherwise return false;
	 *
	 * precondition: rule != null
	 */
	public boolean addRule(Rule rule){
		assert rule != null;
		return this.rules.add(rule);
	}
	
	/**
	 * Adds a Collection of rules
	 *
	 * @param rules List of {@link Rule}s to be added
	 * @return true if rules were added successfully, otherwise return false;
	 *
	 * precondition: rules != null
	 */
	public boolean addRules(List<Rule> rules){
		assert rules != null;
		return this.rules.addAll(rules);
	}
	
	/**
	 * This handles the performing of a {@link ActionOLD}
	 *
	 * @param action the action to perform
	 *
	 * precondition: action != null
	 * precondition: !action.isEmpty()
	 */
	public void performAction(String action){
		assert action != null;
		assert !action.isEmpty();
	}
}
