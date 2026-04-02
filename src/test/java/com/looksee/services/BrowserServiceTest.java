package com.looksee.services;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.looksee.models.Browser;
import com.looksee.models.enums.TemplateType;
import com.looksee.services.BrowserService;
import com.looksee.utils.HtmlGeneralizer;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.List;
import javax.xml.xpath.XPathExpressionException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.Point;
import org.springframework.beans.factory.annotation.Autowired;

//@RunWith(SpringRunner.class)
//@SpringBootTest(classes = EntryPoint.class)
public class BrowserServiceTest {
	
	@Autowired
	private BrowserService browser_service;
	
	@Test
	public void htmlCommentRemoval() {
		String html = "<html><head></head><body><div><!-- foo --><p>bar<!-- baz --></div><!--qux--></body></html>";
		
		html = HtmlGeneralizer.removeComments(html);
		System.out.println("html :: "+html);
	}
	
	@Test
	public void verifyXpathUniquification() {
		String html = "<html><head></head><body><div><!-- foo --><p>bar<!-- baz --></div><!--qux--></body></html>";
		Document html_doc = Jsoup.parse(html);

		String xpath = "//body";
		String new_xpath = BrowserService.uniqifyXpath(xpath, html_doc);
		assert("//body".equals(new_xpath));
		
		String xpath1 = "//body/div[1]";
		String new_xpath1 = BrowserService.uniqifyXpath(xpath1, html_doc);
		assert("//div[1]".equals(new_xpath1));

	}
	
	@Test
	public void isElementVisibleInPanel(){
		Browser browser = new Browser();
		browser.setViewportSize(new Dimension(1224, 844));
		
		Point location = new Point(1132, 0);
		Dimension dimension = new Dimension(80, 56);
		
		boolean is_visible = BrowserService.isElementVisibleInPane(browser, location, dimension);
		assertTrue(is_visible);
		
		browser.setXScrollOffset(0);
		browser.setYScrollOffset(0);
		browser.setViewportSize(new Dimension(1359, 903));
		
		location = new Point(852, 106);
		dimension = new Dimension(1344, 14);
		
		is_visible = BrowserService.isElementVisibleInPane(browser, location, dimension);
		assertTrue(!is_visible);
	}	

	@Test
	public void verifyTransformXpathSelectorToCss() {
		String xpath = "section[2]";
		String css_selector = BrowserService.transformXpathSelectorToCss(xpath);
		assertTrue("section:nth-child(2)".contentEquals(css_selector));
		
		String xpath2 = "header[1]";
		String css_selector2 = BrowserService.transformXpathSelectorToCss(xpath2);
		assertTrue("header:nth-child(1)".contentEquals(css_selector2));
	}
	
	@Test
	public void verifyGenerateCssSelectorFromXpath() {
		String xpath = "//body/section[2]/div[1]";
		String css_selector = BrowserService.generateCssSelectorFromXpath(xpath);
		assertTrue("body section:nth-child(2) div:nth-child(1)".contentEquals(css_selector));

		String xpath2 = "//body/header[1]/section[1]/div[1]/div[1]/div[1]";
		String css_selector2 = BrowserService.generateCssSelectorFromXpath(xpath2);
		assertTrue("body header:nth-child(1) section:nth-child(1) div:nth-child(1) div:nth-child(1) div:nth-child(1)".contentEquals(css_selector2));

	}
	
	@Test
	public void verifyExtractAllUniqueElementXpaths() throws XPathExpressionException, MalformedURLException, IOException {
		String src = "<html>"
				+ "<div></div>"
				+ "<body>"
				+ "<div><span>item1<div>*</div></span><span>item 2</span></div>"
				+ "<div>Other text here</div>"
				+ "</body></html>";
		try {
		List<String> xpaths = browser_service.extractAllUniqueElementXpaths(src);
		System.err.println(xpaths);
		}
		catch(Exception e) {
			e.printStackTrace();
		}
	}
	
