/**
 * 
 */
package wiki.toolbox.wiki;

import java.io.File;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.index.IndexableField;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.search.highlight.Highlighter;
import org.apache.lucene.search.highlight.InvalidTokenOffsetsException;
import org.apache.lucene.search.highlight.QueryScorer;
import org.apache.lucene.search.highlight.SimpleHTMLFormatter;
import org.apache.lucene.search.highlight.TextFragment;
import org.apache.lucene.search.highlight.TokenSources;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.HttpSolrServer;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.params.CommonParams;
import org.apache.solr.common.params.ModifiableSolrParams;


/**
 * @author wshalaby
 *
 */
public class ContextGenerator implements Runnable {
	WikiSearchConfig cfg;
	Analyzer stdAnalyzer;
	QueryParser parser;
	IndexReader indexReader;
	IndexSearcher searcher;
	String[] samples;
	String[] labels;
	public ContextGenerator(WikiSearchConfig c) {
		// TODO Auto-generated constructor stub
		cfg = c;
	}
	
	public ContextGenerator(String[] s, String[] l, 
			WikiSearchConfig c) {
		// TODO Auto-generated constructor stub
		cfg = c;
		samples = s;
		labels = l;
	}
	public ContextGenerator(String[] s, String[] l, 
			WikiSearchConfig c, IndexReader r, IndexSearcher sh) {
		// TODO Auto-generated constructor stub
		cfg = c;
		samples = s;
		labels = l;
		stdAnalyzer = new StandardAnalyzer();
		parser = new QueryParser("text", stdAnalyzer); //
		indexReader = r;
		searcher = sh;
	}
	
	public String getContext(String sample) throws IOException {
		if(cfg.onlinemode==true) {
			return getContextOnline(sample);
		}
		else
			return getContextBatch(sample);
	}
	
	private String getContextBatch(String sample) {
		// TODO Auto-generated method stub
		String result = "";
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
						result += String.format("\t%d", hits[i].doc);
					}
					if(cfg.displayScore) {
						result += String.format("\t%f", hits[i].score);
					}
					if(cfg.displayLen) {
						result += "\t"+indexReader.document(hits[i].doc).getField("length").stringValue();
					}
					if(cfg.displayTitle) {
						data = indexReader.document(hits[i].doc).getField("title").stringValue();
						if(cfg.removeParen && (indx=data.indexOf(" ("))!=-1)
							data = indexReader.document(hits[i].doc).getField("title").stringValue().substring(0,indx);
						result += "\t"+data;
					}
					if(cfg.displayTxt || cfg.displayHighlights) {
						String text = indexReader.document(hits[i].doc).getField("text").stringValue();
						if(cfg.displayTxt)
							result += "\t"+text;
						if(cfg.displayHighlights) {
							TokenStream tokenStream = TokenSources.getAnyTokenStream(searcher.getIndexReader(), 
									hits[i].doc, "text", stdAnalyzer);
							TextFragment[] frag;
							try {
								frag = highlighter.getBestTextFragments(tokenStream, text, false, 10);
								for (int j = 0; j < frag.length; j++) {
									if ((frag[j] != null) && (frag[j].getScore() > 0)) {
										result += "\t"+(frag[j].toString());										
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
							result += "\t"+categories[j].stringValue();
						}
					}
					
					result += System.lineSeparator()+System.lineSeparator()+System.lineSeparator();
				}
			}
			else if(cfg.debug==true) 
				System.out.println("No results found :(");
		} catch (ParseException | IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} //
		
