package com.looksee.browsing.helpers;

import com.looksee.models.ActionOLD;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

/**
 * Contains the Order of operations for actions. A lower value indicates that
 *   an action has less precedence.
 */
@Getter
@Setter
public class ActionHelper {
	/**
	 * Map of action names to their order of operations.
	 */
	public static HashMap<String, Integer> actionOrderOfOperationsMap = new HashMap<>();
	
	/**
	 * List of lists of actions.
	 */
	private static List<List<ActionOLD>> actionLists = new ArrayList<>();
	
	/*
	 * ACTION POLICIES
	 * <<<<<<<<<<<<>>>>>>>>>>>>>>>
	 * 
	 * Mouse over
	 * --------------------
	 * Click
	 * clickAt
	 * click and wait
	 * clickAtAndWait
	 * Click and hold
	 * Release
	 * Double Click
	 * -------------------------
	 * keypresses - all same policy
	 * --------------------
	 * drag and drop
	 * -------------------------
	 */
	static {
		List<ActionOLD> mouse_motion_actions = new ArrayList<>();
		//mouse_motion_actions.add(new Action("mouseover"));
		//mouse_motion_actions.add(new Action("scroll","100"));
		
		List<ActionOLD> click_actions = new ArrayList<>();
		//click_actions.add(new Action("clickAndHold"));
		click_actions.add(new ActionOLD("click"));
		//click_actions.add(new Action("clickAt"));
		//click_actions.add(new Action("clickAndWait"));
		//click_actions.add(new Action("clickAtAndWait"));
		//click_actions.add(new Action("release"));
		//click_actions.add(new Action("doubleClick"));
		
		//List<Action> keyboard_actions = new ArrayList<Action>();
		//keyboard_actions.add(new Action("sendKeys"));
		
		actionLists.add(mouse_motion_actions);
		actionLists.add(click_actions);
		//action_lists.add(keyboard_actions);
	}
	
	/**
	 * Gets the order of operation for an action
	 *
	 * @param actionName name of the action
	 * @return {@link Integer} representing the order of operation
	 *
	 * precondition: actionName != null
	 * precondition: !actionName.isEmpty()
	 */
	public static Integer getOrderOfOperationForAction(String actionName){
		assert actionName != null;
		assert !actionName.isEmpty();
		return actionOrderOfOperationsMap.get(actionName);
	}
}