	//@Test
	public void verifyListDetectionExtractsAllRepeatedItems(){
		String html = "<html>"
						+"<body>"
						+"<div>"
						+"<div id=\"item1345\" class=\"product__card\">"
							+"<div id='fhaiuhreoaf120945' class='product_img'>"
								+"<img src='noImg.jpg' />"
							+"</div>"
							+"<div>"
								+" fadsf fadsfdsfa fadsfa"
							+"</div>"
							+"<div class='functions'>"
								+"<i class='fa fa-pencil'></i>"
								+"<i  class='fa fa-times'></i>"
							+"</div>"
							+"<a href='https://qanairy.com/pricing.html' />"
							+"</div>"
							+"<div id='item2' class='product__card'>"
							+"<div id='fdyairehwafo121422' class='product_img'>"
								+"<img src='noImg.jpg' />"
							+"</div>"
							+"<div>"
								+" this is lklj holmk"
							+"</div>"
							+"<div class='functions'>"
								+"<i class='fa fa-pencil'></i>"
								+"<i  class='fa fa-times'></i>"
							+"</div>"
							+"<a href='https://qanairy.com/pricing.html' />"
							+"</div>"
							+"<div id='item3' class='product__card'>"
							+"<div id='fdkfdfhaewur1232429335' class='product_img'>"
								+"<img src='http://qanairy.com/quality/noImg3.jpg' />"
							+"</div>"
							+"<div>"
								+" this is a testafda"
							+"</div>"
							+"<div class='functions'>"
								+"<i class='fa fa-pencil'></i>"
								+"<i  class='fa fa-times'></i>"
							+"</div>"
							+"<a href='https://qanairy.com/pricing.html' />"
							+"</div>"
							+"<div id='item4' class='product__card'>"
							+"<div id='fsaf2313' class='product_img'>"
								+"<img src='http://qanairy.com/noImg2.jpg' />"
							+"</div>"
							+"<div>"
								+" fdbfsud a testafda"
							+"</div>"
							+"<div class='functions'>"
								+"<i class='fa fa-pencil'></i>"
								+"<i  class='fa fa-times'></i>"
							+"</div>"
							+"<a href='https://qanairy.com/about.html' />"
							+"</div>"
							+"<div id='item5' class='product__card'>"
							+"<div id='fsaf5672313' class='product_img'>"
								+"<img src='http://qanairy.com/noImg.jpg' />"
							+"</div>"
							+"<div>"
								+" shut up and get on with it"
							+"</div>"
							+"<div class='functions'>"
								+"<i class='fa fa-pencil'></i>"
								+"<i  class='fa fa-times'></i>"
							+"</div>"
							+"<a href='https://qanairy.com/features.html' />"
							+"</div>"
							+"<div id='item' class='product__card'>"
							+"<div class='functions'>"
								+"<i  class='fa fa-times'></i>"
							+"</div>"
							+"</div>"
						+"</div>"
						+"</body>"
					+"</html>";

		//List<ElementState> element_list = BrowserService.getAllElementsUsingJSoup(html, null);
	}
			
	//@Test
	public void testExpandAllTypeListDetectedCorrectly(){
		String html = "<html>"
						  +"<body>"
						    +"<div>"
						    +"<ul class='navbar-nav ml-auto'>"
					    	+"<li class='nav-item'>"
					        +"  <a class='nav-link' href='index.html#steps'>How It Works</a>"
					        +"</li>"
					        +"<li class='nav-item'>"
					        +"  <a class='nav-link' href='features.html'>Features</a>"
					        +"</li>"
					        +"<li class='nav-item'>"
					        +"  <a class='nav-link' href='pricing.html'>Pricing</a>"
					        +"</li>"
					        +"<li class='nav-item'>"
					        +"  <a class='nav-link' href='https://chrome.google.com/webstore/detail/qanairy-test-recorder/gaeciehbbgjmpkpeeojblplhcpemnlpo' target='_blank'>Test Recorder</a>"
					        +"</li>"
					        +"<li class='nav-item dropdown'>"
					        +"  <a class='nav-link dropdown-toggle' href='#' id='navbarDropdownMenuLink2' role='button' data-toggle='dropdown' aria-haspopup='true' aria-expanded='false'>"
					        +"    About"
					        +"  </a>"
					        +"  <div class='dropdown-menu dropdown-menu-right' aria-labelledby='navbarDropdownMenuLink2'>"
					        +"    <a class='dropdown-item' href='contact.html'>Contact</a>"
					        +"    <a class='dropdown-item' href='about.html'>About Us</a>"
					        +"  </div>"
					        +"</li>"
					        +"<li class='nav-item dropdown'>"
					        +"  <a class='nav-link dropdown-toggle' href='#' id='navbarDropdownMenuLink3' role='button' data-toggle='dropdown' aria-haspopup='true' aria-expanded='false'>"
					        +"    Resources"
					        +"  </a>"
					        +"  <div class='dropdown-menu dropdown-menu-right' aria-labelledby='navbarDropdownMenuLink3'>"
					        +"    <a class='dropdown-item' href='roadmap.html'>Release Roadmap</a>"
					        +"    <a class='dropdown-item' href='http://blog.qanairy.com/' target='_blank'>Blog</a>"
					        +"  </div>"
					        +"</li>"
					      +"</ul>"
					      +"</div>"
					      +"</body>"
						+"</html>";

		//List<ElementState> element_list = BrowserService.getAllElementsUsingJSoup(html, null);

	}