		return result;
	}
	private String getContextOnline(String sample) {
		// TODO Auto-generated method stub
		String result = "";
		try {
			String highlightQueryString = cfg.searchField+":"+cfg.quotes+sample+cfg.quotes;
		    String filterQueryString = "length:["+String.format("%d", cfg.minDocLen)+" TO *]";
		    String queryString;
	        if(cfg.enableTitleSearch) {
	        	queryString = "title:"+cfg.quotes+sample+cfg.quotes+" OR "+cfg.searchField+":"+cfg.quotes+sample+cfg.quotes;
	        }
	        else {
	        	queryString = cfg.searchField+":"+cfg.quotes+sample+cfg.quotes;
	        }
		
	        HttpSolrServer server = new HttpSolrServer(cfg.indexPath);
	        ModifiableSolrParams qParams = new ModifiableSolrParams();
	        qParams.set(CommonParams.Q, queryString);
	        //qParams.set("defType","edismax");
	        qParams.set(CommonParams.FQ,filterQueryString);
	        qParams.set(CommonParams.ROWS, cfg.maxHits!=0?cfg.maxHits:Integer.MAX_VALUE);
	        qParams.set("hl", true);
	        //qParams.set("hl.field", cfg.searchField);
	        qParams.set("hl.fl", cfg.searchField);
	        //qParams.set("hl.q", queryString);
	        qParams.set("hl.simple.pre", "<B>");
	        qParams.set("hl.simple.post", "</B>");
	        qParams.set("hl.snippets", 10);
	        qParams.set("fl", new String[]{"score","id","title","text","category","length"});
	        
			if(cfg.debug==true)
				System.out.println("Searching (" + queryString + ").....");
			
			QueryResponse qResp = server.query(qParams);
		    
		      // loop on results and add to concepts
		      SolrDocumentList results = qResp.getResults();
		      if(results!=null && results.size()>0) {
		    	  String data = "";
		    	  int indx;
					
		    	  if(cfg.debug==true)
						System.out.println("Results ("+results.size()+") :)");
		    	  for(int i = 0 ; i < results.size(); i++) {
					SolrDocument hit = results.get(i);
					
					if(cfg.displayDID) {
						result += (String)hit.getFieldValue("id");
					}
					if(cfg.displayScore) {
						result += String.valueOf(((Float)hit.getFieldValue("score")).floatValue());
					}
					if(cfg.displayLen) {
						result += "\t"+String.valueOf(((Integer)hit.getFieldValue("length")).intValue());
					}
					if(cfg.displayTitle) {
						data = ((ArrayList<String>)hit.getFieldValue("title")).get(0);
						if(cfg.removeParen && (indx=data.indexOf(" ("))!=-1)
							data = ((String)hit.getFieldValue("title")).substring(0,indx);
						result += "\t"+data;
					}
					if(cfg.displayTxt) {
						String text = ((ArrayList<String>)hit.getFieldValue("text")).get(0);
						if(cfg.displayTxt)
							result += "\t"+text;
					}
					if(cfg.displayCategories) {
						Collection<Object> categories = hit.getFieldValues("category");
						if(categories!=null) {
							Object[] categoriesArr = categories.toArray();
							for(int j=0; j<categoriesArr.length && (cfg.numCategories==0 || j<cfg.numCategories); j++){ 
								result += "\t"+categoriesArr[j];
							}
						}
					}
					
					result += System.lineSeparator()+System.lineSeparator()+System.lineSeparator();
		    	  }
		    	  if(cfg.displayHighlights) {
						Map<String,Map<String,List<String>>> highlights = qResp.getHighlighting();
						if(highlights!=null && highlights.size()>0) {
							for(Map<String,List<String>> highhlight : highlights.values()) {
								for(List<String> snippets : highhlight.values()) {
									for(String snippet : snippets) {
										result += "\t"+snippet;
									}
								}
								result += System.lineSeparator()+System.lineSeparator()+System.lineSeparator();
							}
						}
					}
		    	  server.close();
			}
			else if(cfg.debug==true) 
				System.out.println("No results found :(");
		} catch (SolrServerException | IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} //
		return result;
	}
	@Override
	public void run() {
		// open output file
		for(int s=0; s<samples.length; s++)
		{
			String sample = samples[s];
			String label = labels[s];
			try {
				File f = new File(cfg.outPath+"/"+label+"/"+sample.toLowerCase());
				FileWriter out = new FileWriter(f);
				if(f.exists()) {
					out.write(sample+"\n");
					out.write(getContext(sample));
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

