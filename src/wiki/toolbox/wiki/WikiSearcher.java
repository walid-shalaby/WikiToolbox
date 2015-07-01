/**
 * @author wshalaby
 *
 */

package wiki.toolbox.wiki;

import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Scanner;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexableField;
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

public class WikiSearcher {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		WikiSearchConfig cfg = new WikiSearchConfig();
		boolean result = false;
		if(args.length==0)
		{	
			Scanner reader = new Scanner(System.in);
			
			// get index path
			System.out.print("Enter index path: ");		
			cfg.indexPath = reader.nextLine();
			
			// get search query
			System.out.print("Enter search query: ");		
			cfg.input = reader.nextLine();
	
			// get output path
			System.out.print("Enter output path: ");		
			cfg.outPath = reader.nextLine();
			
			// get search field
			System.out.print("Enter search field: ");		
			cfg.searchField = reader.nextLine();
	
			// get enable title search flag
			System.out.print("Enable title search (y/n): ");		
			cfg.enableTitleSearch = reader.nextLine().compareTo("y")==0;
			
			// get enable title search flag
			System.out.print("Enclose search query with quotes (y/n): ");		
			cfg.quotes = reader.nextLine().compareTo("y")==0? "\"":"";
	
			// get min. doc. length
			System.out.print("Min. doc. length: ");		
			cfg.minDocLen = Integer.parseInt(reader.nextLine());
			
			// get maximum hits
			System.out.print("Enter maximum hits (0 for max. allowed): ");		
			cfg.maxHits = Integer.parseInt(reader.nextLine());
			
			// get docid flag
			System.out.print("Display docid (y/n): ");		
			cfg.displayDID = reader.nextLine().compareToIgnoreCase("y")==0;
	
			// get score flag
			System.out.print("Display score (y/n): ");		
			cfg.displayScore = reader.nextLine().compareToIgnoreCase("y")==0;
			
			// get docid flag
			System.out.print("Display length (y/n): ");		
			cfg.displayLen = reader.nextLine().compareToIgnoreCase("y")==0;
	
			// get title flag
			System.out.print("Display title (y/n): ");		
			cfg.displayTitle = reader.nextLine().compareToIgnoreCase("y")==0;
			cfg.removeParen = false;
			if(cfg.displayTitle) {
				// get drop parentheses flag
				System.out.print("Drop parentheses (y/n): ");		
				cfg.removeParen = reader.nextLine().compareToIgnoreCase("y")==0;			
			}
	
			// get text flag
			System.out.print("Display text (y/n): ");		
			cfg.displayTxt = reader.nextLine().compareToIgnoreCase("y")==0;
			
			// get highlights flag
			System.out.print("Display highlights (y/n): ");		
			cfg.displayHighlights = reader.nextLine().compareToIgnoreCase("y")==0;			
			
			// get categories flag
			System.out.print("Display categories (y/n): ");		
			cfg.displayCategories = reader.nextLine().compareToIgnoreCase("y")==0;
		
			// get number of categories to display
			if(cfg.displayCategories) {
				System.out.print("Enter maximum categories (0 for max. allowed): ");		
				cfg.numCategories = Integer.parseInt(reader.nextLine());
			}
			reader.close();
			result = true;
		}
		else {
			result = cfg.parseOpts(args);
			if(result==false) 
				displayUsage();				
		}
		if(result==true) {
			try {
				// open output path
				FileWriter out = new FileWriter(new File(cfg.outPath));
				
				// open the index
				IndexReader indexReader = DirectoryReader.open(FSDirectory.open(new File(cfg.indexPath)));
				IndexSearcher searcher = new IndexSearcher(indexReader);
				Analyzer stdAnalyzer = new StandardAnalyzer(Version.LUCENE_46);
				QueryParser parser = new QueryParser(Version.LUCENE_46, "text", stdAnalyzer); //
				try {
					
					parser.setAllowLeadingWildcard(true);
					String highlight_query_str = cfg.searchField+":"+cfg.quotes+cfg.input+cfg.quotes;
					String query_str = "padded_length:["+String.format("%09d", cfg.minDocLen)+" TO *]";
			        if(cfg.enableTitleSearch) {
			          query_str += " AND (title:"+cfg.quotes+cfg.input+cfg.quotes+" OR "+cfg.searchField+":"+cfg.quotes+cfg.input+cfg.quotes+")";
			        }
			        else {
			          query_str += " AND ("+cfg.searchField+":"+cfg.quotes+cfg.input+cfg.quotes+")";
			        }
			        Query query = parser.parse(query_str);
					Query highlight_query = parser.parse(highlight_query_str);
					System.out.println("Searching (" + query + ").....");
					TopDocs topDocs = searcher.search(query, cfg.maxHits!=0?cfg.maxHits:Integer.MAX_VALUE);
					if(topDocs.totalHits > 0) {
						ScoreDoc[] hits = topDocs.scoreDocs;
						System.out.println("Results ("+hits.length+") :)");
						String data;
						int indx;
						SimpleHTMLFormatter htmlFormatter = null;
						Highlighter highlighter = null;
						if(cfg.displayHighlights) {
							htmlFormatter = new SimpleHTMLFormatter();
							highlighter = new Highlighter(htmlFormatter, new QueryScorer(highlight_query));
						}
						for(int i = 0 ; i < hits.length; i++) {
							if(cfg.displayDID) {
								out.write(String.format("\t%d", hits[i].doc));
							}
							if(cfg.displayScore) {
								out.write(String.format("\t%f", hits[i].score));
							}
							if(cfg.displayLen) {
								out.write("\t"+indexReader.document(hits[i].doc).getField("length").stringValue());
							}
							if(cfg.displayTitle) {
								data = indexReader.document(hits[i].doc).getField("title").stringValue();
								if(cfg.removeParen && (indx=data.indexOf(" ("))!=-1)
									data = indexReader.document(hits[i].doc).getField("data").stringValue().substring(0,indx);
								out.write("\t"+data);
							}
							if(cfg.displayTxt || cfg.displayHighlights) {
								String text = indexReader.document(hits[i].doc).getField("text").stringValue();
								if(cfg.displayTxt)
									out.write("\t"+text);
								if(cfg.displayHighlights) {
									TokenStream tokenStream = TokenSources.getAnyTokenStream(searcher.getIndexReader(), 
											hits[i].doc, "text", stdAnalyzer);
									TextFragment[] frag;
									try {
										frag = highlighter.getBestTextFragments(tokenStream, text, false, 10);
										for (int j = 0; j < frag.length; j++) {
											if ((frag[j] != null) && (frag[j].getScore() > 0)) {
												out.write("\t"+(frag[j].toString()));
											}
										}
									} catch (InvalidTokenOffsetsException e) {
										// TODO Auto-generated catch block
										e.printStackTrace();
									}//highlighter.getBestFragments(tokenStream, text, 3, "...");
									
								}
							}
							if(cfg.displayCategories) {
								IndexableField categories[] = indexReader.document(hits[i].doc).getFields("category");
								for(int j=0; j<categories.length && (cfg.numCategories==0 || j<cfg.numCategories); j++){ 
									out.write("\t"+categories[j].stringValue());
								}
							}
							
							out.write(System.lineSeparator()+System.lineSeparator()+System.lineSeparator());
													
							//if(indexReader.document(hits[i].doc).getField("title_ne").stringValue().compareTo("O")==0)
								
							//System.out.println("doc(" + hits[i].doc + "), score=" + hits[i].score + ", length=" + indexReader.document(hits[i].doc).getField("text").stringValue().length() + " --> " + indexReader.document(hits[i].doc).getField("title").stringValue() + " :::" + indexReader.document(hits[i].doc).getField("text").stringValue());
							//IndexableField arr[] = indexReader.document(hits[i].doc).getFields("title");
							//for (IndexableField f : arr) 
							//	System.out.println(f.stringValue());
						
						}
					}
					else 
						System.out.println("No results found :(");
				} catch (ParseException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} //			
				out.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				
				e.printStackTrace();
			}
		}
	}
	
	static void displayUsage() {
		System.out.println("java -jar wiki_searcher.jar "
				+ "--index path-to-wiki-index "
				+ "--input path--to-input-file-or-search-string "
				+ "--output path-to-output-file "
				+ "[--field target-search-field] "
				+ "[--min-len min-doc-length] "
				+ "[--max-hits max-search-hits] "
				+ "[--title-search] "
				+ "[--remove-paren] "
				+ "[--quote] "
				+ "[--write-did] "
				+ "[--write-score] "
				+ "[--write-len] "
				+ "[--write-title] "
				+ "[--write-text] "
				+ "[--write-highlights] "
				+ "[--write-categories] "
				+ "[--categories-num num-categories-to-write ]");
	}

}
