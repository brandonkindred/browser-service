package com.looksee.utils;

import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import com.looksee.gcp.GoogleCloudStorage;
import com.looksee.gcp.GoogleCloudStorageProperties;
import com.looksee.models.Browser;
import com.looksee.models.ColorData;
import com.looksee.models.Domain;
import com.looksee.models.ElementState;
import com.looksee.models.ImageElementState;
import com.looksee.models.LookseeObject;
import com.looksee.models.PageLoadAnimation;
import com.looksee.models.PageState;
import com.looksee.models.enums.BrowserEnvironment;
import com.looksee.models.enums.BrowserType;
import com.looksee.services.BrowserService;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.SocketTimeoutException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.UnknownHostException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.Certificate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLException;
import javax.net.ssl.SSLHandshakeException;
import javax.net.ssl.SSLPeerUnverifiedException;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import lombok.NoArgsConstructor;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.openqa.grid.common.exception.GridException;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.Point;
import org.openqa.selenium.Rectangle;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.WebElement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Contains utility methods for browser operations
 */
@NoArgsConstructor
public class BrowserUtils {
	private static Logger log = LoggerFactory.getLogger(BrowserUtils.class);
	
	/**
	 * Sanitizes a url
	 * @param url the url to sanitize
	 * @param is_secure whether the url is secure
	 * @return the sanitized url
	 *
	 * precondition: url != null
	 * precondition: !url.isEmpty()
	 */
	public static String sanitizeUrl(String url, boolean is_secure) {
		assert url != null;
		assert !url.isEmpty();
		
		if(!url.contains("://")) {
			if(is_secure) {
				url = "https://"+url;
			}
			else {
				url = "http://"+url;
			}
		}
		
		url = url.replace("www.", "");
		String domain = url;
		int param_index = domain.indexOf("?");
		if(param_index >= 0){
			domain = domain.substring(0, param_index);
		}
		
		domain = domain.replace("index.html", "");
		domain = domain.replace("index.htm", "");

		if(!domain.isEmpty() && domain.charAt(domain.length()-1) == '/' && !domain.startsWith("//")){
			domain = domain.substring(0, domain.length()-1);
		}
		
		//remove any anchor link references
		int hash_index = domain.indexOf("#");
		if(hash_index > 0) {
			domain = domain.substring(0, hash_index);
		}
		return domain;
	}
	
	/**
	 * Reformats url so that it matches the Look-see requirements
	 *
	 * @param url the url to sanitize
	 * @return sanitized url string
	 *
	 * @throws MalformedURLException
	 *
	 * precondition: url != null
	 * precondition: !url.isEmpty()
	 */
	public static String sanitizeUserUrl(String url) throws MalformedURLException  {
		assert url != null;
		assert !url.isEmpty();
		
		if(!url.contains("://")) {
			url = "http://"+url;
		}
		URL new_url = new URL(url);
		//check if host is subdomain
		String new_host = new_url.getHost();
		new_host.replace("www.", "");

		String new_key = new_host+new_url.getPath();
		if(new_key.endsWith("/")){
			new_key = new_key.substring(0, new_key.length()-1);
		}
		
		new_key = new_key.replace("index.html", "");
		new_key = new_key.replace("index.htm", "");
		
		if(new_key.endsWith("/")){
			new_key = new_key.substring(0, new_key.length()-1);
		}
				
		return "http://"+new_key;
	}

	/**
	 * Updates the location of an element
	 * @param browser the browser to update the element location for
	 * @param element the element to update the location for
	 * @return the updated element
	 *
	 * precondition: browser != null
	 * precondition: element != null
	 */
	public static ElementState updateElementLocations(Browser browser, ElementState element) {
		assert browser != null;
		assert element != null;
		
		WebElement web_elem = browser.findWebElementByXpath("");
		Point location = web_elem.getLocation();
		if(location.getX() != element.getXLocation() || location.getY() != element.getYLocation()){
			element.setXLocation(location.getX());
			element.setYLocation(location.getY());
		}
		
		return element;
	}

	/**
	 * Checks if the host of a url changes
	 * @param urls the list of urls to check
	 * @return true if the host of a url changes, otherwise false
	 *
	 * precondition: urls != null
	 * @throws MalformedURLException if the url is malformed
	 */
	public static boolean doesHostChange(List<String> urls) throws MalformedURLException {
		assert urls != null;
		
		for(String url : urls){
			String last_host_and_path = "";
			URL url_obj = new URL(url);
			String host_and_path = url_obj.getHost()+url_obj.getPath();
			if(!last_host_and_path.equals(host_and_path)){
				return true;
			}
		}
		return false;
	}

	/**
	 * Checks if url is part of domain including sub-domains
	 *
	 * @param domain_host host of {@link Domain domain}
	 * @param url the url to check
	 *
	 * @return true if url is external, otherwise false
	 *
	 * precondition: domain_host != null
	 * precondition: !domain_host.isEmpty()
	 * precondition: url != null
	 * precondition: !url.isEmpty()
	 */
	public static boolean isExternalLink(String domain_host, String url) {
		assert domain_host != null;
		assert !domain_host.isEmpty();
		assert url != null;
		assert !url.isEmpty();
		
		if(url.indexOf('?') >= 0) {
			url = url.substring(0, url.indexOf('?'));
		}
		
		//remove protocol for checking same domain
		String url_without_protocol = url.replace("http://", "");
		url_without_protocol = url_without_protocol.replace("https://", "");
		boolean is_same_domain = false;
		
		boolean contains_domain = url_without_protocol.contains(domain_host);
		boolean is_url_longer = url_without_protocol.length() > domain_host.length();
		boolean url_contains_long_host = url.contains(domain_host+"/");
		if( contains_domain && ((is_url_longer && url_contains_long_host) || !is_url_longer) ) {
			is_same_domain = true;
		}
		boolean is_relative = isRelativeLink(domain_host, url);
		return (!is_same_domain && !is_relative ) || url.contains("////");
	}
	
