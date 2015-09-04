/**
 * 
 */
package wiki.toolbox.wiki;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Scanner;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.IndexableField;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;

/**
 * @author wshalaby
 *
 */

public class WikiIndexUpdater {

	/**
	 * @param args
	 * @throws FileNotFoundException 
	 */
	public static void main(String[] args) throws FileNotFoundException {
		// TODO Auto-generated method stub
		Scanner reader = new Scanner(System.in);
		
		// get source index path
		System.out.print("Enter source index path: ");		
		String sourcePath = reader.nextLine();
		
		// create indexer writer & configuration
		try {
			Directory dir = FSDirectory.open(new File(sourcePath));
			Analyzer stdAnalyzer = new StandardAnalyzer();
			IndexWriterConfig cfg = new IndexWriterConfig(Version.LATEST, stdAnalyzer);
			cfg.setOpenMode(IndexWriterConfig.OpenMode.APPEND);
			IndexReader indexReader = DirectoryReader.open(dir);
			IndexWriter writer = new IndexWriter(dir, cfg);
			IndexSearcher searcher = new IndexSearcher(indexReader);
			QueryParser parser = new QueryParser("text", stdAnalyzer); //
			parser.setAllowLeadingWildcard(true);				
			Query query = parser.parse("*");
			
			System.out.println("Searching (" + query + ").....");
			TopDocs topDocs = searcher.search(query, Integer.MAX_VALUE);
			if(topDocs.totalHits > 0) {
				ScoreDoc[] hits = topDocs.scoreDocs;
				System.out.println("Results ("+hits.length+") :)");
				
				for(int i = 0 ; i < hits.length; i++) {
					Document doc = indexReader.document(hits[i].doc);
					
					// get docno
					String docno = doc.getField("docno").stringValue();
					
					// get title					
					String title = doc.getField("title").stringValue();
					int len = WikiIndexer.getTitleLength(title);
					doc.add(new Field("title_length", String.format("%03d", len), Field.Store.YES, Field.Index.ANALYZED_NO_NORMS));
					
					// get anchors
					IndexableField anchors[] = doc.getFields("title_anchors");
					for(int f=0; f<anchors.length; f++) {
						len = WikiIndexer.getTitleLength(anchors[f].stringValue());
						doc.add(new Field("anchor_length", String.format("%03d", len), Field.Store.YES, Field.Index.ANALYZED_NO_NORMS));							
					}
					
					// get see also
					IndexableField seealso[] = doc.getFields("see_also");
					for(int s=0; s<seealso.length; s++) {
						len = WikiIndexer.getTitleLength(seealso[s].stringValue());
						doc.add(new Field("seealso_length", String.format("%03d", len), Field.Store.YES, Field.Index.ANALYZED_NO_NORMS));							
					}
					writer.updateDocument(new Term("docno",docno), doc);
				}
					
				System.out.println("\n\nDone...");
			}
			writer.close();
			reader.close();
		} catch (IOException | ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