	@Test
	public void templateAtomClassificationTest(){
		String html = "<li class='nav-item'>"
						+"  <a class='nav-link' href='index.html#steps'>How It Works</a>"
						+"</li>";
		
		BrowserService browser_service = new BrowserService();
		TemplateType type = browser_service.classifyTemplate(html);
		System.err.println("Template type :: "+type);
	
		assertTrue(TemplateType.ATOM == type);
	}

	@Test
	public void templateMoleculeClassificationTest(){
		String html = "<li class='nav-item1'>"
						+ "<li class='nav-item'>"
						+ "  <a class='nav-link' href='index.html#steps'>link 1</a>"
						+ "</li>"
						+ "<li class='nav-item'>"
						+ "  <a class='nav-link' href='index.html#steps'>How It Works</a>"
						+ "</li>"
						+"</li>";
		
		BrowserService browser_service = new BrowserService();
		TemplateType type = browser_service.classifyTemplate(html);
		System.err.println("Template type :: "+type);
	
		assertTrue(TemplateType.MOLECULE == type);
	}
	
	@Test
	public void templateOrganismClassificationTest(){
		String html = "<div class='nav-item1'>"
						+ "<div class='nav-item'>"
						+ "  <a class='nav-link' href='index.html#steps'>link 1</a>"
						+ "</div>"
						+ "<div class='nav-item'>"
						+ "  <a class='nav-link' href='index.html#steps'>How It Works</a>"
						+ "</div>"
						+ "<div class='nav-item2'>"
							+ "<div class='nav-item21'>"
							+ "  <a class='nav-link' href='index.html#steps'>link 1</a>"
							+ "</div>"
							+ "<div class='nav-item21'>"
							+ "  <a class='nav-link' href='index.html#steps'>How It Works</a>"
							+ "</div>"
							+"</div>"
						+"</div>";
		
		System.err.println("browser service :: ");
		BrowserService browser_service = new BrowserService();
		System.err.println("classifying template");
		TemplateType type = browser_service.classifyTemplate(html);
		System.err.println("Template type :: "+type);
	
		assertTrue(TemplateType.ORGANISM == type);
	}
	
public String long_html = "<html>"
				  +"<body>"
				    +"<div class='test-container'>"
				    +"<div class='empty__state vertical__center_empty_state_discovery ng-hide' ng-show='!isStarted &amp;&amp; !waitingOnTests &amp;&amp; tests.length==0' aria-hidden='true'>"
					      +"<div>"
					        +"<h2>Collect your tests by first running a Discovery.</h2>"
					      +"</div>"
					      +"<div>"
					        +"<button class='btn btn--primary btn-lg' ng-click='startDiscovery()'>"
					          +"Start Discovery"
					        +"</button>"
					      +"</div>"
					    +"</div>"
					   +"<div class='test__profile vertical__left__profile' ng-click='setTestIndex($index);visible_tab='nodedata'+$index' role='button' tabindex='0'>"
					       +"<div class='test__status test__profile_stretch flex-none text--shadow' id='test0_status'>"
					         +"<div class='row test__status__frame'>"
					           +"<div class='col-xs-12 test__status ng-hide' ng-show='test.waitingOnStatusChange' aria-hidden='true'>"
					             +"<i class='fa fa-circle-o-notch fa-spin fa-3x green fa-fw' aria-hidden='true'></i>"
					           +"</div>"
					           +"<div class='flex-auto-no-padding status-passing btn--green discovery_status discovery_status_border' ng-show='!test.waitingOnStatusChange' ng-click='updateCorrectness(test, 'PASSING', $index)' role='button' tabindex='0' aria-hidden='false'>"
					             +"<div class=''>"
					                +"Passing"
					               +"<br>"
					               +"<i class='fa fa-check discovery--status--symbol white' style='{{test.status == ' passing'='' ?='' 'color:green'='' :='' 'color:#bfbfbf'}}'='' aria-hidden='true'></i>"
					             +"</div>"
					           +"</div>"
					           +"<div class='discovery_status status-failing flex-auto-no-padding btn--red' ng-show='!test.waitingOnStatusChange' ng-click='updateCorrectness(test, 'FAILING', $index)' role='button' tabindex='0' aria-hidden='false'>"
					             +"<div class=''>"
					                +"Failing"
					               +"<br>"
					               +"<i class='fa fa-times discovery--status--symbol white' style='{{!test.status == ' failing'='' &&='' test.status='' !='null' ?='' 'color:red'='' :='' 'color:#bfbfbf'}}'='' aria-hidden='true'></i>"
					             +"</div>"
					           +"</div>"
					         +"</div>"
					       +"</div>"
					       +"<div class='flex-auto test__title test_functions test__profile_stretch ng-scope' ng-click='toggleTestDataVisibility(test, $index)' ng-if='!test.show_test_name_edit_field' role='button' tabindex='0'>"
					         +"<div class='' ng-show='!test.show_test_name_edit_field' aria-hidden='false'>"
					           +"<h5 class='test__title ng-binding' ng-click='test.show_test_name_edit_field = true' role='button' tabindex='0'>"
					              +"login page loaded"
					           +"</h5>"
					           +"<i class='fa fa-pencil edithover' ng-click='editTest(test)' role='button' tabindex='0' aria-hidden='true'></i>"
					         +"</div>"
					       +"</div>"
					       +"<div class='flex-none text-right date_stats test__profile_stretch ng-binding' ng-click='toggleTestDataVisibility(test, $index)' role='button' tabindex='0'>"
										+"08/18/19 10:42 PM"
								 +"</div>"
									+"<div class='flex-none discovery_functions text-right test__btns' aria-label='Test Functions'>"
					         +"<button class='btn btn--tertiary button--ujarak' ng-click='askDelete(test)'>"
					            +"<i class='fa fa-trash' aria-hidden='true'></i>"
					         +"</button>"
					       +"</div>"
					       +"<div class='flex-none accordion-flex test__profile_stretch' ng-click='toggleTestDataVisibility(test, $index)' role='button' tabindex='0'>"
					         +"<div class='accordion' aria-label='open/close accordion'>"
					           +"<i class='fa fa-2x fa-angle-right' ng-class='test.visible ? 'fa-angle-down' : 'fa-angle-right'' aria-hidden='true'></i>"
					         +"</div>"
					       +"</div>"
								+"</div>"
						    +"<div class='row no-gutter test__dropdown ng-hide' ng-show='test.visible' aria-hidden='true'>"
									+"<div class='col-sm-12'>"
											+"<div class='tabs tabs-style-linemove'>"
												+"<nav class='path_tabs'>"
												+"	<ul>"
												+"		<li class='tabs-style tabs-style_active' ng-click='visible_test_nav1='section-linemove-1'' ng-class='visible_test_nav1=='section-linemove-1' ? 'tabs-style_active' : ''' role='button' tabindex='0'>"
					                   +"<a>"
					                     +"<h5>"
					                       +"<i class='fa test_tab_icons fa-map-signs' aria-hidden='true'></i>"
					                        +"&nbsp;  Path"
					                     +"</h5>"
					                   +"</a>"
					                 +"</li>"
									  +"<li class='tabs-style' ng-click='visible_test_nav1='section-linemove-2'' ng-class='visible_test_nav1=='section-linemove-2' ? 'tabs-style_active' : ''' role='button' tabindex='0'>"
					                   +"<a>"
					                     +"<h5>"
					                       +"<i class='fa test_tab_icons fa-object-group' aria-hidden='true'></i>"
					                        +"&nbsp;  Groups"
					                     +"</h5>"
					                   +"</a>"
					                 +"</li>"
					                 +"</ul>"
					                 +"</nav>"
										+"		<div class='content-wrap'>"
											+"			<div class='col-xs-12 test__path vertical__left__int' ng-click='visible_tab='nodedata'+$index' id='test0_data' role='button' tabindex='0'>"
												+"			<div class='node-group path-node-container vertical__center__int' ng-click='setCurrentNode(test.result, 0)' role='button' tabindex='0'>"
													+"		</div>"
													+"	</div>"
												+"</div>"
											+"</div>"
										+"</div>"
									+"</div>"
					     +"</div>"
						 +"</div>"
					   +"<div class='test ng-scope' ng-repeat='test in (filteredTests = tests) | filter:searchText | filter:{archived: false}:true | orderBy:'lastRunTimestamp' track by test.key' ng-show='tests.length>0' aria-hidden='false'>"
					     +"<div class='test__profile vertical__left__profile' ng-click='setTestIndex($index);visible_tab='nodedata'+$index' role='button' tabindex='0'>"
					       +"<div class='test__status test__profile_stretch flex-none text--shadow' id='test1_status'>"
					         +"<div class='row test__status__frame'>"
					           +"<div class='col-xs-12 test__status ng-hide' ng-show='test.waitingOnStatusChange' aria-hidden='true'>"
					             +"<i class='fa fa-circle-o-notch fa-spin fa-3x green fa-fw' aria-hidden='true'></i>"
					           +"</div>"
					           +"<div class='flex-auto-no-padding status-passing btn--green discovery_status discovery_status_border' ng-show='!test.waitingOnStatusChange' ng-click='updateCorrectness(test, 'PASSING', $index)' role='button' tabindex='0' aria-hidden='false'>"
					             +"<div class=''>"
					                +"Passing"
					               +"<br>"
					               +"<i class='fa fa-check discovery--status--symbol white' style='{{test.status == ' passing'='' ?='' 'color:green'='' :='' 'color:#bfbfbf'}}'='' aria-hidden='true'></i>"
					             +"</div>"
					           +"</div>"
					           +"<div class='discovery_status status-failing flex-auto-no-padding btn--red' ng-show='!test.waitingOnStatusChange' ng-click='updateCorrectness(test, 'FAILING', $index)' role='button' tabindex='0' aria-hidden='false'>"
					             +"<div class=''>"
					                +"Failing"
					               +"<br>"
					               +"<i class='fa fa-times discovery--status--symbol white' style='{{!test.status == ' failing'='' &&='' test.status='' !='null' ?='' 'color:red'='' :='' 'color:#bfbfbf'}}'='' aria-hidden='true'></i>"
					             +"</div>"
					           +"</div>"
					         +"</div>"
					       +"</div>"
					       +"<div class='flex-auto test__title test_functions test__profile_stretch ng-scope' ng-click='toggleTestDataVisibility(test, $index)' ng-if='!test.show_test_name_edit_field' role='button' tabindex='0'>"
					         +"<div class='' ng-show='!test.show_test_name_edit_field' aria-hidden='false'>"
					           +"<h5 class='test__title ng-binding' ng-click='test.show_test_name_edit_field = true' role='button' tabindex='0'>"
					              +"login page div click"
					           +"</h5>"
					           +"<i class='fa fa-pencil edithover' ng-click='editTest(test)' role='button' tabindex='0' aria-hidden='true'></i>"
					         +"</div>"
					       +"</div>"
								 +"<div class='flex-none text-right date_stats test__profile_stretch ng-binding' ng-click='toggleTestDataVisibility(test, $index)' role='button' tabindex='0'>"
										+"08/18/19 10:48 PM"
								 +"</div>"
								 +"<div class='flex-none discovery_functions text-right test__btns' aria-label='Test Functions'>"
					         +"<button class='btn btn--tertiary button--ujarak' ng-click='askDelete(test)'>"
					            +"<i class='fa fa-trash' aria-hidden='true'></i>"
					         +"</button>"
					       +"</div>"
					       +"<div class='flex-none accordion-flex test__profile_stretch' ng-click='toggleTestDataVisibility(test, $index)' role='button' tabindex='0'>"
					         +"<div class='accordion' aria-label='open/close accordion'>"
					           +"<i class='fa fa-2x fa-angle-right' ng-class='test.visible ? 'fa-angle-down' : 'fa-angle-right'' aria-hidden='true'></i>"
					         +"</div>"
					       +"</div>"
								+"</div>"
						    +"<div class='row no-gutter test__dropdown ng-hide' ng-show='test.visible' aria-hidden='true'>"
									+"<div class='col-sm-12'>"
											+"<div class='tabs tabs-style-linemove'>"
												+"<nav class='path_tabs'>"
													+"<ul>"
														+"<li class='tabs-style tabs-style_active' ng-click='visible_test_nav1='section-linemove-1'' ng-class='visible_test_nav1=='section-linemove-1' ? 'tabs-style_active' : ''' role='button' tabindex='0'>"
					                   +"<a>"
					                     +"<h5>"
					                       +"<i class='fa test_tab_icons fa-map-signs' aria-hidden='true'></i>"
					                        +"&nbsp;  Path"
					                     +"</h5>"
					                   +"</a>"
					                 +"</li>"
													+"	<li class='tabs-style' ng-click='visible_test_nav1='section-linemove-2'' ng-class='visible_test_nav1=='section-linemove-2' ? 'tabs-style_active' : ''' role='button' tabindex='0'>"
					                   +"<a>"
					                     +"<h5>"
					                       +"<i class='fa test_tab_icons fa-object-group' aria-hidden='true'></i>"
					                        +"&nbsp;  Groups"
					                     +"</h5>"
					                   +"</a>"
					                 +"</li>"
										+"			</ul>"
											+"	</nav>"
												+"<div class='content-wrap'>"
													+"<div id='section-linemove-1' ng-if='visible_test_nav1=='section-linemove-1'' class='ng-scope'>"
														+"<div class='col-xs-12 test__path vertical__left__int' ng-click='visible_tab='nodedata'+$index' id='test1_data' role='button' tabindex='0'>"
														+"	<div class='node-group path-node-container vertical__center__int' ng-click='setCurrentNode(test.result, 0)' role='button' tabindex='0'>"
														+"	<div ng-if='test.pathKeys.length > 1 &amp;&amp; !(test.pathKeys.length == 2 &amp;&amp; (test.pathKeys[0].includes('redirect') || test.pathKeys[0].includes('PageLoadAnimation')))' class='path-page path-node ng-scope'>"
															+"		<button class='path-page-node btn btn--tertiary button--sacnite' ng-class='current_node[test_idx]==test.result ? 'active' : '''>"
																+"		<img src='https://s3-us-west-2.amazonaws.com/qanairy/www.slack.com/acec110c8a00a4f5ceea0a772e2d7f1c29e75f9542bdd7dae8a05e2a5678b779/chrome-viewport.png' class='path-node-image' tooltip-placement='top'>"
																+"	</button>"
																+"</div>"
															+"</div>"
														+"</div>"
												+"</div>"
											+"</div>"
										+"</div>"
									+"</div>"
					     +"</div>"
						 +"</div>"
					   +"<div class='test ng-scope' ng-repeat='test in (filteredTests = tests) | filter:searchText | filter:{archived: false}:true | orderBy:'lastRunTimestamp' track by test.key' ng-show='tests.length>0' aria-hidden='false'>"
					     +"<div class='test__profile vertical__left__profile' ng-click='setTestIndex($index);visible_tab='nodedata'+$index' role='button' tabindex='0'>"
					       +"<div class='test__status test__profile_stretch flex-none text--shadow' id='test2_status'>"
					         +"<div class='row test__status__frame'>"
					           +"<div class='col-xs-12 test__status ng-hide' ng-show='test.waitingOnStatusChange' aria-hidden='true'>"
					             +"<i class='fa fa-circle-o-notch fa-spin fa-3x green fa-fw' aria-hidden='true'></i>"
					           +"</div>"
					           +"<div class='flex-auto-no-padding status-passing btn--green discovery_status discovery_status_border' ng-show='!test.waitingOnStatusChange' ng-click='updateCorrectness(test, 'PASSING', $index)' role='button' tabindex='0' aria-hidden='false'>"
					             +"<div class=''>"
					                +"Passing"
					               +"<br>"
					               +"<i class='fa fa-check discovery--status--symbol white' style='{{test.status == ' passing'='' ?='' 'color:green'='' :='' 'color:#bfbfbf'}}'='' aria-hidden='true'></i>"
					             +"</div>"
					           +"</div>"
					           +"<div class='discovery_status status-failing flex-auto-no-padding btn--red' ng-show='!test.waitingOnStatusChange' ng-click='updateCorrectness(test, 'FAILING', $index)' role='button' tabindex='0' aria-hidden='false'>"
					             +"<div class=''>"
					                +"Failing"
					               +"<br>"
					               +"<i class='fa fa-times discovery--status--symbol white' style='{{!test.status == ' failing'='' &&='' test.status='' !='null' ?='' 'color:red'='' :='' 'color:#bfbfbf'}}'='' aria-hidden='true'></i>"
					             +"</div>"
					           +"</div>"
					         +"</div>"
					       +"</div>"
					       +"<div class='flex-auto test__title test_functions test__profile_stretch ng-scope' ng-click='toggleTestDataVisibility(test, $index)' ng-if='!test.show_test_name_edit_field' role='button' tabindex='0'>"
					         +"<div class='' ng-show='!test.show_test_name_edit_field' aria-hidden='false'>"
					           +"<h5 class='test__title ng-binding' ng-click='test.show_test_name_edit_field = true' role='button' tabindex='0'>"
					              +"login page link click"
					           +"</h5>"
					           +"<i class='fa fa-pencil edithover' ng-click='editTest(test)' role='button' tabindex='0' aria-hidden='true'></i>"
					         +"</div>"
					       +"</div>"
								 +"<div class='flex-none text-right date_stats test__profile_stretch ng-binding' ng-click='toggleTestDataVisibility(test, $index)' role='button' tabindex='0'>"
										+"08/19/19 12:25 AM"
								 +"</div>"
									+"<div class='flex-none discovery_functions text-right test__btns' aria-label='Test Functions'>"
					         +"<button class='btn btn--tertiary button--ujarak' ng-click='askDelete(test)'>"
					            +"<i class='fa fa-trash' aria-hidden='true'></i>"
					         +"</button>"
					       +"</div>"
					       +"<div class='flex-none accordion-flex test__profile_stretch' ng-click='toggleTestDataVisibility(test, $index)' role='button' tabindex='0'>"
					         +"<div class='accordion' aria-label='open/close accordion'>"
					           +"<i class='fa fa-2x fa-angle-right' ng-class='test.visible ? 'fa-angle-down' : 'fa-angle-right'' aria-hidden='true'></i>"
					         +"</div>"
					       +"</div>"
								+"</div>"
						    +"<div class='row no-gutter test__dropdown ng-hide' ng-show='test.visible' aria-hidden='true'>"
									+"<div class='col-sm-12'>"
											+"<div class='tabs tabs-style-linemove'>"
												+"<nav class='path_tabs'>"
													+"<ul>"
														+"<li class='tabs-style tabs-style_active' ng-click='visible_test_nav1='section-linemove-1'' ng-class='visible_test_nav1=='section-linemove-1' ? 'tabs-style_active' : ''' role='button' tabindex='0'>"
					                   +"<a>"
					                     +"<h5>"
					                       +"<i class='fa test_tab_icons fa-map-signs' aria-hidden='true'></i>"
					                        +"&nbsp;  Path"
					                     +"</h5>"
					                   +"</a>"
					                 +"</li>"
														+"<li class='tabs-style' ng-click='visible_test_nav1='section-linemove-2'' ng-class='visible_test_nav1=='section-linemove-2' ? 'tabs-style_active' : ''' role='button' tabindex='0'>"
					                   +"<a>"
					                     +"<h5>"
					                       +"<i class='fa test_tab_icons fa-object-group' aria-hidden='true'></i>"
					                        +"&nbsp;  Groups"
					                     +"</h5>"
					                   +"</a>"
					                 +"</li>"
											+"		</ul>"
											+"	</nav>"
					+"							<div class='content-wrap'>"
						+"							<div id='section-linemove-1' ng-if='visible_test_nav1=='section-linemove-1'' class='ng-scope'>"
							+"							<div class='col-xs-12 test__path vertical__left__int' ng-click='visible_tab='nodedata'+$index' id='test2_data' role='button' tabindex='0'>"
								+"							<div class='node-group path-node-container vertical__center__int' ng-click='setCurrentNode(test.result, 0)' role='button' tabindex='0'>"
									+"							<div ng-if='test.pathKeys.length > 1 &amp;&amp; !(test.pathKeys.length == 2 &amp;&amp; (test.pathKeys[0].includes('redirect') || test.pathKeys[0].includes('PageLoadAnimation')))' class='path-page path-node ng-scope'>"
										+"							<button class='path-page-node btn btn--tertiary button--sacnite' ng-class='current_node[test_idx]==test.result ? 'active' : '''>"
											+"							<img src='https://s3-us-west-2.amazonaws.com/qanairy/staging-qanairy.auth0.com/5f1883473351892832ff71d820958c7d200ed0402950a820e9ec9c56ca5b6b5e/chrome-viewport.png' class='path-node-image' tooltip-placement='top'>"
												+"					</button>"
													+"			</div>"
														+"	</div>"
														+"</div>"
												+"</div>"
											+"</div>"
										+"</div>"
									+"</div>"
					     +"</div>"
						 +"</div>"
					   +"<div class='test ng-scope' ng-repeat='test in (filteredTests = tests) | filter:searchText | filter:{archived: false}:true | orderBy:'lastRunTimestamp' track by test.key' ng-show='tests.length>0' aria-hidden='false'>"
					     +"<div class='test__profile vertical__left__profile' ng-click='setTestIndex($index);visible_tab='nodedata'+$index' role='button' tabindex='0'>"
					       +"<div class='test__status test__profile_stretch flex-none text--shadow' id='test3_status'>"
					         +"<div class='row test__status__frame'>"
					           +"<div class='col-xs-12 test__status ng-hide' ng-show='test.waitingOnStatusChange' aria-hidden='true'>"
					             +"<i class='fa fa-circle-o-notch fa-spin fa-3x green fa-fw' aria-hidden='true'></i>"
					           +"</div>"
					           +"<div class='flex-auto-no-padding status-passing btn--green discovery_status discovery_status_border' ng-show='!test.waitingOnStatusChange' ng-click='updateCorrectness(test, 'PASSING', $index)' role='button' tabindex='0' aria-hidden='false'>"
					             +"<div class=''>"
					                +"Passing"
					               +"<br>"
					               +"<i class='fa fa-check discovery--status--symbol white' style='{{test.status == ' passing'='' ?='' 'color:green'='' :='' 'color:#bfbfbf'}}'='' aria-hidden='true'></i>"
					             +"</div>"
					           +"</div>"
					           +"<div class='discovery_status status-failing flex-auto-no-padding btn--red' ng-show='!test.waitingOnStatusChange' ng-click='updateCorrectness(test, 'FAILING', $index)' role='button' tabindex='0' aria-hidden='false'>"
					             +"<div class=''>"
					                +"Failing"
					               +"<br>"
					               +"<i class='fa fa-times discovery--status--symbol white' style='{{!test.status == ' failing'='' &&='' test.status='' !='null' ?='' 'color:red'='' :='' 'color:#bfbfbf'}}'='' aria-hidden='true'></i>"
					             +"</div>"
					           +"</div>"
					         +"</div>"
					       +"</div>"
					       +"<div class='flex-auto test__title test_functions test__profile_stretch ng-scope' ng-click='toggleTestDataVisibility(test, $index)' ng-if='!test.show_test_name_edit_field' role='button' tabindex='0'>"
					         +"<div class='' ng-show='!test.show_test_name_edit_field' aria-hidden='false'>"
					           +"<h5 class='test__title ng-binding' ng-click='test.show_test_name_edit_field = true' role='button' tabindex='0'>"
					              +"login page link click"
					           +"</h5>"
					           +"<i class='fa fa-pencil edithover' ng-click='editTest(test)' role='button' tabindex='0' aria-hidden='true'></i>"
					         +"</div>"
					       +"</div>"
								 +"<div class='flex-none text-right date_stats test__profile_stretch ng-binding' ng-click='toggleTestDataVisibility(test, $index)' role='button' tabindex='0'>"
										+"08/19/19 12:26 AM"
								 +"</div>"
								+"	<div class='flex-none discovery_functions text-right test__btns' aria-label='Test Functions'>"
					         +"<button class='btn btn--tertiary button--ujarak' ng-click='askDelete(test)'>"
					            +"<i class='fa fa-trash' aria-hidden='true'></i>"
					         +"</button>"
					       +"</div>"
					       +"<div class='flex-none accordion-flex test__profile_stretch' ng-click='toggleTestDataVisibility(test, $index)' role='button' tabindex='0'>"
					         +"<div class='accordion' aria-label='open/close accordion'>"
					           +"<i class='fa fa-2x fa-angle-right' ng-class='test.visible ? 'fa-angle-down' : 'fa-angle-right'' aria-hidden='true'></i>"
					         +"</div>"
					       +"</div>"
								+"</div>"
						    +"<div class='row no-gutter test__dropdown ng-hide' ng-show='test.visible' aria-hidden='true'>"
									+"<div class='col-sm-12'>"
										+"<div class='tabs tabs-style-linemove'>"
											+"<nav class='path_tabs'>"
												+"<ul>"
													+"<li class='tabs-style tabs-style_active' ng-click='visible_test_nav1='section-linemove-1'' ng-class='visible_test_nav1=='section-linemove-1' ? 'tabs-style_active' : ''' role='button' tabindex='0'>"
					                   +"<a>"
					                     +"<h5>"
					                       +"<i class='fa test_tab_icons fa-map-signs' aria-hidden='true'></i>"
					                        +"&nbsp;  Path"
					                     +"</h5>"
					                   +"</a>"
					                 +"</li>"
								+"		<li class='tabs-style' ng-click='visible_test_nav1='section-linemove-2'' ng-class='visible_test_nav1=='section-linemove-2' ? 'tabs-style_active' : ''' role='button' tabindex='0'>"
					                   +"<a>"
					                     +"<h5>"
					                       +"<i class='fa test_tab_icons fa-object-group' aria-hidden='true'></i>"
					                      +"  &nbsp;  Groups"
					                     +"</h5>"
					                   +"</a>"
					                 +"</li>"
					               +"</ul>"
					             +"</nav>"
					           +"<div class='content-wrap'>"
								+"					<div id='section-linemove-1' ng-if='visible_test_nav1=='section-linemove-1'' class='ng-scope'>"
									+"					<div class='col-xs-12 test__path vertical__left__int' ng-click='visible_tab='nodedata'+$index' id='test3_data' role='button' tabindex='0'>"
										+"					<div class='node-group path-node-container vertical__center__int' ng-click='setCurrentNode(test.result, 0)' role='button' tabindex='0'>"
											+"					<div ng-if='test.pathKeys.length > 1 &amp;&amp; !(test.pathKeys.length == 2 &amp;&amp; (test.pathKeys[0].includes('redirect') || test.pathKeys[0].includes('PageLoadAnimation')))' class='path-page path-node ng-scope'>"
												+"					<button class='path-page-node btn btn--tertiary button--sacnite' ng-class='current_node[test_idx]==test.result ? 'active' : '''>"
													+"					<img src='https://s3-us-west-2.amazonaws.com/qanairy/staging-qanairy.auth0.com/464847d2677c2ac6850fd3059d7a65ee48160c7a798a9a54604854479b116dfa/chrome-viewport.png' class='path-node-image' tooltip-placement='top'>"
														+"			</button>"
															+"	</div>"
															+"</div>"
														+"</div>"
												+"</div>"
											+"</div>"
										+"</div>"
									+"</div>"
					     +"</div>"
						 +"</div>"
					 +"</div>"
			      +"</body>"
				+"</html>";
}