	/**
	 * Returns true if link is empty or if it starts with a '/' and doesn't contain the domain host
	 * @param domain_host host (example: google.com)
	 * @param link_url link href value to be evaluated
	 *
	 * @return true if link is empty or if it starts with a '/' and doesn't contain the domain host, otherwise false
	 *
	 * precondition: domain_host != null
	 * precondition: !domain_host.isEmpty()
	 * precondition: link_url != null
	 */
	public static boolean isRelativeLink(String domain_host, String link_url) {
		assert domain_host != null;
		assert !domain_host.isEmpty();
		assert link_url != null;
		
		//add check that link url does not contain a host. The host does not need to be the same as the domain host. for example the domain host might be look-see.com and the link url might be shopify.dev/docs/api/customer/unstable/mutations/subscriptionContractPause
		if(containsHost(link_url)) {
			return false;
		}

		String link_without_params = link_url;
		if( link_url.indexOf('?') >= 0) {
			link_without_params = link_url.substring(0, link_url.indexOf('?'));
		}
		
		//check if link is a path by ensuring that it neither contains the/a domain host or a protocol
		return link_without_params.isEmpty()
				|| (link_without_params.charAt(0) == '/' && !link_without_params.startsWith("//") && !link_without_params.contains(domain_host))
				|| (link_without_params.charAt(0) == '?' && !link_without_params.contains(domain_host))
				|| (link_without_params.charAt(0) == '#' && !link_without_params.contains(domain_host))
				|| (!link_without_params.contains(domain_host) && !containsHost(link_url));
	}

	/**
	 * Checks provided link URL to determine if it contains a domain host
	 *
	 * @param link_url the link to check
	 *
	 * @return true if it contains a valid host format, otherwise false
	 *
	 * precondition: link_url != null
	 */
	public static boolean containsHost(String link_url) {
		assert link_url != null;
		
		String host_pattern = "([a-zA-Z0-9.-]+(:[a-zA-Z0-9.&%$-]+)*@)*((25[0-5]|2[0-4][0-9]|1[0-9]{2}|[1-9][0-9]?)(\\.(25[0-5]|2[0-4][0-9]|1[0-9]{2}|[1-9]?[0-9])){3}|([a-zA-Z0-9-]+\\.)*[a-zA-Z0-9-]+\\.(com|app|edu|gov|int|mil|net|org|biz|arpa|info|name|pro|aero|coop|museum|website|space|ca|us|co|uk|cc|es|tn|dev|us|ai))(:[0-9]+)*";
		Pattern pattern = Pattern.compile(host_pattern);
        Matcher matcher = pattern.matcher(link_url);

		return matcher.find();
	}

	/**
	 * Checks if a new host is a subdomain of a domain host
	 * @param domain_host the domain host
	 * @param new_host the new host
	 * @return true if the new host is a subdomain of the domain host, otherwise false
	 * @throws URISyntaxException
	 *
	 * precondition: domain_host != null
	 * precondition: !domain_host.isEmpty()
	 * precondition: new_host != null
	 * precondition: !new_host.isEmpty()
	 */
	public static boolean isSubdomain(String domain_host, String new_host) throws URISyntaxException {
		assert domain_host != null;
		assert !domain_host.isEmpty();
		assert new_host != null;
		assert !new_host.isEmpty();
		
		boolean is_contained = new_host.contains(domain_host) || domain_host.contains(new_host);
		boolean is_equal = new_host.equals(domain_host);
		boolean ends_with = new_host.endsWith(domain_host) || domain_host.endsWith(new_host);
		return is_contained && !is_equal && ends_with;
	}
	
	/**
	 * Checks if a url is a file
	 * @param url the url to check
	 * @return true if the url is a file, otherwise false
	 *
	 * precondition: url != null
	 * precondition: !url.isEmpty()
	 */
	public static boolean isFile(String url) {
		assert url != null;
		assert !url.isEmpty();
		
		return url.endsWith(".zip")
				|| url.endsWith(".usdt")
				|| url.endsWith(".rss")
				|| url.endsWith(".svg")
				|| url.endsWith(".pdf")
				|| url.endsWith(".m3u8") //apple file extension
				|| url.endsWith(".usdz") //apple file extension
				|| url.endsWith(".doc")
				|| url.endsWith(".docx")
				|| isVideoFile(url)
				|| isImageUrl(url);
	}
	
	/**
	 * Checks if a url is a video file
	 * @param url the url to check
	 * @return true if the url is a video file, otherwise false
	 *
	 * precondition: url != null
	 * precondition: !url.isEmpty()
	 */
	public static boolean isVideoFile(String url) {
		assert url != null;
		assert !url.isEmpty();
		
		return url.endsWith(".mov")
				|| url.endsWith(".webm")
				|| url.endsWith(".mkv")
				|| url.endsWith(".flv")
				|| url.endsWith(".vob")
				|| url.endsWith(".ogv")
				|| url.endsWith(".ogg")
				|| url.endsWith(".drc")
				|| url.endsWith(".gif")
				|| url.endsWith(".mng")
				|| url.endsWith(".avi")
				|| url.endsWith(".MTS")
				|| url.endsWith(".M2TS")
				|| url.endsWith(".TS")
				|| url.endsWith(".qt")
				|| url.endsWith(".wmv")
				|| url.endsWith(".yuv")
				|| url.endsWith(".rm")
				|| url.endsWith(".rmvb")
				|| url.endsWith(".viv")
				|| url.endsWith(".asf")
				|| url.endsWith(".amv")
				|| url.endsWith(".mp4")
				|| url.endsWith(".m4p")
				|| url.endsWith(".m4v")
				|| url.endsWith(".mpg")
				|| url.endsWith(".mpeg")
				|| url.endsWith(".m2v")
				|| url.endsWith(".mp3")
				|| url.endsWith(".mp2")
				|| url.endsWith(".mpv")
				|| url.endsWith(".m4v")
				|| url.endsWith(".svi")
				|| url.endsWith(".3gp")
				|| url.endsWith(".3g2")
				|| url.endsWith(".mxf")
				|| url.endsWith(".roq")
				|| url.endsWith(".nsv")
				|| url.endsWith(".flv")
				|| url.endsWith(".f4v")
				|| url.endsWith(".f4p")
				|| url.endsWith(".f4a")
				|| url.endsWith(".f4b");
	}

