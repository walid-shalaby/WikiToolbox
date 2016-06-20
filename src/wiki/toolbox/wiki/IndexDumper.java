package wiki.toolbox.wiki;

import java.io.IOException;
import java.util.ArrayList;

import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.HttpSolrServer;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.params.CommonParams;
import org.apache.solr.common.params.ModifiableSolrParams;

public class IndexDumper {

	public static void main(String[] args) {
		// TODO Auto-generated method stub
		// retrieve all concepts
	    HttpSolrServer server = new HttpSolrServer("http://localhost:9091/solr/collection1");
	    ModifiableSolrParams conceptsQParams = new ModifiableSolrParams();
	    conceptsQParams.set(CommonParams.Q, "*");
	    conceptsQParams.set("defType","edismax");
	    conceptsQParams.set("qf", "title");
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
	        for(int i=0; i<results.size(); i++) {
	          SolrDocument doc = results.get(i);
	          
	          System.out.println("label\t"+(String) doc.getFieldValue("id")+"\t"+
	          ((String) doc.getFieldValue("title")).replaceAll("\n", " ")+" "+((String) doc.getFieldValue("abstract")).replaceAll("\n", " "));	  
	        }
	    }
	}catch (SolrServerException | IOException e) {
    	e.printStackTrace();      
    }        
	}
}
