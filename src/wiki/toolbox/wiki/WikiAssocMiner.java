/**
 * @author wshalaby
 *
 */

package wiki.toolbox.wiki;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexableField;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.HttpSolrServer;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.params.CommonParams;
import org.apache.solr.common.params.ModifiableSolrParams;

//class WikiTopic {
//	public int docno;
//	public int count;
//	
//	public WikiTopic(int d, int c) {
//		docno = d;
//		count = c;
//	}
//}

public class WikiAssocMiner {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		
		Scanner reader = new Scanner(System.in);
		
		// get index path
		System.out.print("Enter url: ");		
		String wikiUrl = reader.nextLine();
		
		// get output path
		System.out.print("Enter keeponly path (press enter to skip): ");		
		String keeppath = reader.nextLine();	
		
		// get output path
		System.out.print("Enter output path: ");		
		String outpath = reader.nextLine();
		

		HttpSolrServer server = new HttpSolrServer(wikiUrl);
	    ModifiableSolrParams conceptsQParams = new ModifiableSolrParams();
	    conceptsQParams.set(CommonParams.Q, "*");
	    conceptsQParams.set("defType","edismax");
	    conceptsQParams.set("qf", "title");
	    conceptsQParams.set(CommonParams.ROWS, Integer.MAX_VALUE);
	    
	    // add target extract field
	    conceptsQParams.set("fl", new String[]{"title","redirect","seealso"});
	    QueryResponse conceptsQResp;
	    try {
	      conceptsQResp = server.query(conceptsQParams);
	    
	      // loop on results and add to concepts
	      SolrDocumentList results = conceptsQResp.getResults();
	      if(results!=null && results.size()>0) {
	    	// load keep only
	    	HashSet<String> toKeep = new HashSet<String>();
	    	if(keeppath.length()>0) {
	    		BufferedReader br = new BufferedReader(new FileReader(keeppath));
	    		String line = br.readLine();
	    		while(line!=null) {
	    			toKeep.add(line);
	    			line = br.readLine();
	    		}
	    		br.close();
	    	}
	    	HashMap<String, HashMap<String, Integer>> wikiAssociations = 
						new HashMap<String, HashMap<String, Integer> >();
				
			HashMap<String, Integer> wikiTopics = new HashMap<String, Integer>();
			HashMap<String, String> wikiRedirects = new HashMap<String, String>();
			HashMap<String, Integer> wikiTopicsCounts = new HashMap<String, Integer>();
				
			System.out.println("Results ("+results.size()+") :)");
				
			// write all see_also transactions
			FileWriter seeWriter = new FileWriter(new File(outpath+"/wiki_seealso.arff"));
			seeWriter.write("@relation wiki_seealso.symbolic\n\n");		
				
	        for(int i=0; i<results.size(); i++) {
	        	SolrDocument doc = results.get(i);
	        	
				// get title
				String title = (String)doc.getFieldValue("title");					
				
				if(toKeep.size()==0 || toKeep.contains(title)) {
					seeWriter.write("@attribute \""+title.replace("\\", "\\\\")+"\" {f,t}\n");
					
					wikiTopics.put(title, i);
					
					// add redirects
			        Collection<Object> redirectValues = doc.getFieldValues("redirect");
			        if(redirectValues!=null) {
			          Object[] redirects = redirectValues.toArray();
			          for(int t=0; t<redirects.length; t++) {
			            wikiRedirects.put((String) redirects[t], title);
			          }
			        }
				}
	        }
	        seeWriter.write("\n\n@data\n");
	        
	        for(int i=0; i<results.size(); i++) {
	        	ArrayList<String> titles = new ArrayList<String>(300); 
	        	SolrDocument doc = results.get(i);
	        	
				// get title
	        	String title = (String)doc.getFieldValue("title");
				titles.add(title);
				
				Integer idx = wikiTopics.get(title);
				if(idx==null) {
					System.out.println(title+" -- invalid title");
					continue;
				}
				
				// get see also
				int j = 0;
				Collection<Object> seeAlsoValues = doc.getFieldValues("seealso");
				if(seeAlsoValues!=null && seeAlsoValues.size()>0) {
					// get see also
					ArrayList<Integer> idxs = new ArrayList<Integer>(seeAlsoValues.size());
					Object[] multiSeeAlso = seeAlsoValues.toArray();
					for(j=0; j<multiSeeAlso.length; j++) {
						Integer sidx = wikiTopics.get((String)multiSeeAlso[j]);
						if(sidx!=null) {
							idxs.add(sidx);
							titles.add((String)multiSeeAlso[j]);
						}
						else {
							String orgtitle = wikiRedirects.get((String)multiSeeAlso[j]);
							if(orgtitle!=null) {
								idxs.add(wikiTopics.get(orgtitle));
								titles.add(orgtitle);
							}
							else
								System.out.println((String)multiSeeAlso[j]+" -- invalid see_also of ("+(String)multiSeeAlso[j]+") with title("+title+")");
						}
					}
					if(idxs.size()>0) {
						// add original title
						idxs.add(idx);
						
						// sort out indices
						Integer[] sidxs = new Integer[idxs.size()];
						sidxs = idxs.toArray(sidxs);
						Arrays.sort(sidxs);
						
						seeWriter.write("{"+sidxs[0].toString()+" t");
						for(j=1; j<sidxs.length; j++)
							if(sidxs[j-1]!=sidxs[j]) // there are some duplicates!
								seeWriter.write(","+sidxs[j].toString()+" t");
						
						seeWriter.write("}\n");
					}
				}
				updateCounts(titles, wikiAssociations, wikiTopicsCounts);
	        }
	        seeWriter.close();
	        
	        // write all associations
			FileWriter assocWriter = new FileWriter(new File(outpath+"/wiki_associations.txt"));
			
			for(String title : wikiTopicsCounts.keySet()) {
				assocWriter.write(title+"/\\/\\"+wikiTopicsCounts.get(title));
				Iterator<Entry<String, Integer>> iter = wikiAssociations.get(title).entrySet().iterator();
				while(iter.hasNext()) {
					Entry<String,Integer> e = iter.next();
					assocWriter.write("#$#"+e.getKey()+"/\\/\\"+e.getValue());
				}
				assocWriter.write("\n");
			}
			seeWriter.close();
			assocWriter.close();
	      }
	      else 
			System.out.println("No results found :(");
				
			System.out.println("\nDone...\n");
	    } catch (SolrServerException e) {
	    	e.printStackTrace();
	    } catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	

	private static void updateCounts(ArrayList<String> titles, 
			HashMap<String, HashMap<String, Integer>> wikiAssociations, 
			HashMap<String, Integer> wikiTopicsCounts) {

		for(int i=0; i<titles.size(); i++) {
			String title = titles.get(i);
			
			HashMap<String,Integer> titleAssociations = null;
			
			Integer c = wikiTopicsCounts.get(title);
			if(c==null) { // new title, add it						
				// create new associations
				wikiTopicsCounts.put(title, 1);
				titleAssociations = new HashMap<String,Integer>();
				wikiAssociations.put(title, titleAssociations);
			}
			else { // increment its count
				wikiTopicsCounts.put(title, c+1);
				titleAssociations = wikiAssociations.get(title);			
			}
			
			for(int j=0; j<titles.size(); j++) {
				if(i==j)
					continue;
				
				// check in current title associations
				c = titleAssociations.get(titles.get(j));
				if(c==null) { // new association, add it 
					// create new association
					titleAssociations.put(titles.get(j), 1);						
				}
				else { // increment its count
					titleAssociations.put(titles.get(j), c+1);						
				}
			}
		}
	}
}