	/**
	 * Extracts a {@link List list} of link urls by looking up `a` html tags and extracting the href values
	 *
	 * @param source valid html source
	 * @return {@link List list} of link urls
	 *
	 * precondition: source != null
	 * precondition: !source.isEmpty()
	 */
	public static List<String> extractLinkUrls(String source) {
		assert source != null;
		assert !source.isEmpty();

		List<String> link_urls = new ArrayList<>();
		Document document = Jsoup.parse(source);
		Elements elements = document.getElementsByTag("a");
		
		for(Element element : elements) {
			String url = element.attr("href");
			if(!url.isEmpty()) {
				link_urls.add(url);
			}
		}
		return link_urls;
	}
	
	/**
	 * Extracts a {@link List list} of link urls by looking up `a` html tags and extracting the href values
	 *
	 * @param elements a list of {@link Element elements}
	 * @return {@link List list} of link urls
	 *
	 * precondition: elements != null
	 */
	public static List<com.looksee.models.Element> extractLinks(List<com.looksee.models.Element> elements) {
		assert elements != null;

		List<com.looksee.models.Element> links = new ArrayList<>();
		
		for(com.looksee.models.Element element : elements) {
			if(element.getName().equalsIgnoreCase("a")) {
				links.add(element);
			}
		}
		return links;
	}
	
	/**
	 * Checks if a url exists
	 * @param url the url to check
	 * @return true if the url exists, otherwise false
	 * @throws Exception
	 *
	 * precondition: url != null
	 */
	public static boolean doesUrlExist(URL url) throws Exception {
		assert(url != null);
		
		//perform check for http clients
		if("http".equalsIgnoreCase(url.getProtocol())){
			HttpURLConnection huc = (HttpURLConnection) url.openConnection();
			int responseCode = huc.getResponseCode();
			
			if (responseCode != 404) {
				return true;
			} else {
				return false;
			}
		}
		if("https".equalsIgnoreCase(url.getProtocol())){
			HttpsURLConnection https_client = getHttpsClient(url);

			try {
				int responseCode = https_client.getResponseCode();

				if (responseCode != 404) {
					return true;
				} else {
					return false;
				}
			} catch(UnknownHostException e) {
				return false;
			}
			catch(SSLException e) {
				log.warn("SSL Exception occurred while checking if URL exists");
				return false;
			}
		}
		else if("mailto".equalsIgnoreCase(url.getProtocol())) {
			//TODO check if mailto address is vailid
		}
		else {
			// TODO handle image links
		}
		
		return false;
	}
	
	/**
	 * Checks if a url exists
	 * @param url_str the url to check
	 * @return true if the url exists, otherwise false
	 * @throws Exception
	 *
	 * precondition: url_str != null
	 * precondition: !url_str.isEmpty()
	 */
	public static boolean doesUrlExist(String url_str) throws Exception {
		assert url_str != null;
		assert !url_str.isEmpty();
		
		if(BrowserUtils.isJavascript(url_str)
			|| url_str.startsWith("itms-apps:")
			|| url_str.startsWith("snap:")
			|| url_str.startsWith("tel:")
			|| url_str.startsWith("mailto:")
		) {
			return true;
		}
		
		URL url = new URL(url_str);
		//perform check for http clients
		if("http".equalsIgnoreCase(url.getProtocol())){
			HttpURLConnection huc = (HttpURLConnection) url.openConnection();
			huc.setConnectTimeout(10000);
			huc.setReadTimeout(10000);
			huc.setInstanceFollowRedirects(true);
			
			int responseCode = huc.getResponseCode();
			huc.disconnect();
			if (responseCode == 404) {
				return false;
			} else {
				return true;
			}
		}
		else if("https".equalsIgnoreCase(url.getProtocol())){
			try {
				HttpsURLConnection https_client = getHttpsClient(url);
				https_client.setConnectTimeout(10000);
				https_client.setReadTimeout(10000);
				https_client.setInstanceFollowRedirects(true);

				int response_code = https_client.getResponseCode();

				if (response_code == 404) {
					return false;
				} else {
					return true;
				}
			} catch(UnknownHostException e) {
				return false;
			}
			catch(SSLException e) {
				log.warn("SSL Exception occurred while checking if URL exists");
				return true;
			}
			catch(Exception e) {
				return false;
			}
		}
		else {
			log.warn("neither protocol is present");
			// TODO handle image links
		}
		
		return false;
	}

	/**
	 * Gets a https client
	 * @param url the url to get the https client for
	 * @return the https client
	 * @throws Exception
	 *
	 * precondition: url != null
	 */
	private static HttpsURLConnection getHttpsClient(URL url) throws Exception {
		assert url != null;
        // Security section START
        TrustManager[] trustAllCerts = new TrustManager[]{
            new X509TrustManager() {
                @Override
                public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                    return null;
                }

                @Override
                public void checkClientTrusted(
                        java.security.cert.X509Certificate[] certs, String authType) {
                }

                @Override
                public void checkServerTrusted(
                        java.security.cert.X509Certificate[] certs, String authType) {
                }
            }};

