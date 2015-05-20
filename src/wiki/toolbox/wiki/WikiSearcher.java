/**
 * @author wshalaby
 *
 */

package wiki.toolbox.wiki;

import java.io.File;
import java.io.IOException;
import java.util.Scanner;

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

public class WikiSearcher {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		int maxhits;
		
		Scanner reader = new Scanner(System.in);
		
		// get source path
		System.out.print("Enter search query: ");		
		String queryTxt = reader.nextLine();
		
		// get index path
		System.out.print("Enter index path: ");		
		String indexPath = reader.nextLine();
		
		// get index path
		System.out.print("Enter maximum hits: ");		
		maxhits = Integer.parseInt(reader.nextLine());
		
		try {
			// open the index
			IndexReader indexReader = DirectoryReader.open(FSDirectory.open(new File(indexPath)));
			IndexSearcher searcher = new IndexSearcher(indexReader);
			Analyzer stdAnalyzer = new StandardAnalyzer(Version.LUCENE_46);
			QueryParser parser = new QueryParser(Version.LUCENE_46, "text", stdAnalyzer); //
			Query query = null;
			try {
				
				parser.setAllowLeadingWildcard(true);				
				query = parser.parse(queryTxt);
			} catch (ParseException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} //
			//QueryBuilder qbuilder = new QueryBuilder(stdAnalyzer);			
			//Query query = qbuilder.createPhraseQuery("text", queryTxt);
			System.out.println("Searching (" + query + ").....");
			TopDocs topDocs = searcher.search(query, maxhits!=0?maxhits:Integer.MAX_VALUE);
			if(topDocs.totalHits > 0) {
				ScoreDoc[] hits = topDocs.scoreDocs;
				System.out.println("Results ("+hits.length+") :)");
				for(int i = 0 ; i < hits.length; i++) {
					System.out.println("doc(" + hits[i].doc + "), score=" + hits[i].score + ", length=" + indexReader.document(hits[i].doc).getField("text").stringValue().length() + " --> " + indexReader.document(hits[i].doc).getField("title").stringValue() + " :::" + indexReader.document(hits[i].doc).getField("text").stringValue());
					//IndexableField arr[] = indexReader.document(hits[i].doc).getFields("title");
					//for (IndexableField f : arr) 
					//	System.out.println(f.stringValue());
				
				}
			}
			else 
				System.out.println("No results found :(");
				
			reader.close();
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			
			e.printStackTrace();
		} 
				
		

	}

}
