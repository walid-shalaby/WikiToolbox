package wiki.toolbox.wiki;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
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

public class IndexUpdater {

	public static void main(String[] args) {
		// TODO Auto-generated method stub
		// retrieve all concepts
		System.out.println("Usage: jar indexupdater.jar update index-url source-field destination-field update-file-path auto-warm(on/off)");
		System.out.println("e.g., jar indexupdater.jar update http://localhost:5678/solr/collection1/ title pagerank /path/to/update/file/source-then-tab-then-value on");
		System.out.println("Usage: jar indexupdater.jar keep index-url source-field to-keep-file-path auto-warm(on/off)");
		System.out.println("e.g., jar indexupdater.jar keep http://localhost:5678/solr/collection1/ title /path/to/to/keep/file/source-value on");
		
		if(args[0].equalsIgnoreCase("update")) {
			update(args);
		}
		else if(args[0].equalsIgnoreCase("keep")) {
			delete(args, false);
		}
		else if(args[0].equalsIgnoreCase("delete")) {
			delete(args, true);
		}
	}

	private static void update(String[] args) {
		// TODO Auto-generated method stub
		String indexUrl = args[1];
		String source = args[2];
		String destination = args[3];
		String updateFilePath = args[4];
		boolean autowarm = args[5].compareToIgnoreCase("on")==0;
		
		HttpSolrServer server = new HttpSolrServer(indexUrl);
	    ModifiableSolrParams qParams = new ModifiableSolrParams();
	    qParams.set("defType","edismax");
	    qParams.set("qf", source);
	    qParams.set(CommonParams.ROWS, Integer.MAX_VALUE);
	    
	    QueryResponse qResp;
	    
	    HashMap<String,String> cache = new HashMap<String,String>();
	    
	    if(autowarm==true) {	    	
	    	BufferedReader records;
			try {
				records = new BufferedReader(new FileReader(updateFilePath));
			
				String line = records.readLine();
			    int count = 0;
			    while(line!=null && line.length()>0) {
			    	String[] input = line.split("\t");
			    	cache.put(input[0], input[1]);
			    	line = records.readLine();
			    }
			    records.close();
			    
			    ArrayList<String> fl = new ArrayList<String>();
			    fl.add("id");
			    fl.add(source);
			    fl.add(destination);
			 
			    // add target extract field
			    qParams.set("fl", fl.toArray(new String[fl.size()]));
			 	qParams.set(CommonParams.Q, "*");
			    try {
			    	 qResp = server.query(qParams);
				      SolrDocumentList results = qResp.getResults();
				      if(results!=null && results.size()>0) {
				        for(int d=0; d<results.size(); d++) {
				        	SolrDocument doc = results.get(d);
							String sourceval = (String)doc.getFieldValue(source);
							boolean found = cache.containsKey(sourceval);
							if(found==true) {
						    	String destval = cache.get(sourceval); 			
								Object val = doc.getFieldValue(destination);
								
								if(val==null || ((Double)val).doubleValue()!=new Double(destval).doubleValue()) {	
									SolrInputDocument indoc = new SolrInputDocument();
									indoc.addField("id", (String)doc.getFieldValue("id"));
								
									Map<String,Object> destinationMap = new HashMap<>(1);
									destinationMap.put("add", destval);
									indoc.addField(destination, destinationMap);
									server.add(indoc);						
									
									try {
										if(++count%1000==0)
											server.commit();
									} catch (IOException e) {
										// TODO Auto-generated catch block
											e.printStackTrace();
									}
								}
							}
							else
								System.out.println(sourceval+"...not found");
				        }
				        try {
							server.commit();
						} catch (IOException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
				      }
					}catch (SolrServerException | IOException e) {
					   	e.printStackTrace();      
					}
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}   		    
	    }
	}
	
	private static void delete(String[] args, boolean delete) {
		// TODO Auto-generated method stub
		String indexUrl = args[1];
		String source = args[2];
		String filePath = args[3];
		boolean autowarm = args[4].compareToIgnoreCase("on")==0;
		
		HttpSolrServer server = new HttpSolrServer(indexUrl);
	    ModifiableSolrParams qParams = new ModifiableSolrParams();
	    qParams.set("defType","edismax");
	    qParams.set("qf", source);
	    qParams.set(CommonParams.ROWS, Integer.MAX_VALUE);
	    
	    QueryResponse qResp;
	    
	    HashSet<String> cache = new HashSet<String>();
	    
	    if(autowarm==true) {	    	
	    	BufferedReader records;
			try {
				records = new BufferedReader(new FileReader(filePath));
			
				String line = records.readLine();
			    int count = 0;
			    while(line!=null && line.length()>0) {
			    	cache.add(line);
			    	line = records.readLine();
			    }
			    records.close();
			    
			    ArrayList<String> fl = new ArrayList<String>();
			    fl.add("id");
			    fl.add(source);
			 
			    // add target extract field
			    qParams.set("fl", fl.toArray(new String[fl.size()]));
			 	qParams.set(CommonParams.Q, "*");
			    try {
			    	 qResp = server.query(qParams);
				      SolrDocumentList results = qResp.getResults();
				      if(results!=null && results.size()>0) {
				        for(int d=0; d<results.size(); d++) {
				        	SolrDocument doc = results.get(d);
							String sourceval = (String)doc.getFieldValue(source);
							boolean found = cache.contains(sourceval);
							if((delete==true && found==true) || 
									(delete==false && found==false)) {
								String idval = (String)doc.getFieldValue("id");
								System.out.println("deleting: "+idval);
									server.deleteById(idval);
							}	
						    				
								try {
									if(++count%1000==0)
										server.commit();
								} catch (IOException e) {
									// TODO Auto-generated catch block
										e.printStackTrace();
								}
							}							
				        }
				        try {
							server.commit();
						} catch (IOException | SolrServerException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
			    	} catch (SolrServerException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
			    } catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
	    }
	}
	
	private static void update1(String[] args) {
		// TODO Auto-generated method stub
		String indexUrl = args[0];
		String source = args[1];
		String destination = args[2];
		String updateFilePath = args[3];
		boolean autowarm = args[4].compareToIgnoreCase("on")==0;
		
		HttpSolrServer server = new HttpSolrServer(indexUrl);
	    ModifiableSolrParams qParams = new ModifiableSolrParams();
	    qParams.set("defType","edismax");
	    qParams.set("qf", source);
	    qParams.set(CommonParams.ROWS, Integer.MAX_VALUE);
	    
	    QueryResponse qResp;
	    
	    HashSet<String> cache = new HashSet<String>();
	    
	    if(autowarm==true) {
	    	qParams.set("fl", new String[]{source});
	    	qParams.set(CommonParams.Q, "*");
		    try {
		    	 qResp = server.query(qParams);
		      SolrDocumentList results = qResp.getResults();
		      if(results!=null && results.size()>0) {
		        for(int d=0; d<results.size(); d++) {
		        	SolrDocument doc = results.get(d);
					String value = (String)doc.getFieldValue(source);
					cache.add(value);
		        }
		      }
		    }catch (SolrServerException e) {
		    	e.printStackTrace();      
		    }   
	    }
	    
	    ArrayList<String> fl = new ArrayList<String>();
	    fl.add("id");
	    fl.add(source);
	    fl.add(destination);
	 
	    // add target extract field
	    qParams.set("fl", fl.toArray(new String[fl.size()]));
	    
	    try {
		    BufferedReader records = new BufferedReader(new FileReader(updateFilePath));	    
		    String line = records.readLine();
		    int count = 0;
		    while(line!=null && line.length()>0) {
		    	String[] input = line.split("\t");
		    	boolean found = cache.contains(input[0]);
		    	if(found==true || autowarm==false) {
			    	qParams.set(CommonParams.Q, input[0]);
				    try {
				      qResp = server.query(qParams);
				    
				      // loop on results and add new field
				      SolrDocumentList results = qResp.getResults();
				      if(results!=null && results.size()>0) {
				        for(int d=0; d<results.size(); d++) {
				        	SolrDocument doc = results.get(d);
							String value = (String)doc.getFieldValue(source);
							if(value.compareTo(input[0])==0) {
								found = true;
								
								Object val = doc.getFieldValue(destination);
								
								if(val==null || ((Double)val).doubleValue()!=new Double(input[1]).doubleValue())
								{	
									SolrInputDocument indoc = new SolrInputDocument();
									indoc.addField("id", (String)doc.getFieldValue("id"));
								
									Map<String,Object> destinationMap = new HashMap<>(1);
									destinationMap.put("add", input[1]);
									indoc.addField(destination, destinationMap);
									
									server.add(indoc);								
								}
							}
						}
				    }
					}catch (SolrServerException e) {
				    	e.printStackTrace();      
				    } 
		    	}
			    
			    if(found==false)
			    	System.out.println(input[0]+"...not found");
			    
		    	line = records.readLine();
		    	
		    	try {
					if(++count%1000==0)
						server.commit();
				} catch (SolrServerException e) {
					// TODO Auto-generated catch block
						e.printStackTrace();
				}
		    }
		    records.close();
		    try {
				server.commit();
			} catch (SolrServerException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
	    } catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
	}
}