        SSLContext sc = SSLContext.getInstance("SSL");
        sc.init(null, trustAllCerts, new java.security.SecureRandom());
        HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
        // Security section END
        
        HttpsURLConnection client = (HttpsURLConnection) url.openConnection();
        client.setSSLSocketFactory(sc.getSocketFactory());
        //add request header
        client.setRequestProperty("User-Agent",
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/78.0.3904.108 Safari/537.36");
        return client;
    }
	
	/**
	 * Checks if a url is an image url
	 * @param href the url to check
	 * @return true if the url is an image url, otherwise false
	 *
	 * precondition: href != null
	 * precondition: !href.isEmpty()
	 */
	public static boolean isImageUrl(String href) {
		assert href != null;
		assert !href.isEmpty();
		
		return href.endsWith(".jpg") || href.endsWith(".png") || href.endsWith(".gif") || href.endsWith(".bmp") || href.endsWith(".tiff") || href.endsWith(".webp") || href.endsWith(".bpg") || href.endsWith(".heif");
	}
	
	/**
	 * Gets the title of a page
	 * @param page_state the page state to get the title from
	 * @return the title of the page
	 *
	 * precondition: page_state != null
	 */
	public static String getTitle(PageState page_state) {
		assert page_state != null;
		
		Document doc = Jsoup.parse(page_state.getSrc());
		
		return doc.title();
	}
	
	/**
	 * Extracts css property declarations from a css string
	 *
	 * @param prop the property to extract
	 * @param css the css string to extract the property from
	 * @return {@link List list} of css property declarations
	 *
	 * precondition: prop != null
	 * precondition: !prop.isEmpty()
	 * precondition: css != null
	 * precondition: !css.isEmpty()
	 */
	public static List<String> extractCssPropertyDeclarations(String prop, String css) {
		assert prop != null;
		assert !prop.isEmpty();
		assert css != null;
		assert !css.isEmpty();
		
		String patternString = prop+":(.*?)[?=;|}]";
		List<String> settings = new ArrayList<>();

        Pattern pattern = Pattern.compile(patternString);
        Matcher matcher = pattern.matcher(css);
        while(matcher.find()) {
			String setting = matcher.group();
			if(setting.contains("inherit")
				|| setting.contains("transparent")) {
				continue;
			}
			setting = setting.replaceAll("'", "");
			setting = setting.replaceAll("\"", "");
			setting = setting.replaceAll(";", "");
			setting = setting.replaceAll(":", "");
			setting = setting.replaceAll(":", "");
			setting = setting.replaceAll("}", "");
			setting = setting.replaceAll("!important", "");
			setting = setting.replaceAll(prop, "");

			settings.add(setting);
        }
        
        return settings;
	}

	/**
	 * Converts hexadecimal colors to RGB format
	 * @param color_str the hexadecimal color to convert
	 * @return the RGB color
	 *
	 * precondition: color_str != null
	 * precondition: !color_str.isEmpty()
	 */
	public static Color hex2Rgb(String color_str) {
		assert color_str != null;
		assert !color_str.isEmpty();

		if(color_str.contentEquals("0")) {
			return new Color(0,0,0);
		}
		if(color_str.length() == 3) {
			color_str = expandHex(color_str);
		}
		
		return new Color(
				Integer.valueOf( color_str.substring( 0, 2 ), 16 ),
				Integer.valueOf( color_str.substring( 2, 4 ), 16 ),
				Integer.valueOf( color_str.substring( 4, 6 ), 16 ) );
	}

	/**
	 * Expands a hexadecimal color string
	 * @param color_str the hexadecimal color to expand
	 * @return the expanded hexadecimal color
	 *
	 * precondition: color_str != null
	 */
	private static String expandHex(String color_str) {
		assert color_str != null;
		
		String expanded_hex = "";
		for(int idx = 0; idx < color_str.length(); idx++) {
			expanded_hex += color_str.charAt(idx)  + color_str.charAt(idx);
		}
		
		return expanded_hex;
	}

	/**
	 * Checks if a font weight is bold
	 * @param font_weight the font weight to check
	 * @return true if the font weight is bold, otherwise false
	 *
	 * precondition: font_weight != null
	 * precondition: !font_weight.isEmpty()
	 */
	public static boolean isTextBold(String font_weight) {
		assert font_weight != null;
		assert !font_weight.isEmpty();
		
		return font_weight.contentEquals("bold")
				|| font_weight.contentEquals("bolder")
				|| font_weight.contentEquals("700")
				|| font_weight.contentEquals("800")
				|| font_weight.contentEquals("900");
	}

	/**
	 * Gets the page url from a sanitized url
	 * @param sanitizedUrl the sanitized url to get the page url from
	 * @return the page url
	 *
	 * precondition: sanitizedUrl != null
	 */
	public static String getPageUrl(URL sanitizedUrl) {
		assert sanitizedUrl != null;

		String path = sanitizedUrl.getPath();
		path = path.replace("index.html", "");
		path = path.replace("index.htm", "");
		
		if("/".contentEquals(path.trim())) {
			path = "";
		}
		String pageUrl = sanitizedUrl.getHost() + path;
		
		return pageUrl.replace("www.", "");
	}

