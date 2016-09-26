package wiki.toolbox.semantic;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import wiki.toolbox.semantic.Enums.ENUM_IPC_FILTER;

public class SemanticSearchConfigParams {
	public Enums.ENUM_SEMANTIC_METHOD e_Method = Enums.ENUM_SEMANTIC_METHOD.e_MSA_SEE_ALSO;
	public Enums.ENUM_DISTANCE_METRIC e_Distance = Enums.ENUM_DISTANCE_METRIC.e_COSINE;
	public Enums.ENUM_ANALYTIC_TYPE e_analytic_type = Enums.ENUM_ANALYTIC_TYPE.e_UNKNOWN;
	public ENUM_IPC_FILTER e_ipc_filter = Enums.ENUM_IPC_FILTER.e_NONE;
	public boolean enable_title_search = false;
	public boolean row_based = false;	
	public boolean write_weights = false;
	public boolean write_content = false;
	public boolean quoted_search = false;
	public boolean enable_search_all = false;
	public boolean enable_show_records = false;
	public boolean enable_search_title = false;
	public boolean enable_search_abstract = false;
	public boolean enable_search_description = false;
	public boolean enable_search_claims = false;
	public boolean enable_extract_all = false;
	public boolean enable_extract_title = false;
	public boolean enable_extract_abstract = false;
	public boolean enable_extract_description = false;
	public boolean enable_extract_claims = false;
	public boolean measure_relatedness = false;
	public boolean hidden_relax_see_also = false;
	public boolean hidden_relax_ner = false;
	public boolean hidden_relax_categories = false;
	public boolean hidden_relax_search = true;
	public boolean hidden_include_q = false;
	public boolean hidden_boolean = false;
	public boolean hidden_relax_cache = true;
	public boolean hidden_relax_disambig = false;
	public boolean hidden_relax_listof = false;
	public boolean hidden_relax_same_title = false;
	public boolean hidden_relax_filters = false;
	public boolean hidden_relatedness_experiment = false;
	public boolean hidden_pagerank_weighting = false;
	public boolean hidden_unquote_concepts = false;
	public boolean abs_explicit = false;
	public int hidden_max_hits = 1000;
	public int hidden_min_wiki_length = 0;
	public int hidden_min_seealso_length = 0;
	public int hidden_min_asso_cnt = 1;
	public int hidden_min_supp = 1;
	public float hidden_min_confidence = 0;
	public int hidden_max_title_ngrams = 3;
	public int hidden_max_seealso_ngrams = 3;
	public int concepts_num = 10;
	public int results_num = 10;
	public int ci_patents_num = 10;
	public int priors_hits = 1000;
	public boolean ci_freetext = false;
	public int hidden_max_levels = 2;
	public String semantics_separator = "\t";
	public String file_separator = ",";
	public boolean write_ids = false;
	public boolean write_cats = false;
	public boolean write_sem_ids = false;
	public String hidden_wiki_search_field = "alltext";
	public String hidden_wiki_extra_query = "AND NOT title:list* AND NOT title:index* AND NOT title:*disambiguation*";
	public String in_path = "";
	public String out_path = "";
	public String[] relevancy_priors = null;
	public HashMap<String,ArrayList<String>> fqs = null;
	public HashSet<String> ignored_concepts = null;
	public String wikiUrl = "http://localhost:5678/solr/collection1/";
	

	// number of threads
	public int numThreads = 1;
	
	// block size per thread
	public int blockSize = 10000;
	
	// debug flag
	public boolean debug = false;
	
	public SemanticSearchConfigParams() {
	
	}
	
