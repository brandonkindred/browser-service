package com.looksee.utils;

import com.looksee.models.ColorData;
import com.looksee.models.ElementState;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import lombok.NoArgsConstructor;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.WebElement;

/**
 * Utility class for filtering and enriching element states.
 */
@NoArgsConstructor
public class ElementStateUtils {
	
	/**
	 * Filters elements with negative positions.
	 * @param elements the list of element states
	 * @return the filtered list of element states
	 */
	public static List<ElementState> filterElementsWithNegativePositions(List<ElementState> elements) {
		assert elements != null;
		List<ElementState> filtered_elements = new ArrayList<>();

		for(ElementState element : elements){
			if(element.getXLocation() >= 0 && element.getYLocation() >= 0){
				filtered_elements.add(element);
			}
		}

		return filtered_elements;
	}

	/**
	 * Filters elements that are not visible in the viewport.
	 * @param x_offset the x offset
	 * @param y_offset the y offset
	 * @param elements the list of element states
	 * @param viewport_size the viewport size
	 * @return the filtered list of element states
	 *
	 * <b>Preconditions:</b>
	 * <ul>
	 *   <li>elements != null</li>
	 *   <li>viewport_size != null</li>
	 * </ul>
	 */
	public static List<ElementState> filterNotVisibleInViewport(int x_offset,
																int y_offset,
																List<ElementState> elements,
																Dimension viewport_size) {
		assert elements != null;
		assert viewport_size != null;

		List<ElementState> filtered_elements = new ArrayList<>();

		for(ElementState element : elements){
			if(isElementVisibleInPane( x_offset, y_offset, element, viewport_size)){
				filtered_elements.add(element);
			}
		}

		return filtered_elements;
	}

	/**
	 * Filters out html, body, script and link tags
	 * @param web_elements the list of web elements
	 * @return the filtered list of web elements
	 *
	 * <b>Preconditions:</b>
	 * <ul>
	 *   <li>web_elements != null</li>
	 * </ul>
	 */
	public static List<WebElement> filterStructureTags(List<WebElement> web_elements) {
		assert web_elements != null;

		List<WebElement> elements = new ArrayList<>();
		for(WebElement element : web_elements){
			if(element.getTagName().equals("html") || element.getTagName().equals("body")
					|| element.getTagName().equals("link") || element.getTagName().equals("script")
					|| element.getTagName().equals("title") || element.getTagName().equals("meta")
					|| element.getTagName().equals("head")){
				continue;
			}
			elements.add(element);
		}
		return elements;
	}
	
	/**
	 * Checks if an element is visible in the viewport.
	 * @param x_offset the x offset
	 * @param y_offset the y offset
	 * @param elem the element state
	 * @param viewport_size the viewport size
	 * @return true if the element is visible in the viewport, false otherwise
	 *
	 * <b>Preconditions:</b>
	 * <ul>
	 *   <li>elem != null</li>
	 *   <li>viewport_size != null</li>
	 * </ul>
	 */
	public static boolean isElementVisibleInPane(int x_offset,
													int y_offset,
													ElementState elem,
													Dimension viewport_size){
		assert elem != null;
		assert viewport_size != null;

		int x = elem.getXLocation();
		int y = elem.getYLocation();

		int height = elem.getHeight();
		int width = elem.getWidth();

		return x >= x_offset && y >= y_offset && (x+width) <= (viewport_size.getWidth()+x_offset)
				&& (y+height) <= (viewport_size.getHeight()+y_offset);
	}

	/**
	 * Checks if the tag name is a header.
	 * @param tag_name the tag name
	 * @return true if the tag name is a header, false otherwise
	 */
	public static boolean isHeader(String tag_name) {
		if(tag_name == null) {
			return false;
		}
		return "h1".equalsIgnoreCase(tag_name)
				|| "h2".equalsIgnoreCase(tag_name)
				|| "h3".equalsIgnoreCase(tag_name)
				|| "h4".equalsIgnoreCase(tag_name)
				|| "h5".equalsIgnoreCase(tag_name)
				|| "h6".equalsIgnoreCase(tag_name);
	}


	/**
	 * Checks if outer html fragment owns text. An element is defined as owning text if 
	 *   if it contains text immediately within the element. If an element has only
	 *   child elements and no text then it does not own text
	 *
	 * @param element_state {@link ElementState element} to be evaluated for text ownership
	 *
	 * @return 1 if element is text owner, otherwise 0
	 *
	 * <b>Preconditions:</b>
	 * <ul>
	 *   <li>element_state != null</li>
	 * </ul>
	 */
	public static boolean isTextContainer(ElementState element_state) {
		assert element_state != null;
		
		Document doc = Jsoup.parseBodyFragment(element_state.getOuterHtml());
		Element body = doc.body();
		return !body.ownText().isEmpty();
	}
	
	/**
	 * Checks if outer html fragment owns text. An element is defined as owning text if 
	 *   if it contains text immediately within the element. If an element has only
	 *   child elements and no text then it does not own text
	 *
	 * @param element {@link WebElement element} to be evaluated for text ownership
	 *
	 * @return 1 if element is text owner, otherwise 0
	 *
	 * <b>Preconditions:</b>
	 * <ul>
	 *   <li>element != null</li>
	 * </ul>
	 */
	public static boolean isTextContainer(WebElement element) {
		assert element != null;
		
		Document doc = Jsoup.parseBodyFragment(element.getAttribute("outerHTML"));
		Element body = doc.body();
		return !body.ownText().isEmpty();
	}