	/**
	 * Gets the page url from a sanitized url
	 * @param sanitized_url the sanitized url to get the page url from
	 * @return the page url
	 *
	 * precondition: sanitized_url != null
	 * precondition: !sanitized_url.isEmpty()
	 */
	public static String getPageUrl(String sanitized_url) {
		assert sanitized_url != null;
		assert !sanitized_url.isEmpty();
		//remove protocol
		String url_without_protocol = sanitized_url.replace("https://", "");
		url_without_protocol = url_without_protocol.replace("http://", "");
		url_without_protocol = url_without_protocol.replace("://", "");
		
		int slash_idx = url_without_protocol.indexOf('/');
		String host = "";
		String path = "";
		if(slash_idx >= 0) {
			path = url_without_protocol.substring(slash_idx);
			path = path.replace("index.html", "");
			path = path.replace("index.htm", "");
			if(path.contains("#") && !path.endsWith("?#")) {
				//strip out path parameters
				int param_idx = path.indexOf('#');
				path = path.substring(0, param_idx);
			}
			
			if(path.contains("?") && !path.endsWith("?#")) {
				//strip out path parameters
				int param_idx = path.indexOf('?');
				if(param_idx == 0) {
					path = "";
				}
				else{
					path = path.substring(0, param_idx);
				}
			}
			
			if(path.endsWith("/")) {
				path = path.substring(0, path.length()-1);
			}
			
			host = url_without_protocol.substring(0, slash_idx);
		}
		else {
			host = url_without_protocol;
		}
		
		
		String page_url = host + path;
		
		return page_url.replace("www.", "");
	}

	/**
	 * Checks the http status codes received when visiting the given url
	 *
	 * @param url the url to check
	 * @return the http status code
	 *
	 * precondition: url != null
	 */
	public static int getHttpStatus(URL url) {
		assert url != null;

		int status_code = 500;
		try {
			if(url.getProtocol().contentEquals("http")) {
				HttpURLConnection http_client = (HttpURLConnection)url.openConnection();
				http_client.setInstanceFollowRedirects(true);
				
				status_code = http_client.getResponseCode();
				return status_code;
			}
			else if(url.getProtocol().contentEquals("https")) {
				HttpsURLConnection https_client = (HttpsURLConnection)url.openConnection();
				https_client.setInstanceFollowRedirects(true);
				
				status_code = https_client.getResponseCode();
				return status_code;
			}
			else {
				log.warn("URL Protocol not found :: "+url.getProtocol());
			}
		}
		catch(SocketTimeoutException e) {
			status_code = 408;
		}
		catch(IOException e) {
			status_code = 404;
			e.printStackTrace();
		}
		
		return status_code;
	}
	
	/**
	 * Checks if the server has certificates. Expects an https protocol in the url
	 *
	 * @param url the {@link URL}
	 * @return true if the server has certificates, false otherwise
	 *
	 * @throws MalformedURLException if the url is malformed
	 *
	 * precondition: url != null
	 */
	public static boolean checkIfSecure(URL url) throws MalformedURLException {
		assert url != null;
		
        boolean is_secure = false;
        
        if(url.getProtocol().contentEquals("http")) {
			url = new URL("https://"+url.getHost()+url.getPath());
        }
        
        try{
			HttpsURLConnection con = (HttpsURLConnection)url.openConnection();
			con.setConnectTimeout(10000);
			con.setReadTimeout(10000);
			con.setInstanceFollowRedirects(true);

			con.connect();
			is_secure = con.getServerCertificates().length > 0;
        }
        catch(SSLHandshakeException e) {
			log.warn("SSLHandshakeException occurred for "+url);
        }
        catch(Exception e) {
			log.warn("an error was encountered while checking for SSL!!!!  "+url);
        	//e.printStackTrace();
        }
        
        return is_secure;
	}

	/**
	 * Checks if an element has a background color
	 * @param web_element the element to check
	 * @return true if the element has a background color, otherwise false
	 *
	 * precondition: web_element != null
	 */
	public static boolean doesElementHaveBackgroundColor(WebElement web_element) {
		assert web_element != null;
		
		String background_color = web_element.getCssValue("background-color");
		return background_color != null && !background_color.isEmpty();
	}

	/**
	 * Checks if an element has a font color
	 * @param web_element the element to check
	 * @return true if the element has a font color, otherwise false
	 *
	 * precondition: web_element != null
	 */
	public static boolean doesElementHaveFontColor(WebElement web_element) {
		assert web_element != null;
		
		String font_color = web_element.getCssValue("color");
		return font_color != null && !font_color.isEmpty();
	}

	/**
	 * Checks if an element has a background image
	 * @param web_element the element to check
	 * @return true if the element has a background image, otherwise false
	 *
	 * precondition: web_element != null
	 */
	public static boolean isElementBackgroundImageSet(WebElement web_element) {
		assert web_element != null;
		
		String background_image = web_element.getCssValue("background-image");
		return background_image != null && !background_image.trim().isEmpty() && !background_image.trim().contentEquals("none");
	}

	/**
	 * Converts a pixel size to a point size
	 * @param pixel_size the pixel size to convert
	 * @return the point size
	 */
	public static double convertPxToPt(double pixel_size) {
		return pixel_size * 0.75;
	}

	/**
	 * Checks if a url is a javascript url
	 * @param href the url to check
	 * @return true if the url is a javascript url, otherwise false
	 *
	 * precondition: href != null
	 * precondition: !href.isEmpty()
	 */
	public static boolean isJavascript(String href) {
		assert href != null;
		assert !href.isEmpty();
		
		return href.startsWith("javascript:");
	}
	
	/**
	 * Checks if an element is larger than the viewport
	 * @param element_size the size of the element
	 * @param viewportWidth the width of the viewport
	 * @param viewportHeight the height of the viewport
	 * @return true if the element is larger than the viewport, otherwise false
	 *
	 * precondition: element_size != null
	 * precondition: viewportWidth > 0
	 * precondition: viewportHeight > 0
	 */
	public static boolean isLargerThanViewport(Dimension element_size, int viewportWidth, int viewportHeight) {
		assert element_size != null;
		assert viewportWidth > 0;
		assert viewportHeight > 0;

		return element_size.getWidth() > viewportWidth || element_size.getHeight() > viewportHeight;
	}

