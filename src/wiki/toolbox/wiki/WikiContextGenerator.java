/**
 * @author wshalaby
 *
 */

package wiki.toolbox.wiki;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashSet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.store.FSDirectory;

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
								.replace("~","")
								.replace("!", "");
						
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
							executor.execute(new ContextGenerator(samples, labels, cfg, indexReader, searcher));
							samples =  new String[cfg.blockSize];
							labels = new String[cfg.blockSize];
							lineNo = 0;
						}
						line = in.readLine();
					}
					if(lineNo>0)
						executor.execute(new ContextGenerator(samples, labels, cfg, indexReader, searcher));
					
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
