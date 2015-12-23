package wiki.toolbox.commons;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

/**
 * @author wshalaby
 *
 */
public class TrecTextFileParser {

	/**
	 * @param doctxt: single document in trec text formatted file including docno, title, and text
	 * @return
	 */
	private static TrecTextDocument TokenizeTrecDocument(String doctxt) {		
		// String tmpdoctxt = doctxt.toLowerCase();
		
		TrecTextDocument doc = new TrecTextDocument();
		int start = 0;
		int end = 0;

		// add docno
		start = doctxt.indexOf("<docno>");
		end = doctxt.indexOf("</docno>");
		if(start >= 0 && end >= 0)
			doc.setDocNo(doctxt.substring(start+7, end));
		// add url
		start = doctxt.indexOf("<url>");
		end = doctxt.indexOf("</url>");
		if(start >= 0 && end >= 0)
			doc.setUrl(doctxt.substring(start+5, end));
		// add length
		start = doctxt.indexOf("<length>");
		end = doctxt.indexOf("</length>");
		if(start >= 0 && end >= 0)
			doc.setDocLen(Integer.parseInt(doctxt.substring(start+8, end)));
		// add title
		start = doctxt.indexOf("<title>");
		end = doctxt.indexOf("</title>");
		if(start >= 0 && end >= 0)
			doc.setTitle(doctxt.substring(start+7, end));
		// add see also
		start = doctxt.indexOf("<seealso>");
		while(start>=0) {
			end = doctxt.indexOf("</seealso>",start);
			if(end >= 0) {
				doc.addSeeAlso(doctxt.substring(start+9, end));				
				
				// remove see also so it will not appear in <text>
				doctxt = doctxt.replace(doctxt.substring(start,doctxt.indexOf(System.lineSeparator(),end)+System.lineSeparator().length()), "");
				start = doctxt.indexOf("<seealso>");
			}
			else 
				break;
		}
		// add category info if not same as title
		start = doctxt.indexOf("<Category>");
		while(start>=0) {
			end = doctxt.indexOf("</Category>",start);
			if(end >= 0) {
				String c = doctxt.substring(start+10, end);
				if(c.compareTo(doc.getTitle())!=0) // category is not same as title
					doc.addCategoryInfo(c);
				
				// remove category so it will not appear in <text>
				doctxt = doctxt.replace(doctxt.substring(start,doctxt.indexOf(System.lineSeparator(),end)+System.lineSeparator().length()), "");
				start = doctxt.indexOf("<Category>");
			}
			else 
				break;
		}
		// add text
		start = doctxt.indexOf("<text>");
		end = doctxt.indexOf("</text>");
		if(start >= 0 && end >= 0)
			doc.setText(doctxt.substring(start+6+System.lineSeparator().length(), end));
		//TODO: modify length in trec file to correct one
		doc.setDocLen(doc.getText().length());
		
		return doc;
	}
	
	/**
	 * @param doctxt: path for a file in trec text format
	 * @return: array containing all documents found in the file
	 */
	public static ArrayList<TrecTextDocument> ParseFile(String path) throws IOException {
		ArrayList<TrecTextDocument> docs = new ArrayList<TrecTextDocument>();
		if(!new File(path).isFile())
			throw new FileNotFoundException();
		
		BufferedReader file = new BufferedReader(new FileReader(path));
		String line;
		String doctxt = "";
		String ls = System.getProperty("line.separator");
		TrecTextDocument trecTextDoc = new TrecTextDocument();
		int i=0;
		while((line=file.readLine()) != null) {
			if(line.toLowerCase().compareTo("</doc>")==0) {
				doctxt = doctxt.concat(line);
				docs.add(TokenizeTrecDocument(doctxt));
				doctxt = "";				
			}
			else {
				doctxt = doctxt.concat(line);
				doctxt = doctxt.concat(ls);
			}
		}			
		
		return docs;		
	}
}