	/**
	 * Checks if an element is larger than the viewport
	 * @param element the element to check
	 * @param viewportWidth the width of the viewport
	 * @param viewportHeight the height of the viewport
	 * @return true if the element is larger than the viewport, otherwise false
	 *
	 * precondition: element != null
	 */
	public static boolean isLargerThanViewport(ElementState element,
												int viewportWidth,
												int viewportHeight) {
		assert element != null;
		
		return element.getWidth() > viewportWidth
				|| element.getHeight() > viewportHeight;
	}

	/**
	 * Handles extra formatting for relative links
	 * @param protocol the protocol to use
	 * @param host the host to use
	 * @param href the href to use
	 * @param is_secure whether the url is secure
	 *
	 * @return the formatted url
	 * @throws MalformedURLException
	 *
	 * precondition: host != null
	 * precondition: !host.isEmpty
	 */
	public static String formatUrl(String protocol,
									String host,
									String href,
									boolean is_secure
	) throws MalformedURLException {
		assert host != null;
		assert !host.isEmpty();
		
		href = href.replaceAll(";", "").trim();
		if(href == null 
			|| href.isEmpty() 
			|| BrowserUtils.isJavascript(href)
			|| href.startsWith("itms-apps:")
			|| href.startsWith("snap:")
			|| href.startsWith("tel:")
			|| href.startsWith("mailto:")
			|| href.startsWith("applenews:") //both apple news spellings are here because its' not clear which is the proper protocol
			|| href.startsWith("applenewss:")//both apple news spellings are here because its' not clear which is the proper protocol

		) {
			return href;
		}
		
		if(is_secure) { protocol = "https"; }
		else { protocol = "http"; }
		
		//check if external link
		if(BrowserUtils.isRelativeLink(host, href)) {
			if(!href.startsWith("/") && !href.startsWith("?") && !href.startsWith("#")) {
				href = "/" + href;
			}
			href = protocol + "://" + host + href;
		}
		else if( isSchemeRelative(host, href)) {
			href = protocol + href;
		}
		return href;
	}

	/**
	 * Checks if a url is scheme relative
	 * @param host the host to check
	 * @param href the href to check
	 * @return true if the url is scheme relative, otherwise false
	 *
	 * precondition: host != null
	 * precondition: href != null
	 */
	private static boolean isSchemeRelative(String host, String href) {
		assert host != null;
		assert href != null;
		
		return href.startsWith("//");
	}

	/**
	 * Check if the url begins with a valid protocol and is in the valid format.
	 * Also check if the url is an external link by comparing it to a host name.
	 *
	 * @param sanitized_url A sanitized url, such as https://look-see.com
	 * @param host The host website, such as look-see.com
	 * @return {@code boolean}
	 *
	 * precondition: sanitized_url != null
	 * precondition: !sanitized_url.isEmpty()
	 * precondition: host != null
	 * precondition: !host.isEmpty()
	 */
	public static boolean isValidUrl(String sanitized_url, String host)
	{
		assert sanitized_url != null;
		assert !sanitized_url.isEmpty();
		assert host != null;
		assert !host.isEmpty();

		if(BrowserUtils.isFile(sanitized_url)
			|| BrowserUtils.isJavascript(sanitized_url)
			|| sanitized_url.startsWith("itms-apps:")
			|| sanitized_url.startsWith("snap:")
			|| sanitized_url.startsWith("tel:")
			|| sanitized_url.startsWith("mailto:")
			|| sanitized_url.startsWith("applenews:")
			|| sanitized_url.startsWith("applenewss:")
			|| sanitized_url.startsWith("mailto:")
			|| BrowserUtils.isExternalLink(host, sanitized_url)){
			return false;
		}
		else {
			return true;
		}
	}

	/**
	 * Check to see if a link extracted from an href is empty or begins with a valid protocol.
	 * 
	 * @param href_str An href link from a page source.
	 * @return {@code boolean} True if valid, false if invalid
	 */
	public static boolean isValidLink(String href_str){
		if(href_str == null
				|| href_str.isEmpty()
				|| BrowserUtils.isJavascript(href_str)
				|| href_str.startsWith("itms-apps:")
				|| href_str.startsWith("snap:")
				|| href_str.startsWith("tel:")
				|| href_str.startsWith("mailto:")
				|| BrowserUtils.isFile(href_str)){
			return false;
		}
		else {
			return true;
		}
	}

	/**
	 * Check if the sanitized url returns a valid http status code.
	 *
	 * @param sanitized_url the sanitized url
	 * @return {@code boolean} True if valid, false if page is not found.
	 *
	 * precondition: sanitized_url != null
	 */
	public static boolean hasValidHttpStatus(URL sanitized_url){
		assert sanitized_url != null;

		//Check http status to ensure page exists before trying to extract info from page
		int http_status = BrowserUtils.getHttpStatus(sanitized_url);

		//usually code 301 is returned which is a redirect, which is usually transferring to https
		if(http_status == 404 || http_status == 408) {
			return false;
		}
		else {
			return true;
		}
	}

