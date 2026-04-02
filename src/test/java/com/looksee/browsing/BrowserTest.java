package com.looksee.browsing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.looksee.browsing.helpers.BrowserConnectionHelper;
import com.looksee.models.Browser;
import com.looksee.models.enums.BrowserEnvironment;
import com.looksee.models.enums.BrowserType;
import com.looksee.services.BrowserService;
import com.looksee.utils.HtmlGeneralizer;
import com.looksee.utils.HtmlUtils;
import com.looksee.utils.NetworkUtils;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.Map;
import javax.imageio.ImageIO;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.WebElement;


/**
 *
 */
public class BrowserTest {
	
	@Test
	public void verifyCleanAttributeValuesString(){
		String src_example = "PDF: Pearson\'s Watson-Glaser II Critical Thinking Appraisal and CPP\'s CPI 260 assessment";
		String clean_src = BrowserService.cleanAttributeValues(src_example);
		System.err.println("clean src: " +clean_src);
		assertTrue("PDF: Pearson's Watson-Glaser II Critical Thinking Appraisal and CPP's CPI 260 assessment".equals(clean_src));
		
		src_example = "section[contains(@class,\"dashboard-content-wrapper \t     col-lg-10 col-lg-offset-1 \t\t    col-xs-12 col-xs-offset-0    bordered-box\")]";
		clean_src = BrowserService.cleanAttributeValues(src_example);
		System.err.println("clean src: " +clean_src);
		assertTrue("section[contains(@class,\\\"dashboard-content-wrapper col-lg-10 col-lg-offset-1 col-xs-12 col-xs-offset-0 bordered-box\\\")]".equals(clean_src));
	}

	
	@Test
	public void verifyUrlReaderForHttps() throws MalformedURLException {
		URL url = new URL("https://www.amazon.com");
		try {
			String output = NetworkUtils.readUrl(url);
			System.out.println("output           :: "+output);
			assertTrue(output!= null);
			assertTrue(!output.isEmpty());
		} catch (KeyManagementException | NoSuchAlgorithmException | IOException e) {
			e.printStackTrace();
			assertTrue(false);
		}
	}
	
	@Test
	public void verifyGenerateParentXpath(){
		System.out.println("testing 123");
		try{
			Browser browser = BrowserConnectionHelper.getConnection(BrowserType.FIREFOX, BrowserEnvironment.DISCOVERY);
			browser.navigateTo("https://look-see.com");
			WebElement element = browser.getDriver().findElement(By.xpath("//li//a[contains(@href,'features.html')]/../../.."));
			Map<String, String> attributes = browser.extractAttributes(element);

			BrowserService browser_service = new BrowserService();
			String xpath = browser_service.generateXpath(element, browser.getDriver(), attributes);
			System.err.println("XPATH :: " + xpath);
			//log.info("clean src: " +clean_src);
		//	Assert.assertTrue("concat('This is a embedded ', '\"', 'path', '\"', '')".equals(clean_src));
		}
		catch(Exception e){
			e.printStackTrace();
		}
	}
	
	//@Test
	//NOTE: Commented out because testbed url is not correct anymore
	public void scrollToElementInChrome() throws MalformedURLException{
		Browser browser = BrowserConnectionHelper.getConnection(BrowserType.CHROME, BrowserEnvironment.DISCOVERY);
		browser.navigateTo("https://qa-testbed.qanairy.com/viewport_pages/element_out_of_view_y_axis.html");

		String xpath = "//button";
		WebElement element = browser.getDriver().findElement(By.xpath(xpath));
		
		browser.scrollToElement(element);
		
		assertEquals(0, browser.getXScrollOffset());
		assertEquals(553, browser.getYScrollOffset());
	}
	
