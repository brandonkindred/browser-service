package com.looksee.utils;

import com.looksee.models.LookseeObject;
import com.looksee.models.PageState;
import com.looksee.models.journeys.Redirect;
import com.looksee.models.journeys.Step;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility class for path operations
 * 
 * Deprecated in favor of {@link JourneyUtils}
 */
public class PathUtils {
	@SuppressWarnings("unused")
	private static Logger log = LoggerFactory.getLogger(PathUtils.class);

	/**
	 * Retrieves the last {@link PageState} in the given list of {@link LookseeObject}s
	 *
	 * @param pathObjects list of {@link LookseeObject}s in sequential order
	 *
	 * @return last page state in list
	 *
	 * precondition: pathObjects != null
	 */
	public static PageState getLastPageStateOLD(List<LookseeObject> pathObjects) {
		assert(pathObjects != null);
				
		for(int idx = pathObjects.size()-1; idx >= 0; idx--){
			if(pathObjects.get(idx).getKey().contains("page")){
				return (PageState)pathObjects.get(idx);
			}
		}
		
		return null;
	}
	
	/**
	 * Retrieves the last {@link PageState} in the given list of {@link Step}s
	 *
	 * @param steps list of {@link Step}s in sequential order
	 *
	 * @return last page state in list
	 *
	 * precondition: steps != null
	 */
	public static PageState getLastPageState(List<Step> steps) {
		assert(steps != null);
		
		//get last step
		Step lastStep = steps.get(steps.size()-1);
		PageState lastPage = lastStep.getEndPage();
		
		return lastPage;
	}
	
	/**
	 * Retrieves the second to last {@link PageState} in the given list of {@link Step}s
	 *
	 * @param steps list of {@link Step}s in sequential order
	 *
	 * @return second to last page state in list
	 *
	 * precondition: steps != null
	 */
	public static PageState getSecondToLastPageState(List<Step> steps) {
		assert(steps != null);
			
		//get last step
		Step lastStep = steps.get(steps.size()-1);
		PageState startPage = lastStep.getStartPage();
		
		return startPage;
	}
	
	/**
	 * Retrieves the index of the last {@link String} in the given list of {@link String}s that contains "elementstate"
	 *
	 * @param pathKeys list of {@link String}s
	 *
	 * @return index of last element state in list
	 *
	 * precondition: pathKeys != null
	 */
	public static int getIndexOfLastElementState(List<String> pathKeys){
		assert(pathKeys != null);

		for(int elementIdx=pathKeys.size()-1; elementIdx >= 0; elementIdx--){
			if(pathKeys.get(elementIdx).contains("elementstate")){
				return elementIdx;
			}
		}

		return -1;
	}

	/**
	 * Orders the given list of {@link LookseeObject}s based on the order of the given list of {@link String}s
	 *
	 * @param pathKeys list of {@link String}s
	 * @param pathObjects list of {@link LookseeObject}s
	 *
	 * @return ordered list of {@link LookseeObject}s
	 *
	 * precondition: pathKeys != null
	 * precondition: pathObjects != null
	 */
	public static List<LookseeObject> orderPathObjects(List<String> pathKeys, List<LookseeObject> pathObjects) {
		assert pathKeys != null;
		assert pathObjects != null;

		List<LookseeObject> orderedPathObjects = new ArrayList<>();
		List<String> tempPathKeys = new ArrayList<>(pathKeys);
		
		//Ensure Order path objects
		for(String pathObjKey : tempPathKeys){
			for(LookseeObject obj : pathObjects){
				if(obj.getKey().equals(pathObjKey)){
					orderedPathObjects.add(obj);
				}
			}
		}

		LookseeObject lastPathObj = null;
		List<LookseeObject> reducedPathObj = new ArrayList<>();
		//scrub path objects for duplicates
		for(LookseeObject obj : orderedPathObjects){
			if(lastPathObj == null || !obj.getKey().equals(lastPathObj.getKey())){
				lastPathObj = obj;
				reducedPathObj.add(obj);
			}
		}

		return reducedPathObj;
	}