	/**
	 * Checks if the tag name is a list
	 *
	 * @param tag_name the tag name to check
	 * @return true if the tag name is a list, false otherwise
	 */
	public static boolean isList(String tag_name) {
		if(tag_name == null) {
			return false;
		}
		return "ul".equalsIgnoreCase(tag_name)
				|| "ol".equalsIgnoreCase(tag_name)
				|| "li".equalsIgnoreCase(tag_name);
	}

	/**
	 * Enriches background colors for a list of {@link ElementState elements}
	 *
	 * @param element_states the list of element states to enrich
	 * @return the enriched element states
	 *
	 * <b>Preconditions:</b>
	 * <ul>
	 *   <li>element_states != null</li>
	 * </ul>
	 */
	public static Stream<ElementState> enrichBackgroundColor(List<ElementState> element_states) {
		assert element_states != null;
		//ENRICHMENT : BACKGROUND COLORS
		return element_states.parallelStream()
								.filter(element -> element != null)
								.map(element -> {
				try {
					ColorData font_color = new ColorData(element.getRenderedCssValues().get("color"));
					//extract opacity color
					ColorData bkg_color = null;
					if(element.getScreenshotUrl().trim().isEmpty()) {
						bkg_color = new ColorData(element.getRenderedCssValues().get("background-color"));
					}
					else {
						bkg_color = ImageUtils.extractBackgroundColor( new URL(element.getScreenshotUrl()),
																		font_color);
					}
					String bg_color = bkg_color.rgb();	
					
					//Identify background color by getting largest color used in picture
					ColorData background_color = new ColorData(bg_color);
					element.setBackgroundColor(background_color.rgb());
					element.setForegroundColor(font_color.rgb());
					
					double contrast = ColorData.computeContrast(background_color, font_color);
					element.setTextContrast(contrast);
					return element;
				}
				catch (Exception e) {
					e.printStackTrace();
				}
			return element;
		});
	}
	
	/**
	 * Enriches background colors for a {@link ElementState element}
	 *
	 * @param element the element to enrich
	 * @return the enriched element
	 *
	 * <b>Preconditions:</b>
	 * <ul>
	 *   <li>element != null</li>
	 * </ul>
	 */
	public static ElementState enrichBackgroundColor(ElementState element) {
		assert element != null;
		//ENRICHMENT : BACKGROUND COLORS
		try {
			String color_css = element.getRenderedCssValues().get("color");
			if(color_css == null) {
				color_css = "#000000";
			}
			
			ColorData font_color = new ColorData(color_css);
			
			//extract opacity color
			ColorData bkg_color = null;
			if(element.getScreenshotUrl().trim().isEmpty()) {
			bkg_color = new ColorData(element.getRenderedCssValues().get("background-color"));
			}
			else {
				bkg_color = ImageUtils.extractBackgroundColor( new URL(element.getScreenshotUrl()),
																font_color);
			}
			
			//Identify background color by getting largest color used in picture
			String bg_color = bkg_color.rgb();
			ColorData background_color = new ColorData(bg_color);
			element.setBackgroundColor(background_color.rgb());
			element.setForegroundColor(font_color.rgb());
			
			double contrast = ColorData.computeContrast(background_color, font_color);
			element.setTextContrast(contrast);
		}
		catch (Exception e) {
			e.printStackTrace();
		}
		return element;
	}
	
	/**
	 * Checks if element is a link, a button element or class contains "btn or button"
	 * @param element {@link ElementState}
	 * 
	 * @return true if element is a link or button element or if element class contains "btn or button"
	 *
	 * precondition: element != null
	 */
	public static boolean isInteractiveElement(ElementState element) {
		assert element != null;

		return element.getName().contentEquals("a") 
				|| element.getName().contentEquals("button")
				|| (element.getAttributes().containsKey("class")
					&& (element.getAttribute("class").contains("btn")
						|| element.getAttribute("class").contains("button")))
				|| (element.getAttributes().containsKey("type") 
					&& (element.getAttribute("type").contains("button")
						|| element.getAttribute("type").contains("checkbox")
						|| element.getAttribute("type").contains("email")
						|| element.getAttribute("type").contains("file")
						|| element.getAttribute("type").contains("radio")
						|| element.getAttribute("type").contains("tel")))
				|| element.getAttributes().containsKey("onclick");
	}
	
	/**
	 * Checks if {@link ElementState} is a form element
	 * @param element the element to check
	 * @return true if the element is a form element, otherwise returns false
	 *
	 * precondition: element != null
	 */
	public static boolean isFormElement(ElementState element) {
		assert element != null;

		return element.getXpath().contains("form");
	}

	/**
	 * Checks if {@link ElementState} is an interactive element. The methods
	 * defines element interactivity as an element that is either natively interactive in
	 * HTML such as the tags a, button, select, and input. This method also
	 * checks that the outer html has reference to "click" or onClick.
	 * 
	 * @param element the element to check
	 * @return true if the element is interactive, otherwise returns false
	 * 
	 * precondition: element != null
	 */
    public static boolean isInteractiveElement(Element element) {
		assert element != null;
		
		String tagName = element.tagName();

		if("a".equals(tagName)
			|| "button".equals(tagName)
			|| "select".equals(tagName)
			|| "input".equals(tagName) )
		{
			return true;
		}

		String outerHtml = element.outerHtml().toLowerCase();
		if(outerHtml.contains("click")
			&& (element.html() != null && !element.html().contains("click"))){
			return true;
		}

		return false;
    }
}
