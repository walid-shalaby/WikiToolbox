/**
 * @author wshalaby
 *
 */

package wiki.toolbox.commons;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
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

public class LuceneSearch {

	protected String indexPath = "";
	protected Analyzer analyzer = null;
	protected IndexReader indexReader = null;
	protected IndexSearcher searcher = null;
	
	/**
	 * @param indexpath: full path of patents index
	 * @param queryAnalyzer: query text analyzer (if null, standard analyzer is used)
	 */
	public LuceneSearch(String indexpath, Analyzer queryAnalyzer) {
		indexPath = indexpath;
		if(queryAnalyzer!=null) {
			analyzer = queryAnalyzer;
		}
		else
			analyzer = new StandardAnalyzer(Version.LUCENE_46);
	}	
	public void Initialize() throws IOException {
		indexReader = DirectoryReader.open(FSDirectory.open(new File(indexPath)));
		searcher = new IndexSearcher(indexReader);
	}
	
	/**
	 * @param query
	 * @return
	 */
	public ScoreDoc[] Search(String queryText, int resultsnum) {
		ScoreDoc[] hits = null;		
		try {
			// one time initialization
			if(indexReader==null || searcher==null)
				Initialize();
			
			QueryParser parser = new QueryParser(Version.LUCENE_46, "texty", analyzer);
			Query query = null;
			parser.setAllowLeadingWildcard(true);				
			query = parser.parse(queryText);
			
			System.out.println("Searching (" + query + ").....");
			TopDocs topDocs = searcher.search(query, resultsnum);
			if(topDocs.totalHits > 0) {
				System.out.println("Results :)");
				hits = topDocs.scoreDocs;
			}
		} catch (IOException | ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return hits;
	}
	public Document getDoc(int docid) throws IOException {
		return indexReader.document(docid);		
	}
	public IndexableField getField(int docid, String fieldName) throws IOException {
		return getDoc(docid).getField(fieldName);		
	}
	public List<IndexableField> getFields(int docid) throws IOException {
		return getDoc(docid).getFields();		
	}
}
