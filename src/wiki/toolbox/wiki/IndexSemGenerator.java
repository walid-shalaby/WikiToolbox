package wiki.toolbox.wiki;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.HttpSolrServer;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.SolrInputDocument;
import org.apache.solr.common.params.CommonParams;
import org.apache.solr.common.params.ModifiableSolrParams;
import org.apache.solr.common.util.NamedList;
import org.apache.solr.common.util.SimpleOrderedMap;

import wiki.toolbox.semantic.SemanticSearchConfigParams;
import wiki.toolbox.semantic.SemanticsGenerator;

public class IndexSemGenerator {

	public static void main(String[] args) {
		// TODO Auto-generated method stub
		// retrieve all concepts
		SemanticSearchConfigParams cfg = new SemanticSearchConfigParams();
		cfg.wikiUrl = "http://localhost:"+args[0]+"/solr/collection1/";
		cfg.hidden_max_hits = 50;
		cfg.concepts_num = 50;
		SemanticsGenerator semanticsGenerator = new SemanticsGenerator();
		semanticsGenerator.cacheAssociationsInfo("./wiki_associations.txt");
		semanticsGenerator.cacheConceptsInfo(cfg.wikiUrl, false, false);
		
	    HttpSolrServer server = new HttpSolrServer("http://localhost:9091/solr/collection1");
	    ModifiableSolrParams conceptsQParams = new ModifiableSolrParams();
	    conceptsQParams.set(CommonParams.Q, args[1]);
	    conceptsQParams.set("defType","edismax");
	    conceptsQParams.set("qf", "publication_year");
	    conceptsQParams.set(CommonParams.ROWS, Integer.MAX_VALUE);
	    
	    ArrayList<String> fl = new ArrayList<String>();
	    fl.add("id");
	    fl.add("title");
	    fl.add("abstract");
	    
	    // add target extract field
	    conceptsQParams.set("fl", fl.toArray(new String[fl.size()]));
	    QueryResponse conceptsQResp;
	    try {
	      conceptsQResp = server.query(conceptsQParams);
	    
	      // loop on results and add to concepts
	      SolrDocumentList results = conceptsQResp.getResults();
	      if(results!=null && results.size()>0) {
	        for(int d=0; d<results.size(); d++) {
	          SolrDocument doc = results.get(d);
	          System.out.println((String)doc.getFieldValue("id"));
	          if(doc.getFieldValue("tags")!=null)
	        	  continue;
				
	          String txt = ((String) doc.getFieldValue("title")).replaceAll("\n", " ")+" "+((String) doc.getFieldValue("abstract")).replaceAll("\n", " ");
	          
	          NamedList<Object> semanticConceptsInfo = semanticsGenerator.doSemanticSearch(txt, cfg);
				if(semanticConceptsInfo!=null){
					SolrInputDocument indoc = new SolrInputDocument();
					indoc.addField("id", (String)doc.getFieldValue("id"));
					String[] tags = new String[semanticConceptsInfo.size()];
					String[] tags_docno = new String[semanticConceptsInfo.size()];
					String[] tags_weight = new String[semanticConceptsInfo.size()];
					for(int i=0; i<semanticConceptsInfo.size(); i++) {
						SimpleOrderedMap<Object> obj = (SimpleOrderedMap<Object>)semanticConceptsInfo.getVal(i);
						tags[i] = semanticConceptsInfo.getName(i);
						tags_docno[i] = (String)obj.get("docno");
						tags_weight[i] = String.valueOf(((Float)obj.get("weight")));						
					}
					Map<String,Object> tagsm = new HashMap<>(1);
					Map<String,Object> tagsdocnom = new HashMap<>(1);
					Map<String,Object> tagsweightm = new HashMap<>(1);
					tagsm.put("add", Arrays.asList(tags));
					tagsdocnom.put("add", Arrays.asList(tags_docno));
					tagsweightm.put("add", Arrays.asList(tags_weight));
					indoc.addField("tags", tagsm);
					indoc.addField("tags_docno", tagsdocnom);
					indoc.addField("tags_weight", tagsweightm);
					try {
							server.add(indoc);
							if(d%10000==0)
								server.commit();
						} catch (IOException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}				        
					}			
	          	  
	        }
	        try {
				server.commit();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
	    }
	}catch (SolrServerException e) {
    	e.printStackTrace();      
    }        
	}
}
