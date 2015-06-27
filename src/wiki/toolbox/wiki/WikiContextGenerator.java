/**
 * @author wshalaby
 *
 */

package wiki.toolbox.wiki;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Scanner;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.highlight.Highlighter;
import org.apache.lucene.search.highlight.InvalidTokenOffsetsException;
import org.apache.lucene.search.highlight.QueryScorer;
import org.apache.lucene.search.highlight.SimpleHTMLFormatter;
import org.apache.lucene.search.highlight.TextFragment;
import org.apache.lucene.search.highlight.TokenSources;

/*
 * Takes an input file with records in the format "sample string","class label". 
 * For each record, search for "sample string" in wikipedia. Search results 
 * represent its context in wikipedia. Found context is written to a file under 
 * folder named "class label"
 */
public class WikiContextGenerator {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		Scanner reader = new Scanner(System.in);
		
		// get index path
		System.out.print("Enter index path: ");		
		String indexPath = reader.nextLine();
		
		// get source path
		System.out.print("Enter input path: ");		
		String inpath = reader.nextLine();

		// get source path
		System.out.print("Enter output path: ");		
		String outpath = reader.nextLine();
		
		// get maximum hits
		System.out.print("Enter maximum hits (0 for max. allowed): ");		
		int maxhits = Integer.parseInt(reader.nextLine());
		
		// get docid flag
		System.out.print("Display docid (y/n): ");		
		boolean display_docid = reader.nextLine().compareToIgnoreCase("y")==0;

		// get score flag
		System.out.print("Display score (y/n): ");		
		boolean display_score = reader.nextLine().compareToIgnoreCase("y")==0;
		
		// get docid flag
		System.out.print("Display length (y/n): ");		
		boolean display_length = reader.nextLine().compareToIgnoreCase("y")==0;

		// get title flag
		System.out.print("Display title (y/n): ");		
		boolean display_title = reader.nextLine().compareToIgnoreCase("y")==0;
		boolean noparen = false;
		if(display_title) {
			// get drop parentheses flag
			System.out.print("Drop parentheses (y/n): ");		
			noparen = reader.nextLine().compareToIgnoreCase("y")==0;			
		}

		// get text flag
		System.out.print("Display text (y/n): ");		
		boolean display_text = reader.nextLine().compareToIgnoreCase("y")==0;
		
		// get highlights flag
		System.out.print("Display highlights (y/n): ");		
		boolean display_highlight = reader.nextLine().compareToIgnoreCase("y")==0;			
		
		try {
			// open the index
			IndexReader indexReader = DirectoryReader.open(FSDirectory.open(new File(indexPath)));
			IndexSearcher searcher = new IndexSearcher(indexReader);
			Analyzer stdAnalyzer = new StandardAnalyzer(Version.LUCENE_46);
			QueryParser parser = new QueryParser(Version.LUCENE_46, "text", stdAnalyzer); //
			Query query = null;
			
			// open source file
			BufferedReader in = new BufferedReader(new FileReader(inpath));
			if(in!=null)
			{
				int sep;
				String sample;
				String label;
				HashSet<String> labelsDic = new HashSet<String>();
				
				// loop on input records
				String line = in.readLine();
				while(line!=null)
				{
					// extract sample string
					sep = line.lastIndexOf(',');
					sample = line.substring(0, sep)
							.replace("\"", "")
							.replace("/", "")
							.replace("*", "")
							.replace("|", "")
							.replace("?", "")
							.replace("<", "")
							.replace(">", "")
							.replace("\\", "")
							.replace(":", "")
							.replace("-", " ")
							.replace("~","");
					
					// extract label
					label = line.substring(sep+1).replace("\"", "");
					
					// Create label directory if not there
					if(labelsDic.contains(label)==false) {
						File dir = new File(outpath+"/"+label);
						if(dir.exists()==false)
							dir.mkdirs();
						labelsDic.add(label);
					}
					
					// open output file
					File f = new File(outpath+"/"+label+"/"+sample);
					FileWriter out = new FileWriter(f);
					if(f.exists()) {
						out.write(sample+"\n");
						try {
							query = parser.parse(sample);
							System.out.println("Searching (" + query + ").....");
							TopDocs topDocs = searcher.search(query, maxhits!=0?maxhits:Integer.MAX_VALUE);
							if(topDocs.totalHits > 0) {
								ScoreDoc[] hits = topDocs.scoreDocs;
								System.out.println("Results ("+hits.length+") :)");
								String title;
								int indx;
								SimpleHTMLFormatter htmlFormatter = null;
								Highlighter highlighter = null;
								if(display_highlight) {
									htmlFormatter = new SimpleHTMLFormatter();
									highlighter = new Highlighter(htmlFormatter, new QueryScorer(query));
								}
								for(int i = 0 ; i < hits.length; i++) {
									if(display_docid) {
										out.write(String.format("\t%d", hits[i].doc));
									}
									if(display_score) {
										out.write(String.format("\t%f", hits[i].score));
									}
									if(display_length) {
										out.write("\t"+indexReader.document(hits[i].doc).getField("length").stringValue());
									}
									if(display_title) {
										title = indexReader.document(hits[i].doc).getField("title").stringValue();
										if(noparen && (indx=title.indexOf(" ("))!=-1)
											title = indexReader.document(hits[i].doc).getField("title").stringValue().substring(0,indx);
										out.write("\t"+title);
									}
									if(display_text || display_highlight) {
										String text = indexReader.document(hits[i].doc).getField("text").stringValue();
										if(display_text)
											out.write("\t"+text);
										if(display_highlight) {
											TokenStream tokenStream = TokenSources.getAnyTokenStream(searcher.getIndexReader(), 
													hits[i].doc, "text", stdAnalyzer);
											TextFragment[] frag;
											try {
												frag = highlighter.getBestTextFragments(tokenStream, text, false, 10);
												for (int j = 0; j < frag.length; j++) {
													if ((frag[j] != null) && (frag[j].getScore() > 0)) {
														out.write("\t"+(frag[j].toString()));
														out.flush();
													}
												}
											} catch (InvalidTokenOffsetsException e) {
												// TODO Auto-generated catch block
												e.printStackTrace();
											}//highlighter.getBestFragments(tokenStream, text, 3, "...");
											
										}
									}
									
									out.write(System.lineSeparator()+System.lineSeparator()+System.lineSeparator());
								}
							}
							else 
								System.out.println("No results found :(");
						} catch (ParseException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						} //
						
						out.close();
					}
					else
						System.out.println("Can't create output file for "+sample);
					
					line = in.readLine();
				}
				in.close();
			}	
			reader.close();
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			
			e.printStackTrace();
		} 
				
		

	}

}
