package com.looksee.utils;

import cz.vutbr.web.css.CSSFactory;
import cz.vutbr.web.css.RuleSet;
import cz.vutbr.web.css.StyleSheet;
import cz.vutbr.web.csskit.RuleFontFaceImpl;
import cz.vutbr.web.csskit.RuleKeyframesImpl;
import cz.vutbr.web.csskit.RuleMediaImpl;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility class for HTML parsing, cleaning, and stylesheet extraction operations.
 */
public final class HtmlUtils {

	private static Logger log = LoggerFactory.getLogger(HtmlUtils.class);

	private HtmlUtils() {
		// Utility class — prevent instantiation
	}

	/**
	 * Removes scripts, styles, links, and meta tags from HTML source and normalizes whitespace.
	 *
	 * @param src the source code to clean
	 * @return the cleaned source code
	 *
	 * precondition: src != null
	 */
	public static String cleanSrc(String src) {
		assert src != null;

		Document html_doc = Jsoup.parse(src);
		html_doc.select("script").remove();
		html_doc.select("style").remove();
		html_doc.select("link").remove();
		html_doc.select("meta").remove();

		String html = html_doc.html();
		html = html.replace("\r", "");
		html = html.replace("\n", "");
		html = html.replace("\t", " ");
		html = html.replace("  ", " ");
		html = html.replace("  ", " ");
		html = html.replace("  ", " ");

		return html.replace(" style=\"\"", "");
	}

	/**
	 * Extracts stylesheet contents from HTML source by fetching linked stylesheets.
	 *
	 * @param src the source to extract stylesheets from
	 * @return list of stylesheet contents as strings
	 *
	 * precondition: src != null
	 */
	public static List<String> extractStylesheets(String src) {
		assert src != null;

		List<String> raw_stylesheets = new ArrayList<>();
		Document doc = Jsoup.parse(src);
		Elements stylesheets = doc.select("link");
		for (Element stylesheet : stylesheets) {
			String rel_value = stylesheet.attr("rel");
			if ("stylesheet".equalsIgnoreCase(rel_value)) {
				String stylesheet_url = stylesheet.absUrl("href");
				if (stylesheet_url.trim().isEmpty()) {
					stylesheet_url = stylesheet.attr("href");
					if (stylesheet_url.startsWith("//")) {
						stylesheet_url = "https:" + stylesheet_url;
					}
				}
				try {
					log.warn("Adding stylesheet to raw stylesheets   ::   " + stylesheet_url);
					raw_stylesheets.add(NetworkUtils.readUrl(new URL(stylesheet_url)));
				} catch (KeyManagementException | NoSuchAlgorithmException e) {
					e.printStackTrace();
				} catch (MalformedURLException e1) {
					log.warn(e1.getMessage());
				} catch (IOException e1) {
					e1.printStackTrace();
				}
			}
		}

		return raw_stylesheets;
	}

	/**
	 * Parses raw CSS stylesheet strings into RuleSet objects.
	 *
	 * @param raw_stylesheets the raw stylesheets
	 * @param page_state_url the page state url
	 * @return the rule sets
	 *
	 * precondition: raw_stylesheets != null
	 * precondition: page_state_url != null
	 */
	public static List<RuleSet> extractRuleSetsFromStylesheets(List<String> raw_stylesheets, URL page_state_url) {
		assert raw_stylesheets != null;
		assert page_state_url != null;

		List<RuleSet> rule_sets = new ArrayList<>();
		for (String raw_stylesheet : raw_stylesheets) {
			try {
				StyleSheet sheet = CSSFactory.parseString(raw_stylesheet, page_state_url);
				for (int idx = 0; idx < sheet.size(); idx++) {
					if (sheet.get(idx) instanceof RuleFontFaceImpl
							|| sheet.get(idx) instanceof RuleMediaImpl
							|| sheet.get(idx) instanceof RuleKeyframesImpl) {
						continue;
					}

					RuleSet rule = (RuleSet) sheet.get(idx);
					rule_sets.add(rule);
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return rule_sets;
	}

	/**
	 * Extracts the body HTML from full page source.
	 *
	 * @param src the entire html source for a web page
	 * @return the body html
	 *
	 * precondition: src != null
	 */
	public static String extractBody(String src) {
		assert src != null;

		Document doc = Jsoup.parse(src);
		Elements body_elements = doc.select("body");
		return body_elements.html();
	}

	/**
	 * Checks if the given source contains a 503 Service Temporarily Unavailable error.
	 *
	 * @param source the source to check
	 * @return {@code true} if the source contains a 503 error, {@code false} otherwise
	 *
	 * precondition: source != null
	 */
	public static boolean is503Error(String source) {
		assert source != null;

		return source.contains("503 Service Temporarily Unavailable");
	}
}
