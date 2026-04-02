package com.looksee.browsing.table;

import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

/**
 * Table object for a table on a page
 */
@Getter
@Setter
@NoArgsConstructor
public class Table {
	/**
	 * The headers of the table
	 */
	public List<Row> headers;
	
	/**
	 * The rows of the table
	 */
	public List<Row> tableRows;
	
	/**
	 * Retrieves all cells in a table header passed
	 * 
	 * @param tableHeader header {@link WebElement}
	 * @param driver {@link WebDriver} to use
	 * 
	 * @return List of {@link Row}s for table header
	 * 
	 * precondition: tableHeader != null
	 * precondition: driver != null
	 */
	public List<Row> loadHeaders(WebElement tableHeader, WebDriver driver){
		assert tableHeader != null;
		assert driver != null;
				
		List<WebElement> headerRows = tableHeader.findElements(By.xpath("./tr"));
		List<Row> rows = new ArrayList<Row>();
		/*
		int cnt = 1;
		for(WebElement headerRow : headerRows){
			List<WebElement> headerCells = new ArrayList<WebElement>();

			headerCells.addAll(headerRow.findElements(By.xpath("./td")));
			headerCells.addAll(headerRow.findElements(By.xpath("./th")));
			
			Row row = new Row();
			String row_xpath = "//tr["+ cnt + "]";
			for(WebElement elem : headerCells){
				ElementState page_elem = new ElementState(elem.getText(), browser.generateXpath(elem, row_xpath, null, driver), elem.getTagName(), extractAttributes(elem, (JavascriptExecutor)driver), Browser.loadCssProperties(elem) );
				ElementNode<ElementState> node = new ElementNode<ElementState>(page_elem);
				//load all child elements into tree
				List<WebElement> children = Browser.getChildElements(elem);
				for(WebElement childElem : children){
					ElementState childPageElem = new ElementState(childElem.getText(), Browser.generateXpath(childElem, rowXpath, null, driver), childElem.getTagName(), Browser.extractAttributes(childElem, (JavascriptExecutor)driver), Browser.loadCssProperties(childElem));
					node.addChild(child_page_elem);
				}
				
				row.getRowCells().add(node);
			}
			cnt++;
		}
		*/
		return rows;
	}
	
	/**
	 * Loads the rows of a table
	 *
	 * @param tableBody {@link WebElement} of the table body
	 * @return List of {@link Row}s for table body
	 *
	 * precondition: tableBody != null
	 */
	public List<Row> loadRows(WebElement tableBody){
		assert tableBody != null;
		return null;
	}
}