	/**
	 * Retrieves {@link ElementState}s that contain text
	 *
	 * @param element_states the {@link List} of {@link ElementState}s
	 * @return the {@link List} of {@link ElementState}s
	 *
	 * precondition: element_states != null
	 */
	public static List<ElementState> getTextElements(List<ElementState> element_states) {
		assert element_states != null;
		
		boolean parent_found = false;
		
		List<ElementState> elements = element_states.parallelStream()
													.filter(p -> p.getOwnedText() != null
																	&& !p.getOwnedText().isEmpty()
																	&& !p.getOwnedText().trim().isEmpty())
													.distinct()
													.collect(Collectors.toList());
		//remove all elements that are part of another element
		List<ElementState> filtered_elements = new ArrayList<>();
		for(ElementState element1: elements) {
			for(ElementState element2: elements) {
				if(!element1.equals(element2) 
						&& element2.getAllText().contains(element1.getAllText())
						&& element2.getXpath().contains(element1.getXpath())) {
					parent_found = true;
					break;
				}
			}
			if(!parent_found) {
				filtered_elements.add(element1);
			}
		}
		
		return filtered_elements;
	}

	/**
	 * Retrieves {@link ImageElementState}s that contain text
	 *
	 * @param element_states the {@link List} of {@link ElementState}s
	 * @return the {@link List} of {@link ImageElementState}s
	 *
	 * precondition: element_states != null
	 */
	public static List<ImageElementState> getImageElements(List<ElementState> element_states) {
		assert element_states != null;
		
		List<ElementState> elements = element_states.parallelStream().filter(p ->p.getName().equalsIgnoreCase("img")).distinct().collect(Collectors.toList());
		
		List<ImageElementState> img_elements = new ArrayList<>();
		for(ElementState element : elements) {
			img_elements.add((ImageElementState)element);
		}
		
		return img_elements;
	}
	
	/**
	 * Checks if a {@link WebElement element} is currently hidden
	 *
	 * @param web_element {@link WebElement element}
	 *
	 * @return returns true if it is hidden, otherwise returns false
	 *
	 * precondition: web_element != null
	 */
	public static boolean isHidden(WebElement web_element) {
		assert web_element != null;

		Rectangle rect = web_element.getRect();
		return rect.getX()<=0 && rect.getY()<=0 && rect.getWidth()<=0 && rect.getHeight()<=0;
	}

	/**
	 * Checks if an element is currently hidden based on its location and size
	 *
	 * @param location the {@link Point} location of the element
	 * @param size the {@link Dimension} size of the element
	 *
	 * @return returns true if it is hidden, otherwise returns false
	 *
	 * precondition: location != null
	 * precondition: size != null
	 */
	public static boolean isHidden(Point location, Dimension size) {
		assert location != null;
		assert size != null;

		return location.getX()<=0 && location.getY()<=0 && size.getWidth()<=0 && size.getHeight()<=0;
	}
	
	/**
	 * Opens stylesheet content and searches for font-family css settings
	 * 
	 * @param stylesheet The stylesheet to extract font families from
	 * @return A set of font families
	 * 
	 * precondition: stylesheet != null
	 * precondition: !stylesheet.isEmpty()
	 */
	public static Collection<? extends String> extractFontFamiliesFromStylesheet(String stylesheet) {
		assert stylesheet != null;
		assert !stylesheet.isEmpty();
		
		Map<String, Boolean> font_families = new HashMap<>();

		//extract text matching font-family:.*; from stylesheets
		//for each match, extract entire string even if it's a list and add string to font-families list
		String patternString = "font-family:(.*?)[?=;|}]";

        Pattern pattern = Pattern.compile(patternString);
        Matcher matcher = pattern.matcher(stylesheet);
        while(matcher.find()) {
        	String font_family_setting = matcher.group();
        	if(font_family_setting.contains("inherit")) {
        		continue;
        	}
        	font_family_setting = font_family_setting.replaceAll("'", "");
        	font_family_setting = font_family_setting.replaceAll("\"", "");
        	font_family_setting = font_family_setting.replaceAll(";", "");
        	font_family_setting = font_family_setting.replaceAll(":", "");
        	font_family_setting = font_family_setting.replaceAll(":", "");
        	font_family_setting = font_family_setting.replaceAll("}", "");
        	font_family_setting = font_family_setting.replaceAll("!important", "");
        	font_family_setting = font_family_setting.replaceAll("font-family", "");
        	
        	font_families.put(font_family_setting.trim(), Boolean.TRUE);
        }
        
        return font_families.keySet();
	}

	/**
	 * Extracts set of colors declared as background or text color in the css
	 * 
	 * @param stylesheet the stylesheet to extract colors from
	 * @return the set of colors declared as background or text color in the css
	 * 
	 * precondition: stylesheet != null
	 * precondition: !stylesheet.isEmpty()
	 */
	public static Collection<? extends ColorData> extractColorsFromStylesheet(String stylesheet) {
		assert stylesheet != null;
		assert !stylesheet.isEmpty();
		
		List<ColorData> colors = new ArrayList<>();

		//extract text matching font-family:.*; from stylesheets
		//for each match, extract entire string even if it's a list and add string to font-families list
		for(String prop_setting : extractCssPropertyDeclarations("background-color", stylesheet)) {
			if(prop_setting.startsWith("#")) {
				
				Color color = hex2Rgb(prop_setting.trim().substring(1));
				colors.add(new ColorData(color.getRed() + ","+color.getGreen()+","+color.getBlue()));
			}
			else if( prop_setting.startsWith("rgb") ){
				colors.add(new ColorData(prop_setting));
			}
        }

        for(String prop_setting : extractCssPropertyDeclarations("color", stylesheet)) {
			if(prop_setting.startsWith("#")) {
				Color color = hex2Rgb(prop_setting.trim().substring(1));
				colors.add(new ColorData(color.getRed() + ","+color.getGreen()+","+color.getBlue()));
			}
			else if( prop_setting.startsWith("rgb") ){
				colors.add(new ColorData(prop_setting));
			}
        }
        
        return colors;
	}

