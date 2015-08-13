/**
 * 
 */
package wiki.toolbox.wiki;

/**
 * @author wshalaby
 *
 */
public class WikiSearchConfig {	
	// index path
	public String indexPath = "";
	
	// source input (either string or path)
	public String input = "";

	// output path
	public String outPath = "";
	
	// search field
	public String searchField = "";

	// enable title search flag
	public boolean enableTitleSearch = false;
	// title search operator
	public String titleOp = "AND";
	
	// enable enclose query with quotes flag
	public String quotes = "";
	
	// min. doc. length
	public int minDocLen = 0;
	
	// maximum hits
	public int maxHits = Integer.MAX_VALUE;
	
	// docid flag
	public boolean displayDID = false;

	// score flag
	public boolean displayScore = false;
	
	// docid flag
	public boolean displayLen = false;

	// title flag
	public boolean displayTitle = false;
	// remove parentheses flag
	public boolean removeParen = false;
	
	// text flag
	public boolean displayTxt = false;	
	// highlights flag
	public boolean displayHighlights = false;			
	
	// categories flag
	public boolean displayCategories = false;	
	// number of categories
	public int numCategories = 0;
	
	// number of threads
	int numThreads = 1;
	
	// block size per thread
	int blockSize = 10000;
	
	// debug flag
	boolean debug = false;
	
	public WikiSearchConfig() {
		
	}
	
	public boolean parseOpts(String[] opts) {
		boolean result = false;
		for(int i=0; i<opts.length; i++) {
			if(opts[i].compareToIgnoreCase("--index")==0 && ++i<opts.length) {
				indexPath = opts[i]; 
			}
			else if(opts[i].compareToIgnoreCase("--input")==0 && ++i<opts.length) {
				input = opts[i]; 
			}
			else if(opts[i].compareToIgnoreCase("--output")==0 && ++i<opts.length) {
				outPath = opts[i]; 
			}
			else if(opts[i].compareToIgnoreCase("--field")==0 && ++i<opts.length) {
				searchField = opts[i]; 
			}
			else if(opts[i].compareToIgnoreCase("--title-search")==0) {
				enableTitleSearch = true;
			}
			else if(opts[i].compareToIgnoreCase("--title-op")==0 && ++i<opts.length) {
				titleOp = opts[i].toUpperCase();
			}
			else if(opts[i].compareToIgnoreCase("--write-did")==0) {
				displayDID = true;
			}
			else if(opts[i].compareToIgnoreCase("--write-score")==0) {
				displayScore = true;
			}
			else if(opts[i].compareToIgnoreCase("--write-len")==0) {
				displayLen = true;
			}
			else if(opts[i].compareToIgnoreCase("--write-title")==0) {
				displayTitle = true;
			}
			else if(opts[i].compareToIgnoreCase("--remove-paren")==0) {
				removeParen = true;
			}
			else if(opts[i].compareToIgnoreCase("--write-text")==0) {
				displayTxt = true;
			}
			else if(opts[i].compareToIgnoreCase("--write-highlights")==0) {
				displayHighlights = true;
			}
			else if(opts[i].compareToIgnoreCase("--write-categories")==0) {
				displayCategories = true;
			}
			else if(opts[i].compareToIgnoreCase("--categories-num")==0 && ++i<opts.length) {
				numCategories = Integer.parseInt(opts[i]);
			}
			else if(opts[i].compareToIgnoreCase("--threads-num")==0 && ++i<opts.length) {
				numThreads = Integer.parseInt(opts[i]);
			}
			else if(opts[i].compareToIgnoreCase("--block-size")==0 && ++i<opts.length) {
				blockSize = Integer.parseInt(opts[i]);
			}
			else if(opts[i].compareToIgnoreCase("--quote")==0) {
				quotes = "\""; 
			}
			else if(opts[i].compareToIgnoreCase("--min-len")==0 && ++i<opts.length) {
				minDocLen = Integer.parseInt(opts[i]); 
			}
			else if(opts[i].compareToIgnoreCase("--max-hits")==0 && ++i<opts.length) {
				maxHits = Integer.parseInt(opts[i]); 
			}
			else if(opts[i].compareToIgnoreCase("--debug")==0) {
				debug = true;
			}			
		}
		if(searchField.length()>0 && 
				input.length()>0 && 
				outPath.length()>0)
			result = true;
		
		return result;
	}
}