	/**
	 * Removes duplicate {@link LookseeObject}s from the given list of {@link LookseeObject}s
	 *
	 * @param orderedPathObjects list of {@link LookseeObject}s
	 *
	 * @return list of {@link LookseeObject}s with duplicates removed
	 *
	 * precondition: orderedPathObjects != null
	 */
	public static List<LookseeObject> reducePathObjects(List<LookseeObject> orderedPathObjects) {
		assert orderedPathObjects != null;

		//scrub path objects for duplicates
		List<LookseeObject> reducedPathObjs = new ArrayList<>();
		LookseeObject lastPathObj = null;
		for(LookseeObject obj : orderedPathObjects){
			if(lastPathObj == null || !obj.getKey().equals(lastPathObj.getKey())){
				lastPathObj = obj;
				reducedPathObjs.add(obj);
			}
		}
				
		return reducedPathObjs;
	}

	/**
	 * Retrieves the first {@link PageState} in the given list of {@link LookseeObject}s
	 *
	 * @param orderedPathObjects list of {@link LookseeObject}s
	 *
	 * @return first page state in list
	 *
	 * precondition: orderedPathObjects != null
	 */
	public static PageState getFirstPage(List<LookseeObject> orderedPathObjects) {
		assert orderedPathObjects != null;

		//find first page
		for(LookseeObject obj : orderedPathObjects){
			if(obj instanceof PageState){
				return ((PageState)obj);
			}
		}
		
		return null;
	}

	/**
	 * Retrieves the second to last {@link PageState} in the given list of {@link LookseeObject}s
	 *
	 * @param pathObjects list of {@link LookseeObject}s
	 *
	 * @return second to last page state in list
	 *
	 * precondition: path_objects != null
	 */
	public static PageState getSecondToLastPageStateOLD(List<LookseeObject> pathObjects) {
		assert(pathObjects != null);
		
		int page_states_seen = 0;
		log.warn("path objects length while getting second to last page state ;: "+pathObjects.size());
		for(int idx = pathObjects.size()-1; idx >=0; idx--){
			if(pathObjects.get(idx).getKey().contains("pagestate")){
				if(page_states_seen >= 1){
					return (PageState)pathObjects.get(idx);
				}
				page_states_seen++;
			}
		}
		
		return null;
	}

	/**
	 * Removes duplicate {@link String}s from the given list of {@link String}s
	 *
	 * @param finalKeyList list of {@link String}s
	 *
	 * @return list of {@link String}s with duplicates removed
	 *
	 * precondition: finalKeyList != null
	 */
	public static List<String> reducePathKeys(List<String> finalKeyList) {
		assert finalKeyList != null;

		//scrub path objects for duplicates
		List<String> reducedPathKeys = new ArrayList<>();
		String lastPathKey = null;
		for(String key : finalKeyList){
			if(!key.equals(lastPathKey)){
				lastPathKey = key;
				reducedPathKeys.add(key);
			}
		}
		return reducedPathKeys;
	}

	/**
	 * Retrieves the first {@link String} in the given list of {@link LookseeObject}s
	 *
	 * @param pathObjects list of {@link LookseeObject}s
	 *
	 * @return first url in list
	 *
	 * precondition: pathObjects != null
	 * precondition: !pathObjects.isEmpty()
	 */
	public static String getFirstUrl_OLD(List<LookseeObject> pathObjects) {
		assert pathObjects != null;
		assert !pathObjects.isEmpty();
		
		LookseeObject obj = pathObjects.get(0);
		
		if(obj.getKey().contains("redirect")) {
			log.warn("first path object is a redirect");
			Redirect redirect = (Redirect)obj;
			return redirect.getStartUrl();
		}
		else if(obj.getKey().contains("pagestate")) {
			log.warn("first path object is a page state");
			PageState pageState = (PageState)obj;
			return pageState.getUrl();
		}
		return null;
	}
	
	/**
	 * Retrieves the first {@link String} in the given list of {@link Step}s
	 *
	 * @param steps list of {@link Step}s
	 *
	 * @return first url in list
	 *
	 * <b>Preconditions:</b>
	 * <ul>
	 *   <li>steps != null</li>
	 *   <li>!steps.isEmpty()</li>
	 * </ul>
	 */
	public static String getFirstUrl(List<Step> steps) {
		assert steps != null;
		assert !steps.isEmpty();
		
		Step obj = steps.get(0);
		
		if(obj instanceof Redirect) {
			log.warn("first path object is a redirect");
			Redirect redirect = (Redirect)obj;
			return redirect.getStartUrl();
		}
		else {
			return obj.getStartPage().getUrl();
		}
	}
}