	/**
	 * Prints the https certificate information
	 * @param con The https connection to print the certificate information for
	 */
	private static void print_https_cert(HttpsURLConnection con){
		
		if(con!=null){
			try {
					
				System.out.println("Cipher Suite : " + con.getCipherSuite());
				System.out.println("\n");
							
				Certificate[] certs = con.getServerCertificates();
				for(Certificate cert : certs){
					System.out.println("Cert Type : " + cert.getType());
					System.out.println("Cert Hash Code : " + cert.hashCode());
					System.out.println("Cert Public Key Algorithm : "
												+ cert.getPublicKey().getAlgorithm());
					System.out.println("Cert Public Key Format : "
												+ cert.getPublicKey().getFormat());
					System.out.println("\n");
				}
						
			} catch (SSLPeerUnverifiedException e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * Extracts the page source from the URL.
	 * Attempts to connect to the browser service, then navigates to the url and extracts the source.
	 * 
	 * @param sanitized_url The sanitized URL that contains the page source
	 * @param browser_service The browser service to use to extract the page source
	 * @return {@code String} The page source
	 * 
	 * precondition: sanitized_url != null
	 * precondition: browser_service != null
	 */
	public static String extractPageSrc(URL sanitized_url, BrowserService browser_service){
		assert sanitized_url != null;
		assert browser_service != null;

		//Extract page source from url
		int attempt_cnt = 0;
		String page_src = "";
		
		do {
			Browser browser = null;
			try {
				browser = browser_service.getConnection(BrowserType.CHROME, BrowserEnvironment.DISCOVERY);
				browser.navigateTo(sanitized_url.toString());
				
				sanitized_url = new URL(browser.getDriver().getCurrentUrl());
				page_src = browser_service.getPageSource(browser, sanitized_url);
				attempt_cnt = 10000000;
				break;
			}
			catch(MalformedURLException e) {
				log.warn("Malformed URL exception occurred for  "+sanitized_url);
				break;
			}
			catch(WebDriverException | GridException e) {
				log.warn("failed to obtain page source during crawl of :: "+sanitized_url);
			}
			finally {
				if(browser != null) {
					browser.close();
				}
			}
		} while (page_src.trim().isEmpty() && attempt_cnt < 1000);

		return page_src;
	}

	
	/**
	 * Watches for an animation that occurs during page load
	 * 
	 * @param browser the browser to watch
	 * @param host the host of the page
	 * @return the page load animation
	 * 
	 * @throws IOException if there is an error reading the page source
	 * @throws NoSuchAlgorithmException if the algorithm is not found
	 * 
	 * precondition: browser != null
	 * precondition: host != null
	 * precondition: host != empty
	 */
	public static PageLoadAnimation getLoadingAnimation(Browser browser,
														String host
	) throws IOException, NoSuchAlgorithmException {
		assert browser != null;
		assert host != null;
		assert !host.isEmpty();
		
		List<String> image_checksums = new ArrayList<String>();
		List<String> image_urls = new ArrayList<String>();
		boolean transition_detected = false;
		long start_ms = System.currentTimeMillis();
		long total_time = System.currentTimeMillis();
		
		Map<String, Boolean> animated_state_checksum_hash = new HashMap<String, Boolean>();
		String last_checksum = null;
		String new_checksum = null;

		String bucketName = "web-images";
		String publicUrl = "https://storage.googleapis.com/web-images";
		do{
			//get element screenshot
			BufferedImage screenshot = browser.getViewportScreenshot();
			
			//calculate screenshot checksum
			new_checksum = PageState.getFileChecksum(screenshot);
		
			transition_detected = !new_checksum.equals(last_checksum);

			if( transition_detected ){
				if(animated_state_checksum_hash.containsKey(new_checksum)){
					return null;
				}
				image_checksums.add(new_checksum);
				animated_state_checksum_hash.put(new_checksum, Boolean.TRUE);
				last_checksum = new_checksum;
				GoogleCloudStorageProperties properties = new GoogleCloudStorageProperties();
				properties.setBucketName(bucketName);
				properties.setPublicUrl(publicUrl);
				Storage storage_options = StorageOptions.getDefaultInstance().getService();

				GoogleCloudStorage storage = new GoogleCloudStorage( storage_options, properties);
				image_urls.add(storage.saveImage(screenshot,
												host,
												new_checksum,
												BrowserType.create(browser.getBrowserName())));
			}
		}while((System.currentTimeMillis() - start_ms) < 1000 && (System.currentTimeMillis() - total_time) < 10000);
		
		if(!transition_detected && new_checksum.equals(last_checksum) && image_checksums.size()>2){
			return new PageLoadAnimation(image_urls,
										image_checksums,
										BrowserUtils.sanitizeUrl(browser.getDriver().getCurrentUrl(), true));
		}

		return null;
	}
	
	/**
	 * Checks if the start and end urls span multiple domains
	 *
	 * @param start_url the start url
	 * @param end_url the end url
	 * @param path_objects the path objects
	 * @return true if the start and end urls span multiple domains, false otherwise
	 *
	 * @throws MalformedURLException if the url is malformed
	 *
	 * precondition: start_url != null
	 * precondition: !start_url.isEmpty()
	 * precondition: end_url != null
	 * precondition: !end_url.isEmpty()
	 * precondition: path_objects != null
	 */
	public static boolean doesSpanMutlipleDomains(String start_url, String end_url, List<LookseeObject> path_objects) throws MalformedURLException {
		assert start_url != null;
		assert !start_url.isEmpty();
		assert end_url != null;
		assert !end_url.isEmpty();
		assert path_objects != null;

		return !(start_url.trim().contains(new URL(end_url).getHost()) || end_url.contains((new URL(PathUtils.getLastPageStateOLD(path_objects).getUrl()).getHost())));
	}
}
