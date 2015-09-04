/**
 * 
 */
package wiki.toolbox.wiki;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Scanner;

import wiki.toolbox.commons.TrecTextDocument;
import wiki.toolbox.commons.TrecTextFileParser;
import wiki.toolbox.wiki.NER.NE;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;

import edu.stanford.nlp.ie.AbstractSequenceClassifier;
import edu.stanford.nlp.ie.crf.CRFClassifier;
import edu.stanford.nlp.ling.CoreLabel;

/**
 * @author wshalaby
 *
 */

class NER {
	public static final String[] NETags = new String[]{"<PERSON>","<ORGANIZATION>","<LOCATION>"};
	public static final String[] NETags_c = new String[]{"</PERSON>","</ORGANIZATION>","</LOCATION>"};
				
	public enum NE {
		PERSON,
		ORGANIZATION,
		LOCATION,
		MISC
	}
	public static String getNEString(NE ne) {
		if(ne==NE.PERSON)
			return "P";
		else if(ne==NE.ORGANIZATION)
			return "O";
		if(ne==NE.LOCATION)
			return "L";
		else if(ne==NE.MISC)
			return "M";
		
		return "M";
	}
	public static NE getNEEnum(String ne) {
		if(ne.compareTo("<PERSON>")==0)
			return NE.PERSON;
		
		if(ne.compareTo("<ORGANIZATION>")==0)
			return NE.ORGANIZATION;
		
		if(ne.compareTo("<LOCATION>")==0)
			return NE.LOCATION;
		
		return NE.MISC;
	}
}
public class WikiIndexer {

