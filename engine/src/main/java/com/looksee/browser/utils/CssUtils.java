package com.looksee.browser.utils;

import cz.vutbr.web.css.CombinedSelector;
import cz.vutbr.web.css.Declaration;
import cz.vutbr.web.css.NodeData;
import cz.vutbr.web.css.RuleSet;
import cz.vutbr.web.domassign.StyleMap;
import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.w3c.dom.Node;

/**
 * Utility class for CSS property extraction and parsing operations.
 */
public final class CssUtils {

	private CssUtils() {
		// Utility class — prevent instantiation
	}

	/**
	 * Loads all computed CSS properties for an element via JavaScript execution.
	 *
	 * @param element the element to load CSS properties for
	 * @param driver the driver to execute JavaScript with
	 * @return map of CSS property names to values
	 *
	 * precondition: element != null
	 * precondition: driver != null
	 */
	public static Map<String, String> loadCssProperties(WebElement element, WebDriver driver) {
		assert element != null;
		assert driver != null;

		JavascriptExecutor executor = (JavascriptExecutor) driver;
		String script = "var s = '';" +
				"var o = getComputedStyle(arguments[0]);" +
				"for(var i = 0; i < o.length; i++){" +
				"s+=o[i] + ':' + o.getPropertyValue(o[i])+';';}" +
				"return s;";

		String response = executor.executeScript(script, element).toString();

		Map<String, String> css_map = new HashMap<String, String>();

		String[] css_prop_vals = response.split(";");
		for (String prop_val_pair : css_prop_vals) {
			String[] prop_val = prop_val_pair.split(":");

			if (prop_val.length == 1) {
				continue;
			}
			if (prop_val.length > 0) {
				String prop1 = prop_val[0];
				String prop2 = prop_val[1];
				css_map.put(prop1, prop2);
			}
		}

		return css_map;
	}

	/**
	 * Loads CSS properties from pre-rendered RuleSets by matching element selectors.
	 *
	 * @param rule_sets the rule sets to load the css styles from
	 * @param element the element for which css styles should be loaded
	 * @return the css styles
	 *
	 * precondition: rule_sets != null
	 * precondition: element != null
	 */
	public static Map<String, String> loadCssPrerenderedPropertiesUsingParser(List<RuleSet> rule_sets, org.jsoup.nodes.Node element) {
		assert rule_sets != null;
		assert element != null;

		Map<String, String> css_map = new HashMap<>();
		for (RuleSet rule_set : rule_sets) {
			for (CombinedSelector selector : rule_set.getSelectors()) {

				String selector_str = selector.toString();
				if (selector_str.startsWith(".")
						|| selector_str.startsWith("#")) {
					selector_str = selector_str.substring(1);
				}

				if (element.attr("class").contains(selector_str) || element.attr("id").contains(selector_str) || element.nodeName().equals(selector_str)) {

					for (Declaration declaration : rule_set) {
						String raw_property_value = declaration.toString();
						raw_property_value = raw_property_value.replace(";", "");
						String[] property_val = raw_property_value.split(":");

						css_map.put(property_val[0], property_val[1]);
					}
				}
			}
		}

		return css_map;
	}

	/**
	 * Loads pre-render CSS properties for an element using Jsoup document and CSS rule sets.
	 *
	 * @param jsoup_doc the document to load the css styles from
	 * @param w3c_document the document to load the css styles from
	 * @param rule_set_list the rule sets to load the css styles from
	 * @param url the url of the page
	 * @param xpath the xpath of the element
	 * @param element the element for which css styles should be loaded
	 * @return the css styles
	 *
	 * @throws XPathExpressionException if an error occurs while evaluating the xpath
	 * @throws IOException if an error occurs while loading the css styles
	 *
	 * precondition: jsoup_doc != null
	 * precondition: w3c_document != null
	 * precondition: rule_set_list != null
	 * precondition: url != null
	 * precondition: xpath != null
	 * precondition: element != null
	 */
	public static Map<String, String> loadPreRenderCssProperties(Document jsoup_doc,
																  org.w3c.dom.Document w3c_document,
																  Map<String, Map<String, String>> rule_set_list,
																  URL url,
																  String xpath,
																  Element element
	) throws XPathExpressionException, IOException {
		assert jsoup_doc != null;
		assert w3c_document != null;
		assert url != null;
		assert xpath != null;
		assert rule_set_list != null;
		assert element != null;

		Map<String, String> css_map = new HashMap<>();

		for (String css_selector : rule_set_list.keySet()) {
			if (css_selector.startsWith("@")) {
				continue;
			}
			String suffixless_selector = css_selector;
			if (css_selector.contains(":")) {
				suffixless_selector = css_selector.substring(0, css_selector.indexOf(":"));
			}
			Elements selected_elements = jsoup_doc.select(suffixless_selector);
			for (Element selected_elem : selected_elements) {
				if (selected_elem.html().equals(element.html())) {
					css_map.putAll(rule_set_list.get(css_selector));
				}
			}
		}

		return css_map;
	}

	/**
	 * Loads CSS properties using XPath and a StyleMap parser.
	 *
	 * @param w3c_document the document to load the css styles from
	 * @param map the style map to load the css styles from
	 * @param url the url of the page
	 * @param xpath the xpath of the element
	 * @return the css styles
	 * @throws XPathExpressionException if the xpath is invalid
	 *
	 * precondition: w3c_document != null
	 * precondition: map != null
	 * precondition: url != null
	 * precondition: xpath != null
	 */
	public static Map<String, String> loadCssPropertiesUsingParser(Document w3c_document,
																	StyleMap map,
																	URL url,
																	String xpath
	) throws XPathExpressionException {
		assert w3c_document != null;
		assert map != null;
		assert url != null;
		assert xpath != null;

		Map<String, String> css_map = new HashMap<>();

		XPath xPath = XPathFactory.newInstance().newXPath();
		Node node = (Node) xPath.compile(xpath).evaluate(w3c_document, XPathConstants.NODE);
		NodeData style = map.get((org.w3c.dom.Element) node);
		if (style != null) {
			for (String property : style.getPropertyNames()) {

				if (style.getValue(property, false) == null) {
					continue;
				}
				String property_value = style.getValue(property, true).toString();

				if (property_value == null || property_value.isEmpty() || "none".equalsIgnoreCase(property_value)) {
					continue;
				}
				css_map.put(property, property_value);
			}
		}
		return css_map;
	}

	/**
	 * Loads text-related CSS properties for an element.
	 *
	 * @param element the element to load CSS properties for
	 * @return the css styles
	 *
	 * precondition: element != null
	 */
	public static Map<String, String> loadTextCssProperties(WebElement element) {
		assert element != null;

		String[] cssList = {"font-family", "font-size", "text-decoration-color", "text-emphasis-color"};
		Map<String, String> css_map = new HashMap<String, String>();

		for (String propertyName : cssList) {
			String element_value = element.getCssValue(propertyName);
			if (element_value != null && !element_value.isEmpty()) {
				css_map.put(propertyName, element_value);
			}
		}

		return css_map;
	}
}