	public void verifyAttributes() throws MalformedURLException{
		int cnt = 0;
		do{
			try{
				Browser browser = BrowserConnectionHelper.getConnection(BrowserType.FIREFOX, BrowserEnvironment.DISCOVERY);
				browser.navigateTo("https://qa-testbed.qanairy.com/elements/index.html");
				WebElement element = browser.getDriver().findElement(By.xpath("//button"));
				
				Map<String, String> attributes = browser.extractAttributes(element);
				
				assertTrue(attributes.containsKey("id"));
				assertEquals(1, attributes.get("id").length());
				
				assertTrue(attributes.containsKey("class"));
				assertEquals(3, attributes.get("class").length());
				
				assertTrue(attributes.containsKey("style"));
				assertEquals(1, attributes.get("style").length());
				break;
			}
			catch(WebDriverException e){
				
			}
			cnt++;
		}while(cnt<5);
	}
	
	@Test
	public void verifyCleanSrc() {
		String src = "<html><script src=''></script><link href=''/><style>.style{}</style><head></head><body><div>This is a test</div></body></html>";
		
		String cleaned_src = HtmlUtils.cleanSrc(src);
		assertFalse(cleaned_src.contains("<script"));
		assertFalse(cleaned_src.contains("<style"));
	}
	
	@Test
	public void verifyCleanUrl() throws MalformedURLException {
		String url1 = "https://look-see.com";
		URL url = new URL(url1);
		
		System.err.println("url :: "+url.getHost());
		
	}
	
	//NOTE: This is used for manual testing to generate a full page screenshot.
	//@Test
	public void verifyFullPageScreenshot(){
		System.out.println("testing 123");
		try{
			Browser browser = BrowserConnectionHelper.getConnection(BrowserType.CHROME, BrowserEnvironment.DISCOVERY);
			browser.navigateTo("https://medium.com");
			
			BufferedImage scrn2 = browser.getFullPageScreenshotShutterbug();
			ImageIO.write(scrn2, "png", new File("/home/deepthought/fullimage-shutterbug.png"));

			//log.info("clean src: " +clean_src);
		//	Assert.assertTrue("concat('This is a embedded ', '\"', 'path', '\"', '')".equals(clean_src));
		}
		catch(Exception e){
			//e.printStackTrace();
		}
	}
	
	@Test
	public void testGeneralizeSrc() {
		String expected_result = "<html><head></head><body><app-footer></app-footer></body></html>";
		String src = "<app-footer _ngcontent-wiq-c74=\"\" class=\"footer\" _nghost-wiq-c23=\"\"></app-footer>  ";
		String generalized_src = HtmlGeneralizer.generalizeSrc(src);
		System.out.println(generalized_src);
		assertTrue(expected_result.equals(generalized_src));
	}
	
	@Test
	public void testCommentRemoval() {
		String expected_result = "<html>\n"
				+ " <head></head>\n"
				+ " <body>\n"
				+ "  <app-footer _ngcontent-wiq-c74=\"\" class=\"footer\" _nghost-wiq-c23=\"\">\n"
				+ "   <div></div>\n"
				+ "  </app-footer> \n"
				+ " </body>\n"
				+ "</html>";
		
		String src = "<app-footer _ngcontent-wiq-c74=\"\" class=\"footer\" _nghost-wiq-c23=\"\"><div></div><!-- this is a comment --></app-footer>  ";
		Document html_doc = Jsoup.parse(src);

		HtmlGeneralizer.removeComments(html_doc);
		
		assert(expected_result.equals(html_doc.html()));
	}

	@Test
	public void testCleanSrc(){
		String expected_result = "<html> <head></head> <body> <app-footer _ngcontent-wiq-c74=\"\" class=\"footer\" _nghost-wiq-c23=\"\"></app-footer> </body></html>";
		String src = "<app-footer _ngcontent-wiq-c74=\"\" class=\"footer\" _nghost-wiq-c23=\"\"></app-footer>  ";
		String generalized_src = HtmlUtils.cleanSrc(src);
		System.out.println(generalized_src);
		assertTrue(expected_result.equals(generalized_src));
	}
}