	/**
	 * @param args
	 * @throws FileNotFoundException 
	 */
	public static void main(String[] args) throws FileNotFoundException {
		// TODO Auto-generated method stub
		AbstractSequenceClassifier<CoreLabel> classifier = null;
		HashMap<String,NE>NEMap = new HashMap<String, NE>();
		
		Scanner reader = new Scanner(System.in);
		
		// get source path
		System.out.print("Enter documents source path: ");		
		String sourcePath = reader.nextLine();
		
		// get index path
		System.out.print("Enter index target path: ");		
		String indexTargetPath = reader.nextLine();
		
		// get anchors file path
		System.out.print("Enter anchors file path: (enter to skip): ");		
		String anchorsFilePath = reader.nextLine();
		
		// get page in/out link counts file path
		System.out.print("Enter page in/out link counts file path: (enter to skip): ");		
		String pageInOutCountsFilePath = reader.nextLine();
		
		// get min document length
		int minDocLength = 0;
		System.out.print("Enter minimum document size: (enter to skip): ");
		String tmp = reader.nextLine();
		if(tmp.length()>0)
			minDocLength = Integer.parseInt(tmp);
		
		// get enableSeeAlso flag
		boolean enableSeeAlso = false;
		System.out.print("Index See also titles (y/n)?: ");
		tmp = reader.nextLine();
		if(tmp.length()>0)
			enableSeeAlso = tmp.compareTo("y")==0;		
		
		// get enable category flag
		boolean enableCategoryInfo = false;
		System.out.print("Index category information (y/n)?: ");
		tmp = reader.nextLine();
		if(tmp.length()>0)
			enableCategoryInfo = tmp.compareTo("y")==0;
		
		// get enable NER flag
		boolean enableNER = false;
		System.out.print("Enable NER on titles (y/n)?: ");
		tmp = reader.nextLine();
		if(tmp.length()>0)
			enableNER = tmp.compareTo("y")==0;

		if(enableNER) {
			try {
				classifier = CRFClassifier.getClassifier("ner/classifiers/english.all.3class.distsim.crf.ser.gz");
			} catch (ClassCastException | ClassNotFoundException | IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		
		// create indexer writer & configuration
		try {
			// load anchors if any (should be anchor_text|target_title)
			HashMap<String,HashSet<String>> anchorsDic = null;
			if(anchorsFilePath.length()>0) {
				anchorsDic = new HashMap<String,HashSet<String>>();
				BufferedReader fr = new BufferedReader(new FileReader(anchorsFilePath));
				String line = fr.readLine();
				while(line!=null) {
					String tokens[] = line.split("\\|");
					if(tokens.length==3) {
						HashSet<String> s;
						if(anchorsDic.containsKey(tokens[0])==true) // title already there
							s = anchorsDic.get(tokens[0]);						
						else // new title
							s = new HashSet<String>();
						
						s.add(tokens[1]); // add anchor
						anchorsDic.put(tokens[0], s);
					}
					else
						System.out.println("improper anchor format: "+ line);

					line = fr.readLine();
				}
				fr.close();
			}
			
			// load page in/out link counts if any (should be id#_#incount#_#outcount)
			HashMap<Integer,Integer[]> pageInOutCountsDic = null;
			if(pageInOutCountsFilePath.length()>0) {
				pageInOutCountsDic = new HashMap<Integer,Integer[]>();
				BufferedReader fr = new BufferedReader(new FileReader(pageInOutCountsFilePath));
				String line = fr.readLine();
				while(line!=null) {
					String tokens[] = line.split("#_#");
					if(tokens.length==3) {
						pageInOutCountsDic.put(new Integer(Integer.parseInt(tokens[0])), 
								new Integer[]{Integer.parseInt(tokens[1]),Integer.parseInt(tokens[2])});
					}
					else
						System.out.println("improper format: "+ line);

					line = fr.readLine();
				}
				fr.close();
			}
			
			Directory dir = FSDirectory.open(new File(indexTargetPath));
			Analyzer stdAnalyzer = new StandardAnalyzer(Version.LUCENE_46);
			IndexWriterConfig cfg = new IndexWriterConfig(Version.LUCENE_46, stdAnalyzer);
			cfg.setOpenMode(IndexWriterConfig.OpenMode.CREATE);
			IndexWriter writer = new IndexWriter(dir, cfg);
			
			// loop on documents in source path and add each
			// Assumption: input format is TrecTxt
			File sourceDir = new File(sourcePath);
			if(sourceDir.isDirectory()) {
				if(enableNER==true) {
					doNER(sourceDir, classifier, NEMap);
					System.out.println("done NER for ("+NEMap.size()+") titles.");
				}
				indexDir(sourceDir, stdAnalyzer, writer, pageInOutCountsDic, anchorsDic, NEMap, 
						minDocLength, enableSeeAlso, enableCategoryInfo);
				writer.close();
				
				System.out.println("\n\nDone...");
			}
			else 
				System.out.println("Invalid document source path");
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}  
		
		reader.close();
	}

	private static void doNER(File sourceDir, AbstractSequenceClassifier<CoreLabel> classifier, 
			HashMap<String, NE> NEMap) {
		for (File curFile : sourceDir.listFiles()) {
			if(curFile.isDirectory()==true)
				doNER(curFile, classifier, NEMap);
			else {
				ArrayList<TrecTextDocument> doclist;
				try {
					System.out.println("processing file: "+curFile.getAbsolutePath());
					doclist = TrecTextFileParser.ParseFile(curFile.getAbsolutePath());
					// check documents titles
					for(int i=0; i<doclist.size(); i++) {
						TrecTextDocument trecDoc = ((TrecTextDocument)doclist.get(i));
						
						// analyze title as well as fisrt sentence of the article using NER 
						int sentence_end = trecDoc.getText().indexOf('.');
						String nerstr;
						if(sentence_end>=0) {
							nerstr = classifier.classifyWithInlineXML(
												trecDoc.getTitle()+". "+
												trecDoc.getText().substring(0, sentence_end));
							if(trecDoc.getTitle().toLowerCase().compareTo("tel solar")==0) 
								System.out.println(trecDoc.getText().substring(0, sentence_end)+"-->"+nerstr);							
						}
						else {
							nerstr = classifier.classifyWithInlineXML(
									trecDoc.getTitle()+". "+
									trecDoc.getText());							
						}
						NEMap.put(trecDoc.getTitle(), getNERInStr(trecDoc.getTitle(), nerstr));
					}
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}				
			}
		}
	}

	/**
	 * given a string produced by NER, returns what NE it contains
	 * @param title title of article
	 * @param nerstr title as sentence + first sentence of article analyzed using NER
	 * @return
	 */
	private static NE getNERInStr(String title, String nerstr) {
		String t = title.toLowerCase();
		if(t.compareTo("tel solar")==0) System.out.println(nerstr);
		String tmp;
		// check for NE in title
		for(int i=0; i<NER.NETags.length; i++) { // NETags are person, organization, location
			int start = nerstr.indexOf(NER.NETags[i]);
			while(start>=0) {
				// title should contain that NE as substring or vice versa
				tmp = nerstr.substring(start+NER.NETags[i].length(), 
					nerstr.indexOf(NER.NETags_c[i],start)).toLowerCase();
				if(t.contains(tmp) || tmp.contains(t)) {
					if(t.compareTo("tel solar")==0) System.out.println(tmp);
					return NER.getNEEnum(NER.NETags[i]);
				}
				start = nerstr.indexOf(NER.NETags[i], ++start);
			}
		}

		return NE.MISC;
	}

	/**
	 * @param sourceDir directory containing files (in trec format) 
	 * or other directories
	 * @param stdAnalyzer index analyzer
	 * @param writer	 
	 * @param pageInOutCountsDic 
	 * @param anchorsDic dictionary containing anchors for article's title
	 * @param NEMap dictionary containing NE for titles (person, organization, location...etc)
	 * @param enableSeeAlso whether to index titles in the article's "See also" section
	 * @param minDocLength minimum length required for article to be indexed
	 * @param enableCategoryInfo whether to index category information 
	 */
	private static void indexDir(File sourceDir, Analyzer stdAnalyzer,
			IndexWriter writer, HashMap<Integer, Integer[]> pageInOutCountsDic, 
			HashMap<String, HashSet<String>> anchorsDic, 
			HashMap<String, NE> NEMap, 
			int minDocLength, boolean enableSeeAlso, boolean enableCategoryInfo) {

		for (File curFile : sourceDir.listFiles()) {
			if(curFile.isDirectory()==true)
				indexDir(curFile, stdAnalyzer, writer, pageInOutCountsDic, anchorsDic, NEMap, 
						minDocLength, enableSeeAlso, enableCategoryInfo);
			else {
				ArrayList<TrecTextDocument> doclist;
				try {
					System.out.println("processing file: "+curFile.getAbsolutePath());
					doclist = TrecTextFileParser.ParseFile(curFile.getAbsolutePath());
					// add documents to index
					for(int i=0; i<doclist.size(); i++) {
						Document doc = new Document();
						TrecTextDocument trecDoc = ((TrecTextDocument)doclist.get(i));
						if((minDocLength==0 || trecDoc.getDocLen()>=minDocLength)) { // write document to index
							System.out.printf("Adding docno: %s\n", trecDoc.getDocNo());

							// set docno
							doc.add(new Field("docno", trecDoc.getDocNo(), Field.Store.YES, Field.Index.NOT_ANALYZED));							
							if(pageInOutCountsDic!=null) {
								Integer docno = new Integer(Integer.parseInt(trecDoc.getDocNo()));
								
								Integer[] inOutLinkCounts = pageInOutCountsDic.get(docno);
								if(inOutLinkCounts==null)
									inOutLinkCounts = new Integer[]{0,0};
								
								doc.add(new Field("inlinks", String.format("%06d", inOutLinkCounts[0]), Field.Store.YES, Field.Index.ANALYZED_NO_NORMS));
								doc.add(new Field("outlinks", String.format("%06d", inOutLinkCounts[1]), Field.Store.YES, Field.Index.ANALYZED_NO_NORMS));																	
							}
							
							// set url
							doc.add(new Field("url", trecDoc.getUrl(), Field.Store.YES, Field.Index.ANALYZED_NO_NORMS));							
							
							// set length
							doc.add(new Field("length", String.valueOf(trecDoc.getDocLen()), Field.Store.YES, Field.Index.ANALYZED_NO_NORMS));
							
							// set length with padding
							doc.add(new Field("padded_length", String.format("%09d", trecDoc.getDocLen()), Field.Store.YES, Field.Index.ANALYZED_NO_NORMS));
							
							// set title
							String title = trecDoc.getTitle();
							doc.add(new Field("title", title, Field.Store.YES, Field.Index.ANALYZED));
							
							// set title length
							int len = getTitleLength(title);
							doc.add(new Field("title_length", String.format("%03d", len), Field.Store.YES, Field.Index.ANALYZED_NO_NORMS));
							
							if(anchorsDic!=null && anchorsDic.containsKey(title)) { 
								// add all possible anchors as candidate titles
								Iterator<String> anchors = anchorsDic.get(title).iterator();
								while(anchors.hasNext()) {
									String anchor = anchors.next();
									doc.add(new Field("title_anchors", anchor, Field.Store.YES, Field.Index.ANALYZED));
									
									// set anchor length
									len = getTitleLength(anchor);
									doc.add(new Field("anchor_length", String.format("%03d", len), Field.Store.YES, Field.Index.ANALYZED_NO_NORMS));					
								}								
							}
							if(NEMap!=null && NEMap.containsKey(title)) { 
								// add NE for this title
								doc.add(new Field("title_ne", NER.getNEString(NEMap.get(title)), Field.Store.YES, Field.Index.NOT_ANALYZED_NO_NORMS));
							}
							
							if(enableSeeAlso && trecDoc.getSeeAlso()!=null) { // set see also
								for(String seeAlso : trecDoc.getSeeAlso()) {
									doc.add(new Field("see_also", seeAlso, Field.Store.YES, Field.Index.ANALYZED));
									
									// set seealso length
									len = getTitleLength(seeAlso);
									doc.add(new Field("seealso_length", String.format("%03d", len), Field.Store.YES, Field.Index.ANALYZED_NO_NORMS));
									
									if(NEMap!=null && NEMap.containsKey(seeAlso)) { 
										// add NE for this see also
										doc.add(new Field("see_also_ne", NER.getNEString(NEMap.get(seeAlso)), Field.Store.YES, Field.Index.NOT_ANALYZED_NO_NORMS));
									}
									else if(NEMap.containsKey(seeAlso)==false) {
										doc.add(new Field("see_also_ne", NER.getNEString(NE.MISC), Field.Store.YES, Field.Index.NOT_ANALYZED_NO_NORMS));
										System.out.println("see also ("+seeAlso+") not found in NE map");
									}
								}
							}
							
							if(enableCategoryInfo && trecDoc.getCategoryInfo()!=null) { // index category information
								for(String category : trecDoc.getCategoryInfo()) {
									doc.add(new Field("category", category, Field.Store.YES, Field.Index.ANALYZED));
								}
							}
							
							// set lines
							String lines[] = trecDoc.getText().split(System.lineSeparator());
							doc.add(new Field("line1", lines.length>0?lines[0]:"", Field.Store.YES, Field.Index.ANALYZED));
							doc.add(new Field("line2", lines.length>1?lines[1]:"", Field.Store.YES, Field.Index.ANALYZED));
							
							// set text
							doc.add(new Field("text", trecDoc.getText(), Field.Store.YES, Field.Index.ANALYZED));
							writer.addDocument(doc);
						}
						else {
							System.out.printf("Skipping docno: %s (%s)\n", trecDoc.getDocNo(),trecDoc.getTitle());
						}
					}
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
	}
	
	public static int getTitleLength(String title) {
		// TODO Auto-generated method stub
		int index = title.length(), index1, index2;
		if((index1=title.indexOf(','))==-1)
				index1 = index;
		if((index2=title.indexOf('('))==-1)
				index2 = index;
		
		index = Math.min(index1, index2);
		String s[] = title.substring(0,index).split(" ");
		return s.length;
	}
}
