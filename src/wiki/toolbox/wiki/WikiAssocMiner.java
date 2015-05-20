/**
 * @author wshalaby
 *
 */

package wiki.toolbox.wiki;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Scanner;
import java.util.concurrent.ArrayBlockingQueue;

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
import org.apache.lucene.util.Version;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;

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
		System.out.print("Enter index path: ");		
		String indexPath = reader.nextLine();
		
		// get output path
		System.out.print("Enter output path: ");		
		String outpath = reader.nextLine();
		
		try {
			// open the index
			IndexReader indexReader = DirectoryReader.open(FSDirectory.open(new File(indexPath)));
			IndexSearcher searcher = new IndexSearcher(indexReader);
			Analyzer stdAnalyzer = new StandardAnalyzer(Version.LUCENE_46);
			QueryParser parser = new QueryParser(Version.LUCENE_46, "text", stdAnalyzer); //
			Query query = null;
			try {
				
				parser.setAllowLeadingWildcard(true);				
				query = parser.parse("*");
			} catch (ParseException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			System.out.println("Searching (" + query + ").....");
			TopDocs topDocs = searcher.search(query, Integer.MAX_VALUE);
			if(topDocs.totalHits > 0) {
				HashMap<String, HashMap<String, Integer>> wikiAssociations = 
						new HashMap<String, HashMap<String, Integer> >();
				
				HashMap<String, Integer> wikiTopics = new HashMap<String, Integer>();
				HashMap<String, Integer> wikiTopicsCounts = new HashMap<String, Integer>();
				
				ScoreDoc[] hits = topDocs.scoreDocs;
				System.out.println("Results ("+hits.length+") :)");
				
				// write all see_also transactions
				FileWriter seeWriter = new FileWriter(new File(outpath+"wiki_seealso.arff"));
				seeWriter.write("@relation wiki_seealso.symbolic\n\n");		
				for(int i = 0 ; i < hits.length; i++) {
					// get docno
					int docno = Integer.parseInt(indexReader.document(hits[i].doc).getField("docno").stringValue());
					
					// get title
					IndexableField arr[] = indexReader.document(hits[i].doc).getFields("title");					
					
					seeWriter.write("@attribute "+arr[0].stringValue().replace(' ', '_')+"{"+docno+"}\n");
					
					wikiTopics.put(arr[0].stringValue(), new Integer(docno));
				}
				
				seeWriter.write("\n\n@data\n");
				
				for(int i = 0 ; i < hits.length; i++) {
					
					ArrayList<String> titles = new ArrayList<String>(300); 
					
					// get title
					IndexableField arr[] = indexReader.document(hits[i].doc).getFields("title");					
					String title = arr[0].stringValue();
					titles.add(title);
					
					Integer docno = wikiTopics.get(title);
					if(docno!=null)
						seeWriter.write(docno.toString());
					else 
						System.out.println(title+" -- invalid");
					
					// get see also
					int j = 0;
					arr = indexReader.document(hits[i].doc).getFields("see_also");
					for(j=0; j<arr.length; j++) {
						titles.add(arr[j].stringValue());
						
						docno = wikiTopics.get(arr[j].stringValue());
						if(docno!=null)
							seeWriter.write(","+docno.toString());
						else 
							System.out.println(arr[j].stringValue()+" -- invalid");
					}
					seeWriter.write("\n");
					
					updateCounts(titles, wikiAssociations, wikiTopicsCounts);
				}
				
				seeWriter.close();
				
				// write all associations
				FileWriter assocWriter = new FileWriter(new File(outpath+"wiki_associations.txt"));
				
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
				
			reader.close();
			
			System.out.println("\nDone...\n");
			
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
