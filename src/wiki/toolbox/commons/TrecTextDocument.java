/**
 * 
 */
package wiki.toolbox.commons;

import java.util.ArrayList;

/**
 * @author wshalaby
 *
 */
public class TrecTextDocument {
	private String docNo = "";
	private String title = "";
	private String url = "";
	private int length = 0;
	private String Text = "";
	
	private ArrayList<String> seeAlsoLst = null;
	private ArrayList<String> CategoryInfoLst = null;
	
	/**
	 * @return the docNo
	 */
	public String getDocNo() {
		return docNo;
	}
	/**
	 * @param url the url to set
	 */
	public void setUrl(String url) {
		this.url = url;
	}
	/**
	 * @return the url
	 */
	public String getUrl() {
		return url;
	}
	/**
	 * @param docNo the docNo to set
	 */
	public void setDocNo(String docNo) {
		this.docNo = docNo;
	}
	/**
	 * @return the title
	 */
	public String getTitle() {
		return title;
	}
	/**
	 * @param title the title to set
	 */
	public void setTitle(String title) {
		this.title = title;
	}
	/**
	 * @return the text
	 */
	public String getText() {
		return Text;
	}
	/**
	 * @param text the text to set
	 */
	public void setText(String text) {
		Text = text;
	}
	
	/**
	 * @return the length
	 */
	public int getDocLen() {
		return length;
	}
	/**
	 * @param length the length to set
	 */
	public void setDocLen(int length) {
		this.length = length;
	}

	/**
	 * @return See also list
	 */
	public ArrayList<String> getSeeAlso() {
		return seeAlsoLst;
	}

	/**
	 * @return Category Info list
	 */
	public ArrayList<String> getCategoryInfo() {
		return CategoryInfoLst;
	}

	/**
	 * @param stext see also text to add
	 */
	public void addSeeAlso(String stext) {
		if(seeAlsoLst==null)
			seeAlsoLst = new ArrayList<String>();
		
		if(stext.contains(" –"))
			stext = stext.split(" –")[0];
		if(stext.contains(" - "))
			stext = stext.split(" - ")[0];
		else if(stext.contains(", "))
			stext = stext.split(", ")[0];
		
		seeAlsoLst.add(stext);
	}
	
	/**
	 * @param category category to add
	 */
	public void addCategoryInfo(String category) {
		if(CategoryInfoLst==null)
			CategoryInfoLst = new ArrayList<String>();
		CategoryInfoLst.add(category);
	}
}
