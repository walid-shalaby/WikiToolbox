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
import java.util.concurrent.Callable;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

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

/*
 * Takes an input file with records in the format "sample string","class label". 
 * For each record, search for "sample string" in wikipedia. Search results 
 * represent its context in wikipedia. Found context is written to a file under 
 * folder named "class label"
 */

class ContextGeneratorThread implements Runnable {
	WikiSearchConfig cfg;
	Analyzer stdAnalyzer;
	QueryParser parser;
	IndexReader indexReader;
	IndexSearcher searcher;
	String[] samples;
	String[] labels;
	public ContextGeneratorThread(String[] s, String[] l, 
			WikiSearchConfig c, IndexReader r, IndexSearcher sh) {
		// TODO Auto-generated constructor stub
		cfg = c;
		samples = s;
		labels = l;
		stdAnalyzer = new StandardAnalyzer(Version.LUCENE_46);
		parser = new QueryParser(Version.LUCENE_46, "text", stdAnalyzer); //
		indexReader = r;
		searcher = sh;
	}
	
	@Override
	public void run() {
		// open output file
		for(int s=0; s<samples.length; s++)
		{
			String sample = samples[s];
			String label = labels[s];
			try {
				File f = new File(cfg.outPath+"/"+label+"/"+sample);
				FileWriter out = new FileWriter(f);
				if(f.exists()) {
					out.write(sample+"\n");
					try {
						String highlight_query_str = cfg.searchField+":"+cfg.quotes+sample+cfg.quotes;
					    String query_str = "padded_length:["+String.format("%09d", cfg.minDocLen)+" TO *]";
				        if(cfg.enableTitleSearch) {
				          query_str += " AND (title:"+cfg.quotes+sample+cfg.quotes+" OR "+cfg.searchField+":"+cfg.quotes+sample+cfg.quotes+")";
				        }
				        else {
				          query_str += " AND ("+cfg.searchField+":"+cfg.quotes+sample+cfg.quotes+")";
				        }
					
						Query query = parser.parse(query_str);
						Query highlight_query = parser.parse(highlight_query_str);
						
						if(cfg.debug==true)
							System.out.println("Searching (" + query + ").....");
						TopDocs topDocs = searcher.search(query, cfg.maxHits!=0?cfg.maxHits:Integer.MAX_VALUE);
						if(topDocs.totalHits > 0) {
							ScoreDoc[] hits = topDocs.scoreDocs;
							if(cfg.debug==true)
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
										data = indexReader.document(hits[i].doc).getField("title").stringValue().substring(0,indx);
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
													out.flush();
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
							}
						}
						else if(cfg.debug==true) 
							System.out.println("No results found :(");
					} catch (ParseException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					} //
					
					out.close();
				}
				else
					System.out.println("Can't create output file for "+sample);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				
				e.printStackTrace();
			}
		}
	}
}

public class WikiContextGenerator {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		WikiSearchConfig cfg = new WikiSearchConfig();
		if(cfg.parseOpts(args)==false)
			displayUsage();
		else
		{
			try {
				// open the index
				IndexReader indexReader = DirectoryReader.open(FSDirectory.open(new File(cfg.indexPath)));
				IndexSearcher searcher = new IndexSearcher(indexReader);
				
				// open source file
				BufferedReader in = new BufferedReader(new FileReader(cfg.input));
				if(in!=null)
				{
					ExecutorService executor = Executors.newFixedThreadPool(cfg.numThreads);
					
					int sep;
					String[] samples = new String[cfg.blockSize];
					String[] labels = new String[cfg.blockSize];
					HashSet<String> labelsDic = new HashSet<String>();
					
					// loop on input records
					int lineNo = 0;
					String line = in.readLine();
					while(line!=null)
					{
						// extract sample string
						sep = line.lastIndexOf(',');
						samples[lineNo] = line.substring(0, sep)
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
						labels[lineNo] = line.substring(sep+1).replace("\"", "");
						
						// Create label directory if not there
						if(labelsDic.contains(labels[lineNo])==false) {
							File dir = new File(cfg.outPath+"/"+labels[lineNo]);
							if(dir.exists()==false)
								dir.mkdirs();
							labelsDic.add(labels[lineNo]);
						}
						if(++lineNo%cfg.blockSize==0) {
							executor.execute(new ContextGeneratorThread(samples, labels, cfg, indexReader, searcher));
							samples =  new String[cfg.blockSize];
							labels = new String[cfg.blockSize];
							lineNo = 0;
						}
						line = in.readLine();
					}
					if(lineNo>0)
						executor.execute(new ContextGeneratorThread(samples, labels, cfg, indexReader, searcher));
					
					in.close();
					
					// wait for all threads to complete.
					executor.shutdown();
					while(!executor.isTerminated()) {
						
					}
				}
				System.out.println("Done generating contextual information :)");
				
			} catch (IOException e) {
				// TODO Auto-generated catch block
				
				e.printStackTrace();
			}
		}
	}

	static void displayUsage() {
		System.out.println("java -jar wiki_context_generator.jar "
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
				+ "[--categories-num num-categories-to-write ]"
				+ "[--threads-num num-threads-to-run ]"
				+ "[--block-size block-size-per-thread ]"
				+ "[--debug ]");
	}
}