	public boolean parseOpts(String[] opts) {
		boolean result = false;
		for(int i=0; i<opts.length; i++) {
			if(opts[i].compareToIgnoreCase("--url")==0 && ++i<opts.length) {
				wikiUrl = opts[i]; 
			}
			else if(opts[i].compareToIgnoreCase("--input")==0 && ++i<opts.length) {
				in_path = opts[i]; 
			}
			else if(opts[i].compareToIgnoreCase("--output")==0 && ++i<opts.length) {
				out_path = opts[i]; 
			}
			else if(opts[i].compareToIgnoreCase("--field")==0 && ++i<opts.length) {
				hidden_wiki_search_field = opts[i]; 
			}
			else if(opts[i].compareToIgnoreCase("--threads-num")==0 && ++i<opts.length) {
				numThreads = Integer.parseInt(opts[i]);
			}
			else if(opts[i].compareToIgnoreCase("--block-size")==0 && ++i<opts.length) {
				blockSize = Integer.parseInt(opts[i]);
			}
			else if(opts[i].compareToIgnoreCase("--method")==0 && ++i<opts.length) {
				e_Method = Enums.getSemanticMethod(opts[i]);
			}
			else if(opts[i].compareToIgnoreCase("--distance")==0 && ++i<opts.length) {
				e_Distance = Enums.getDistanceMethod(opts[i]);
			}
			else if(opts[i].compareToIgnoreCase("--concepts-num")==0 && ++i<opts.length) {
				concepts_num = Integer.parseInt(opts[i]);
			}
			else if(opts[i].compareToIgnoreCase("--relax-same-title")==0 && ++i<opts.length) {
				if(opts[i].compareToIgnoreCase("on")==0)
					hidden_relax_same_title = true;
				else
					hidden_relax_same_title = false;
			}
			else if(opts[i].compareToIgnoreCase("--abs-explicit")==0 && ++i<opts.length) {
				if(opts[i].compareToIgnoreCase("on")==0)
					abs_explicit = true;
				else
					abs_explicit = false;
			}
			else if(opts[i].compareToIgnoreCase("--max-title-ngrams")==0 && ++i<opts.length) {
				hidden_max_title_ngrams = Integer.parseInt(opts[i]);
			}
			else if(opts[i].compareToIgnoreCase("--max-seealso-ngrams")==0 && ++i<opts.length) {
				hidden_max_seealso_ngrams = Integer.parseInt(opts[i]);
			}
			else if(opts[i].compareToIgnoreCase("--field")==0 && ++i<opts.length) {
				hidden_wiki_search_field = opts[i];
			}
			else if(opts[i].compareToIgnoreCase("--wikiurl")==0 && ++i<opts.length) {
				wikiUrl = opts[i];
			}
			else if(opts[i].compareToIgnoreCase("--min-len")==0 && ++i<opts.length) {
				hidden_min_wiki_length = Integer.parseInt(opts[i]);
			}
			else if(opts[i].compareToIgnoreCase("--min-seealso-len")==0 && ++i<opts.length) {
				hidden_min_seealso_length = Integer.parseInt(opts[i]);
			}
			else if(opts[i].compareToIgnoreCase("--max-hits")==0 && ++i<opts.length) {
				hidden_max_hits = Integer.parseInt(opts[i]);
			}
			else if(opts[i].compareToIgnoreCase("--min-asso-cnt")==0 && ++i<opts.length) {
				hidden_min_asso_cnt = Integer.parseInt(opts[i]);
			}
			else if(opts[i].compareToIgnoreCase("--min-supp")==0 && ++i<opts.length) {
				hidden_min_supp = Integer.parseInt(opts[i]);
			}
			else if(opts[i].compareToIgnoreCase("--min-confidence")==0 && ++i<opts.length) {
				hidden_min_confidence = Float.parseFloat(opts[i]);
			}			
			else if(opts[i].compareToIgnoreCase("--extra-q")==0 && ++i<opts.length) {
				hidden_wiki_extra_query = opts[i];
			}
			else if(opts[i].compareToIgnoreCase("--semantics-separator")==0 && ++i<opts.length) {
				if(opts[i].compareToIgnoreCase("tab")==0)
					semantics_separator = "\t";
				else if(opts[i].compareToIgnoreCase("newline")==0)
					semantics_separator = "\n";
				else
					semantics_separator = opts[i];
			}
			else if(opts[i].compareToIgnoreCase("--file-separator")==0 && ++i<opts.length) {
				if(opts[i].compareToIgnoreCase("tab")==0)
					file_separator = "\t";
				else if(opts[i].compareToIgnoreCase("newline")==0)
					file_separator = "\n";
				else
					file_separator = opts[i];
			}
			else if(opts[i].compareToIgnoreCase("--write-sem-ids")==0 && ++i<opts.length) {
				if(opts[i].compareToIgnoreCase("on")==0)
					write_sem_ids = true;
				else
					write_sem_ids = false;
			}
			else if(opts[i].compareToIgnoreCase("--write-ids")==0 && ++i<opts.length) {
				if(opts[i].compareToIgnoreCase("on")==0)
					write_ids = true;
				else
					write_ids = false;
			}
			else if(opts[i].compareToIgnoreCase("--write-categories")==0 && ++i<opts.length) {
				if(opts[i].compareToIgnoreCase("on")==0)
					write_cats = true;
				else
					write_cats = false;
			}
			else if(opts[i].compareToIgnoreCase("--title-search")==0 && ++i<opts.length) {
				if(opts[i].compareToIgnoreCase("on")==0)
					enable_title_search = true;
				else
					enable_title_search = false;
			}
			else if(opts[i].compareToIgnoreCase("--rows-based")==0 && ++i<opts.length) {
				if(opts[i].compareToIgnoreCase("on")==0)
					row_based = true;
				else
					row_based = false;
			}
			else if(opts[i].compareToIgnoreCase("--write-weights")==0 && ++i<opts.length) {
				if(opts[i].compareToIgnoreCase("on")==0)
					write_weights = true;
				else
					write_weights = false;
			}
			else if(opts[i].compareToIgnoreCase("--write-content")==0 && ++i<opts.length) {
				if(opts[i].compareToIgnoreCase("on")==0)
					write_content = true;
				else
					write_content = false;
			}
			else if(opts[i].compareToIgnoreCase("--relax-cache")==0 && ++i<opts.length) {
				if(opts[i].compareToIgnoreCase("on")==0)
					hidden_relax_cache = true;
				else
					hidden_relax_cache = false;
			}
			else if(opts[i].compareToIgnoreCase("--relatedness-expr")==0 && ++i<opts.length) {
				if(opts[i].compareToIgnoreCase("on")==0)
					hidden_relatedness_experiment = true;
				else
					hidden_relatedness_experiment = false;
			}
			else if(opts[i].compareToIgnoreCase("--unquote-concepts")==0 && ++i<opts.length) {
				if(opts[i].compareToIgnoreCase("on")==0)
					hidden_unquote_concepts = true;
				else
					hidden_unquote_concepts = false;
			}			
			else if(opts[i].compareToIgnoreCase("--pagerank")==0 && ++i<opts.length) {
				if(opts[i].compareToIgnoreCase("on")==0)
					hidden_pagerank_weighting = true;
				else
					hidden_pagerank_weighting = false;
			}
			else if(opts[i].compareToIgnoreCase("--debug")==0 && ++i<opts.length) {
				if(opts[i].compareToIgnoreCase("on")==0)
					debug = true;
				else
					debug = false;
			}
			else if(opts[i].compareToIgnoreCase("--relax-seealso")==0 && ++i<opts.length) {
				if(opts[i].compareToIgnoreCase("on")==0)
					hidden_relax_see_also = true;
				else
					hidden_relax_see_also = false;
			}
			else if(opts[i].compareToIgnoreCase("--relax-listof")==0 && ++i<opts.length) {
				if(opts[i].compareToIgnoreCase("on")==0)
					hidden_relax_listof = true;
				else
					hidden_relax_listof = false;
			}
			else if(opts[i].compareToIgnoreCase("--relax-disambig")==0 && ++i<opts.length) {
				if(opts[i].compareToIgnoreCase("on")==0)
					hidden_relax_disambig = true;
				else
					hidden_relax_disambig = false;
			}
			else if(opts[i].compareToIgnoreCase("--relax-ner")==0 && ++i<opts.length) {
				if(opts[i].compareToIgnoreCase("on")==0)
					hidden_relax_ner = true;
				else
					hidden_relax_ner = false;
			}
			else if(opts[i].compareToIgnoreCase("--relax-categories")==0 && ++i<opts.length) {
				if(opts[i].compareToIgnoreCase("on")==0)
					hidden_relax_categories = true;
				else
					hidden_relax_categories = false;
			}
			else if(opts[i].compareToIgnoreCase("--quoted-search")==0 && ++i<opts.length) {
				if(opts[i].compareToIgnoreCase("on")==0)
					quoted_search = true;
				else
					quoted_search = false;
			}/*
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
			else if(opts[i].compareToIgnoreCase("--quote")==0) {
				quotes = "\""; 
			}
			else if(opts[i].compareToIgnoreCase("--min-len")==0 && ++i<opts.length) {
				minDocLen = Integer.parseInt(opts[i]); 
			}
			else if(opts[i].compareToIgnoreCase("--max-hits")==0 && ++i<opts.length) {
				maxHits = Integer.parseInt(opts[i]); 
			}*/			
		}
		if(in_path.length()>0 && 
			out_path.length()>0 && 
			e_Method!=Enums.ENUM_SEMANTIC_METHOD.e_UNKNOWN)
			result = true;
		
		return result;
	}
}
