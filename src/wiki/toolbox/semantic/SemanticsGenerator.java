package wiki.toolbox.semantic;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Set;

import org.apache.commons.lang.ArrayUtils;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.HttpSolrServer;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.params.CommonParams;
import org.apache.solr.common.params.ModifiableSolrParams;
import org.apache.solr.common.util.NamedList;
import org.apache.solr.common.util.SimpleOrderedMap;

/*
 * Generate semantic mappings of input text using MSA
 * Input a path is a file containing samples in each line
 * generate for each line content semantic mappings and output a new file with mappings to output path.  
 * */

class IntegerWrap {
	IntegerWrap(int v) {
		value = v;
	}
	public int value;
	
	void inc() {
		value++;
	}
}

public class SemanticsGenerator {
	static public HashMap<String,CachedConceptInfo> cachedConceptsInfo = null;
	static private HashMap<String,Integer> titleIntMapping = null;
	static private HashMap<Integer,String> titleStrMapping = null;
	static private HashMap<Integer,CachedAssociationInfo> cachedAssociationsInfo = null;
  
	public SemanticsGenerator() {
		
	}
	
  /**
   * cache Wiki titles with some required information for fast retrieval
   */
  public void cacheConceptsInfo(String wikiUrl, boolean cacheRedirects, boolean cacheAnchors) {
	  if(cachedConceptsInfo!=null)
		  return;
	  
    // retrieve all concepts
    HttpSolrServer server = new HttpSolrServer(wikiUrl);
    ModifiableSolrParams conceptsQParams = new ModifiableSolrParams();
    conceptsQParams.set(CommonParams.Q, "*");
    conceptsQParams.set("defType","edismax");
    conceptsQParams.set("qf", "title");
    conceptsQParams.set(CommonParams.ROWS, Integer.MAX_VALUE);
    
    ArrayList<String> fl = new ArrayList<String>();
    fl.add("id");
    fl.add("title");
    fl.add("length");
    fl.add("category");
    fl.add("redirect");
    fl.add("pagerank");
    if(cacheAnchors==true)
    	fl.add("anchor");
        
    // add target extract field
    conceptsQParams.set("fl", fl.toArray(new String[fl.size()]));
    QueryResponse conceptsQResp;
    try {
      conceptsQResp = server.query(conceptsQParams);
    
      // loop on results and add to concepts
      SolrDocumentList results = conceptsQResp.getResults();
      if(results!=null && results.size()>0) {
        cachedConceptsInfo = new HashMap<String,CachedConceptInfo>(/*5000000*/);
        String title = "";
        String docno = "";
        Integer length;
        Double pr;
        Collection<Object> category = null;
        Object[] multi = null;
        CachedConceptInfo cachedInfo = null;
        for(int i=0; i<results.size(); i++) {
          String cat1 = "";
          String cat2 = "";
          SolrDocument doc = results.get(i);
          
          // retrieve title
          title = (String) doc.getFieldValue("title");
  
          // retrieve docno
          docno = (String) doc.getFieldValue("id");
          
          // retrieve length
          int len = 0;
          length = ((Integer) doc.getFieldValue("length"));
          if(length!=null)
            len = length.intValue();
  
          float pagerank = 0.0f;
          pr = ((Double) doc.getFieldValue("pagerank"));
          if(pr!=null)
        	  pagerank = pr.floatValue();  
          
          // retrieve categories
          category = doc.getFieldValues("category");
          if(category!=null) {
            multi = category.toArray();
            cat1 = multi.length>0? (String)multi[0]:"";
            cat2 = multi.length>1? (String)multi[1]:"";
          }
          cachedInfo = new CachedConceptInfo(title, len, docno, cat1, cat2, pagerank);
          cachedConceptsInfo.put(title,  cachedInfo);
          
          // add redirects (see also might refer to redirects so add them)
          Collection<Object> redirectValues = doc.getFieldValues("redirect");
          if(redirectValues!=null) {
            Object[] redirects = redirectValues.toArray();
            for(int t=0; t<redirects.length; t++) {
              String redirect = (String) redirects[t];
              cachedConceptsInfo.put(redirect,  cachedInfo);
              
              if(cacheRedirects==true) {
            	  cachedInfo.redirects.add(redirect);
              }
            }
          }

          // cache anchors
          if(cacheAnchors==true) {
          Collection<Object> anchorValues = doc.getFieldValues("anchor");
          if(anchorValues!=null) {
            Object[] anchors = anchorValues.toArray();
            for(int t=0; t<anchors.length; t++) {
              String anchor = (String) anchors[t];
              cachedInfo.anchors.add(anchor.replace("\"", ""));
              }
            }
          }
        
          //if(cachedConceptsInfo.size()>500000)
            //break;
        }
        System.out.println("cached ("+cachedConceptsInfo.size()+") concepts.");
      }              
    } catch (SolrServerException e) {
    	e.printStackTrace();      
    }        
  }
  
  // cache all title associations into memory for fast access
  public void cacheAssociationsInfo(String associationspath) {
	  if(cachedAssociationsInfo!=null)
		  return;
	  
    try {
      BufferedReader f;      
      f = new BufferedReader(new FileReader(associationspath));
      
      int curassoc=0, key=0, idx;
      String line;
      CachedAssociationInfo associationInfo = null;
      
      titleIntMapping = new HashMap<String,Integer>(/*5000000*/);
      titleStrMapping = new HashMap<Integer,String>(/*5000000*/);
      
      cachedAssociationsInfo = new HashMap<Integer,CachedAssociationInfo>(/*5000000*/);
      
      line = f.readLine();
      String[] association = new String[2];
      while(line!=null) {
        String[] associations = line.split("#\\$#");
        association = associations[0].split("/\\\\/\\\\");
        if(association[0].length()==0) {
      	  System.out.println("Oops: Unexpected line ("+line+")");
        }
        else { 
	        for(int i=0; i<associations.length; i++) {
	          association = associations[i].split("/\\\\/\\\\");
	          
	          // look it up
	          Integer index = titleIntMapping.get(association[0]);
	          if(index==null) { // add it
	            titleIntMapping.put(association[0], new Integer(curassoc));
	            titleStrMapping.put(new Integer(curassoc), association[0]);
	            idx = curassoc;
	            curassoc++;
	          }
	          else {
	            idx = index.intValue();
	          }
	          if(i==0) {
	            associationInfo = new CachedAssociationInfo(Integer.parseInt(association[1]));
	            key = idx;
	          }
	          else { // add it to associations
	            if(association.length!=2)
	              System.out.println(line);
	            else
	            	associationInfo.addAssociation(idx,Integer.parseInt(association[1]));
	          }
	        }
	        if(associations.length>0)
	          cachedAssociationsInfo.put(new Integer(key), associationInfo);
      	}
        //if(cachedAssociationsInfo.size()>500000)
          //break;
        line = f.readLine();
      }
      f.close();
      
    } catch (IOException e) {
    	e.printStackTrace();
    }    
  }


  /*
   * @param concept source concept for which we search for related concepts
   * @param relatedConcepts related concepts retrieved
   * @param maxhits maximum hits in the initial wiki search
   * @param e_Method method to use for semantic concept retrieval (ESA,ESA_anchors, ESA_seealso, ESA_anchors_seealso)
   * @param enable_title_search whether we search in wiki titles as well as text or not
   */
  public void retrieveRelatedConcepts(String concept, HashMap<String,SemanticConcept> relatedConcepts, 
      SemanticSearchConfigParams params) {
    //TODO: 
    /* do we need to intersect with technical dictionary
     * do we need to score see also based on cross-reference/see also graph similarity (e.g., no of common titles in the see also graph)
     * do we need to filter out titles with places, nationality,N_N,N_N_N,Adj_N_N...etc while indexing
     * do we need to look at cross-references
     * do we need to search in title too with boosting factor then remove exact match at the end
     * do we need to add "" here or in the request
     */
    concept = QueryParser.escape(concept.toLowerCase().replace(" not ", " \\not ").replace(" or ", " \\or ").replace(" and ", " \\and "));
    
    if(params.quoted_search==true)
    	concept = "\"" + concept + "\"";
    
    // retrieve related concepts
    String filterQueryString = "title_ngrams:[0 TO "+String.format("%d", params.hidden_max_title_ngrams)+"] "
        + "AND length:["+String.format("%d", params.hidden_min_wiki_length)+" TO *]";
    String queryString = "";
    if(params.enable_title_search) {
      queryString += "(title:"+concept+" OR "+params.hidden_wiki_search_field+":"+concept+")";
    }
    else {
      queryString += "("+params.hidden_wiki_search_field+":"+concept+")";
    }
    if(params.hidden_wiki_extra_query.length()>0)
    	filterQueryString += " "+ params.hidden_wiki_extra_query;
    
    if(params.debug==true)
    	System.out.println("Query: "+ queryString);
    
    HttpSolrServer server = new HttpSolrServer(params.wikiUrl);
    ModifiableSolrParams conceptsQParams = new ModifiableSolrParams();
    conceptsQParams.set(CommonParams.Q, queryString);
    conceptsQParams.set("defType","edismax");
    conceptsQParams.set(CommonParams.FQ,filterQueryString);
    conceptsQParams.set(CommonParams.ROWS, params.hidden_max_hits);
    
    // add target extract field
    conceptsQParams.set("fl", new String[]{"score","title","anchor","seealso","seealso_ngrams","pagerank","category"});
    QueryResponse conceptsQResp;
    try {
      conceptsQResp = server.query(conceptsQParams);
    
      // loop on results and add to concepts
      SolrDocumentList results = conceptsQResp.getResults();
      if(results!=null && results.size()>0) {
        int cur_id = 1;
        int cur_parent_id = 0;
        
        for(int i=0; i<results.size(); i++) {
          SolrDocument doc = results.get(i);
          boolean relevant = false;
          String title = (String) doc.getFieldValue("title");
          float score = ((Float) doc.getFieldValue("score")).floatValue();
          Object obj = doc.getFieldValue("pagerank");
          float pagerank = 0;
          if(obj!=null)
          	pagerank = ((Double) obj).floatValue();          
          String ner = "";
          /*HEREHERE*///String ner = indexReader.document(hits[i].doc).getField("title_ne").stringValue();/*HEREHERE*/
          
          String cats = "";
          if(params.write_cats==true) {
	          Collection<Object> catsValues = doc.getFieldValues("category");
	          if(catsValues!=null) {
	            Object[] catsArr = catsValues.toArray();
	            for(int t=0; t<catsArr.length; t++) {
	              cats += (String) catsArr[t] + "\t";
	            }
	          }
          }
          
          CachedConceptInfo cachedInfo = null;
          CachedAssociationInfo cachedAssoInfo = null;
          
          //System.out.println(title.stringValue());
          // check if relevant concept
          boolean relevantTitle = true;//TODO: do we need to call isRelevantConcept(f.stringValue());
          if(relevantTitle==true) {
            relevant = true;            
            // check if already there
            SemanticConcept sem = relatedConcepts.get(title);
            if(sem==null) { // new concept              
              cachedInfo = cachedConceptsInfo.get(title);
              if(cachedInfo==null) {
                //if(params.debug==true)
                System.out.println(title+"...title not found!");
                if(params.hidden_relax_cache==true)
                  cachedInfo = new CachedConceptInfo(title, 0, "", "", "", 0);
              }
              else if(title.compareTo(cachedInfo.title)!=0){
            	  System.out.println("Oops! unexpected ("+title+"<->"+cachedInfo.title+")");
            	  title = cachedInfo.title;
              }
              if(cachedInfo!=null) {
            	  if(params.hidden_pagerank_weighting==false) {
            		  sem = new SemanticConcept(title, cachedInfo, ner, cats, score,
            				  cur_id, 0, 0, Enums.ENUM_CONCEPT_TYPE.e_TITLE, cachedInfo.pagerank);
            	  }
            	  else {
            		  sem = new SemanticConcept(title, cachedInfo, ner, cats, cachedInfo.pagerank,
            				  cur_id, 0, 0, Enums.ENUM_CONCEPT_TYPE.e_TITLE, score);
            	  }
                cur_parent_id = cur_id;
                cur_id++;
                // get its associations
                if(params.e_Method!=Enums.ENUM_SEMANTIC_METHOD.e_MSA && 
                    params.e_Method!=Enums.ENUM_SEMANTIC_METHOD.e_MSA_ANCHORS) {
                  Integer I = titleIntMapping.get(title);
                  if(I!=null) {
                    cachedAssoInfo = cachedAssociationsInfo.get(I);
                  }
                  else {
                	  //if(params.debug==true)
                	  System.out.println(title+"...title not in mappings!");
                  }
                }                              
              }
            }
            else { // existing concept, update its weight to higher weight
              cachedInfo = sem.cachedInfo;
              if(params.hidden_pagerank_weighting==false) {
            	  sem.weight = sem.weight>score?sem.weight:score;
              }
              cur_parent_id = sem.id;
            }
            if(sem!=null)
              relatedConcepts.put(sem.name, sem);            
          }
          else {
        	  if(params.debug==true)
        		  System.out.println(title+"...title not relevant!");
          }
          if(relevant==true && (params.e_Method==Enums.ENUM_SEMANTIC_METHOD.e_MSA_ANCHORS || 
                  params.e_Method==Enums.ENUM_SEMANTIC_METHOD.e_MSA_ANCHORS_SEE_ALSO || 
                      params.e_Method==Enums.ENUM_SEMANTIC_METHOD.e_MSA_ANCHORS_SEE_ALSO_ASSO)) // retrieve anchors
          {
            relevant = false;
            Collection<Object> anchorValues = doc.getFieldValues("anchor");
            if(anchorValues!=null) {
              Object[] anchors = anchorValues.toArray();
              for(int t=0; t<anchors.length; t++) {
                String anchor = (String) anchors[t];
                //System.out.println(f.stringValue());
                // check if relevant concept
                relevantTitle = true;//TODO: do we need to call isRelevantConcept(f.stringValue());
                if(relevantTitle==true) {
                  relevant = true;
                  
                  // check if already there
                  SemanticConcept sem = relatedConcepts.get(anchor);
                  if(sem==null) { // new concept                  
                	  if(params.hidden_pagerank_weighting==false) {
                		  sem = new SemanticConcept(anchor, cachedInfo, ner, "", score-0.0001f, 
                				  cur_id, cur_parent_id, 0, Enums.ENUM_CONCEPT_TYPE.e_ANCHOR, cachedInfo.pagerank);
                      }
                	  else {
                		  sem = new SemanticConcept(anchor, cachedInfo, ner, "", cachedInfo.pagerank, 
                				  cur_id, cur_parent_id, 0, Enums.ENUM_CONCEPT_TYPE.e_ANCHOR, score-0.0001f);
                	  }
                    cur_id++;
                  }
                  else { // existing concept, update its weight to higher weight
                    cachedInfo = sem.cachedInfo;
                    if(params.hidden_pagerank_weighting==false) {
                    	sem.weight = sem.weight>score-0.0001f?sem.weight:score-0.0001f;
                    }
                  }
                  if(sem!=null)
                    relatedConcepts.put(sem.name, sem);                
                }
                else {
                	if(params.debug==true)
                		System.out.println(anchor+"...title not relevant!");
                }
              }
            }
          }
          
          //System.out.println();
          if(params.hidden_relax_see_also==false || relevant) {
            // force see also is enabled OR,
            // the original title or one of its anchors is relevant
            // in this case we can add its see_also
            if(params.e_Method==Enums.ENUM_SEMANTIC_METHOD.e_MSA_SEE_ALSO || 
                params.e_Method==Enums.ENUM_SEMANTIC_METHOD.e_MSA_ANCHORS_SEE_ALSO) { // add See also to the hit list
            	IntegerWrap Cur_Id = new IntegerWrap(cur_id);
            	getSeeAlso(doc, cachedAssoInfo, score, Cur_Id, cur_parent_id, relatedConcepts, params);
//              Collection<Object> seeAlsoValues = doc.getFieldValues("seealso");
//              Collection<Object> seeAlsoNgramsValues = doc.getFieldValues("seealso_ngrams");
//              if(seeAlsoValues!=null) {
//            	  if(seeAlsoNgramsValues!=null) {
//	                Object[] multiSeeAlso = seeAlsoValues.toArray();
//	                /*HEREHERE*//*Object[] multiSeeAlsoNE = doc.getFieldValues("see_also_ne").toArray();*//*HEREHERE*/
//	                Object[] multiSeeAlsoNgrams = seeAlsoNgramsValues.toArray();              
//	                
//	                int min = Math.min(multiSeeAlsoNgrams.length,multiSeeAlso.length);
//	                for(int s=0; s<min; s++) {
//	                  //System.out.println(f.stringValue());
//	                  // check if relevant concept
//	                  relevantTitle = true; //TODO: do we need to call isRelevantConcept(multiSeeAlso[s].stringValue());
//	                  /*String re = "\\S+(\\s\\S+){0,"+String.valueOf(params.hidden_max_seealso_ngrams-1)+"}";
//	                  if(multiSeeAlso[s].stringValue().toLowerCase().matches(re)==false)
//	                    relevantTitle = false;
//	                  */
//	                  if(((Integer)multiSeeAlsoNgrams[s]).intValue()>params.hidden_max_seealso_ngrams)
//	                    relevantTitle = false;
//	                  
//	                  if(relevantTitle==true) {
//	                	String seealso = (String)multiSeeAlso[s];
//	                	cachedInfo = cachedConceptsInfo.get(seealso);
//	                    if(cachedInfo==null) {
//	                    	if(params.debug==true)
//	                    		System.out.println(seealso+"...see_also not found!");
//	                    	if(params.hidden_relax_cache==true)
//	                    		cachedInfo = new CachedConceptInfo(seealso, 0, "", "", "", 0);
//	                    }
//	                    else
//	                    	seealso = cachedInfo.title;  // might be redirect so get original title
//	                    
//	                	// check if already there
//	                    SemanticConcept sem = relatedConcepts.get(seealso); 
//	                    if(sem==null) { // new concept
//	                      if(cachedInfo!=null) {  
//	                        // get see also association info
//	                        int asso_cnt = 0;
//	                        if(cachedAssoInfo!=null) {
//	                          Integer I = titleIntMapping.get(seealso);
//	                          if(I!=null) {
//	                            for(Integer[] info :cachedAssoInfo.associations) {
//	                              if(info[0].intValue()==I.intValue()) {
//	                                asso_cnt = info[1].intValue();
//	                                break;
//	                              }
//	                            }
//	                            if(asso_cnt==0) {
//	                            	if(params.debug==true)
//	                            		System.out.println(seealso+"...see_also not in associations!");
//	                            }
//	                          }
//	                          else {
//	                        	  if(params.debug==true)
//	                        		  System.out.println(seealso+"...see_also not in mappings!");
//	                          }
//	                        }
//	                        if((cachedInfo.length==0 || cachedInfo.length>=params.hidden_min_seealso_length) && 
//	                            (asso_cnt==0 || (params.hidden_min_confidence>0.0 && ((float)asso_cnt)/cachedAssoInfo.sup>=params.hidden_min_confidence) || 
//	                            (params.hidden_min_confidence<0.00001 && asso_cnt>=params.hidden_min_asso_cnt))) { // support > minimum support
//	                        	if(params.hidden_pagerank_weighting==false) { 
//	                        		sem = new SemanticConcept(seealso, 
//	                        				cachedInfo, ""/*HEREHERE*//*(String)multiSeeAlsoNE[s]*//*HEREHERE*/, 
//	                        				score-0.0002f, cur_id, cur_parent_id, asso_cnt, Enums.ENUM_CONCEPT_TYPE.e_SEE_ALSO, 
//	                        				cachedInfo.pagerank);
//	                        	}
//	                        	else {
//	                        		sem = new SemanticConcept(seealso, 
//	                        				cachedInfo, ""/*HEREHERE*//*(String)multiSeeAlsoNE[s]*//*HEREHERE*/, 
//	                        				cachedInfo.pagerank, cur_id, cur_parent_id, asso_cnt, Enums.ENUM_CONCEPT_TYPE.e_SEE_ALSO, 
//	                        				score-0.0002f);
//	                        	}
//	                          cur_id++;                      
//	                        }
//	                      }
//	                    }
//	                    else { // existing concept, update its weight to higher weight
//	                      cachedInfo = sem.cachedInfo;
//	                      if(params.hidden_pagerank_weighting==false) {
//	                    	  sem.weight = sem.weight>score-0.0002f?sem.weight:score-0.0002f;
//	                      }	                    	  
//	                    }
//	                    if(sem!=null)
//	                      relatedConcepts.put(sem.name, sem);            
//	                  }
//	                  //else
//	                    //System.out.println(multiSeeAlso[s].stringValue()+"...see-also not relevant!");
//	                  //System.out.println();
//	                }
//              	}
//            	else {
//            		System.out.println("Oops! unexpected null seealso ngrams for ("+title+"<->"+(String) doc.getFieldValue("title")+")");
//            	}
//              }
            }
            else if(params.e_Method==Enums.ENUM_SEMANTIC_METHOD.e_MSA_SEE_ALSO_ASSO || 
                params.e_Method==Enums.ENUM_SEMANTIC_METHOD.e_MSA_ANCHORS_SEE_ALSO_ASSO) { // add see also using association mining
            	IntegerWrap Cur_Id = new IntegerWrap(cur_id);
            	getSeeAlsoAsso(title, score, Cur_Id, cur_parent_id, relatedConcepts, params, 2);
            	cur_id = Cur_Id.value;
            }
          }
        }
      }
      else {
    	  if(params.debug==true)
    		  System.out.println("No semantic results found :(");
      }
    } catch (SolrServerException e) {
    	System.out.println("Error executing query: "+queryString);
    	e.printStackTrace();      
    }
  }
  
//  public HashMap<String, SemanticConcept> getSeeAlso(String concept, SemanticSearchConfigParams params) {
//	  HashMap<String, SemanticConcept> relatedConcepts = new HashMap<String, SemanticConcept>();
//	  CachedConceptInfo cachedInfo = cachedConceptsInfo.get(concept);
//	  if(cachedInfo!=null) {
//	    // retrieve related concepts
//	    String queryString = "";
//	    queryString += "id:"+cachedInfo.docno;
//	    
//	    if(params.debug==true)
//	    	System.out.println("Query: "+ queryString);
//	    
//	    HttpSolrServer server = new HttpSolrServer(params.wikiUrl);
//	    ModifiableSolrParams conceptsQParams = new ModifiableSolrParams();
//	    conceptsQParams.set(CommonParams.Q, queryString);
//	    conceptsQParams.set("defType","edismax");
//	    conceptsQParams.set(CommonParams.ROWS, 1);
//	    
//	    // add target extract field
//	    conceptsQParams.set("fl", new String[]{"score","title","anchor","seealso","seealso_ngrams","pagerank"});
//	    QueryResponse conceptsQResp;
//	    try {
//	      conceptsQResp = server.query(conceptsQParams);
//	    
//	      // loop on results and add to concepts
//	      SolrDocumentList results = conceptsQResp.getResults();
//	      if(results!=null && results.size()==1) {
//	        int cur_id = 1;
//	        int cur_parent_id = 0;	        
//	        
//	          SolrDocument doc = results.get(0);
//	          boolean relevant = false;
//	          String title = (String) doc.getFieldValue("title");
//	          if(title.equals(concept)){
//		          float score = ((Float) doc.getFieldValue("score")).floatValue();
//		          Object obj = doc.getFieldValue("pagerank");
//		          float pagerank = 0;
//		          if(obj!=null)
//		          	pagerank = ((Double) obj).floatValue();          
//		          String ner = "";
//		          /*HEREHERE*///String ner = indexReader.document(hits[i].doc).getField("title_ne").stringValue();/*HEREHERE*/
//		          CachedAssociationInfo cachedAssoInfo = null;
//		          
//		          //System.out.println(title.stringValue());
//		          // check if relevant concept
//		          boolean relevantTitle = true;//TODO: do we need to call isRelevantConcept(f.stringValue());
//		          if(relevantTitle==true) {
//		            relevant = true;            
//		                           
//	                // get its associations
//	                if(params.e_Method!=Enums.ENUM_SEMANTIC_METHOD.e_MSA && 
//	                    params.e_Method!=Enums.ENUM_SEMANTIC_METHOD.e_MSA_ANCHORS) {
//	                  Integer I = titleIntMapping.get(title);
//	                  if(I!=null) {
//	                    cachedAssoInfo = cachedAssociationsInfo.get(I);
//	                  }
//	                  else {
//	                	  //if(params.debug==true)
//	                	  System.out.println(title+"...title not in mappings!");
//	                  }
//	                }      
//		          }
//		          else {
//		        	  if(params.debug==true)
//		        		  System.out.println(title+"...title not relevant!");
//		          }
////		          if(relevant==true && (params.e_Method==Enums.ENUM_SEMANTIC_METHOD.e_MSA_ANCHORS || 
////		                  params.e_Method==Enums.ENUM_SEMANTIC_METHOD.e_MSA_ANCHORS_SEE_ALSO || 
////		                      params.e_Method==Enums.ENUM_SEMANTIC_METHOD.e_MSA_ANCHORS_SEE_ALSO_ASSO)) // retrieve anchors
////		          {
////		            relevant = false;
////		            Collection<Object> anchorValues = doc.getFieldValues("anchor");
////		            if(anchorValues!=null) {
////		              Object[] anchors = anchorValues.toArray();
////		              for(int t=0; t<anchors.length; t++) {
////		                String anchor = (String) anchors[t];
////		                //System.out.println(f.stringValue());
////		                // check if relevant concept
////		                relevantTitle = true;//TODO: do we need to call isRelevantConcept(f.stringValue());
////		                if(relevantTitle==true) {
////		                  relevant = true;
////		                  
////		                  // check if already there
////		                  SemanticConcept sem = relatedConcepts.get(anchor);
////		                  if(sem==null) { // new concept                  
////		                	  if(params.hidden_pagerank_weighting==false) {
////		                		  sem = new SemanticConcept(anchor, cachedInfo, ner, score-0.0001f, 
////		                				  cur_id, cur_parent_id, 0, Enums.ENUM_CONCEPT_TYPE.e_ANCHOR, cachedInfo.pagerank);
////		                      }
////		                	  else {
////		                		  sem = new SemanticConcept(anchor, cachedInfo, ner, cachedInfo.pagerank, 
////		                				  cur_id, cur_parent_id, 0, Enums.ENUM_CONCEPT_TYPE.e_ANCHOR, score-0.0001f);
////		                	  }
////		                    cur_id++;
////		                  }
////		                  else { // existing concept, update its weight to higher weight
////		                    cachedInfo = sem.cachedInfo;
////		                    if(params.hidden_pagerank_weighting==false) {
////		                    	sem.weight = sem.weight>score-0.0001f?sem.weight:score-0.0001f;
////		                    }
////		                  }
////		                  if(sem!=null)
////		                    relatedConcepts.put(sem.name, sem);                
////		                }
////		                else {
////		                	if(params.debug==true)
////		                		System.out.println(anchor+"...title not relevant!");
////		                }
////		              }
////		            }
////		          }
//		          
//		          //System.out.println();
//		          if(params.hidden_relax_see_also==false || relevant) {
//		            // force see also is enabled OR,
//		            // the original title or one of its anchors is relevant
//		            // in this case we can add its see_also
//		            if(params.e_Method==Enums.ENUM_SEMANTIC_METHOD.e_MSA_SEE_ALSO || 
//		                params.e_Method==Enums.ENUM_SEMANTIC_METHOD.e_MSA_ANCHORS_SEE_ALSO) { // add See also to the hit list
//		            	IntegerWrap Cur_Id = new IntegerWrap(1);
//		            	getSeeAlso(doc, cachedAssoInfo, score, Cur_Id, 0, relatedConcepts, params);
//		            }
//		          }
//	          }	        
//	      }
//	      else {
//	    	  if(params.debug==true)
//	    		  System.out.println("No semantic results found :(");
//	      }
//	    } catch (SolrServerException e) {
//	    	System.out.println("Error executing query: "+queryString);
//	    	e.printStackTrace();      
//	    }
//	  }
//	  return relatedConcepts;
//  }
  
  private void getSeeAlso(SolrDocument doc, CachedAssociationInfo cachedAssoInfo, 
		  float score, IntegerWrap Cur_Id, int cur_parent_id, 
		  HashMap<String, SemanticConcept> relatedConcepts, SemanticSearchConfigParams params) {
	// TODO Auto-generated method stub
	  String title = (String) doc.getFieldValue("title");
	  Collection<Object> seeAlsoValues = doc.getFieldValues("seealso");
      Collection<Object> seeAlsoNgramsValues = doc.getFieldValues("seealso_ngrams");
      if(seeAlsoValues!=null) {
    	  if(seeAlsoNgramsValues!=null) {
            Object[] multiSeeAlso = seeAlsoValues.toArray();
            /*HEREHERE*//*Object[] multiSeeAlsoNE = doc.getFieldValues("see_also_ne").toArray();*//*HEREHERE*/
            Object[] multiSeeAlsoNgrams = seeAlsoNgramsValues.toArray();              
            
            int min = Math.min(multiSeeAlsoNgrams.length,multiSeeAlso.length);
            for(int s=0; s<min; s++) {
              //System.out.println(f.stringValue());
              // check if relevant concept
              boolean relevantTitle = true; //TODO: do we need to call isRelevantConcept(multiSeeAlso[s].stringValue());
              /*String re = "\\S+(\\s\\S+){0,"+String.valueOf(params.hidden_max_seealso_ngrams-1)+"}";
              if(multiSeeAlso[s].stringValue().toLowerCase().matches(re)==false)
                relevantTitle = false;
              */
              if(((Integer)multiSeeAlsoNgrams[s]).intValue()>params.hidden_max_seealso_ngrams)
                relevantTitle = false;
              
              if(relevantTitle==true) {
            	String seealso = (String)multiSeeAlso[s];
            	CachedConceptInfo cachedInfo = cachedConceptsInfo.get(seealso);
                if(cachedInfo==null) {
                	if(params.debug==true)
                		System.out.println(seealso+"...see_also not found!");
                	if(params.hidden_relax_cache==true)
                		cachedInfo = new CachedConceptInfo(seealso, 0, "", "", "", 0);
                }
                else
                	seealso = cachedInfo.title;  // might be redirect so get original title
                
            	// check if already there
                SemanticConcept sem = relatedConcepts.get(seealso); 
                if(sem==null) { // new concept
                  if(cachedInfo!=null) {  
                    // get see also association info
                    int asso_cnt = 0;
                    if(cachedAssoInfo!=null) {
                      Integer I = titleIntMapping.get(seealso);
                      if(I!=null) {
                        for(Integer[] info :cachedAssoInfo.associations) {
                          if(info[0].intValue()==I.intValue()) {
                            asso_cnt = info[1].intValue();
                            break;
                          }
                        }
                        if(asso_cnt==0) {
                        	if(params.debug==true)
                        		System.out.println(seealso+"...see_also not in associations!");
                        }
                      }
                      else {
                    	  if(params.debug==true)
                    		  System.out.println(seealso+"...see_also not in mappings!");
                      }
                    }
                    if((cachedInfo.length==0 || cachedInfo.length>=params.hidden_min_seealso_length) && 
                        (asso_cnt==0 || (params.hidden_min_confidence>0.0 && ((float)asso_cnt)/cachedAssoInfo.sup>=params.hidden_min_confidence) || 
                        (params.hidden_min_confidence<0.00001 && asso_cnt>=params.hidden_min_asso_cnt))) { // support > minimum support
                    	if(params.hidden_pagerank_weighting==false) { 
                    		sem = new SemanticConcept(seealso, 
                    				cachedInfo, ""/*HEREHERE*//*(String)multiSeeAlsoNE[s]*//*HEREHERE*/, "", 
                    				score-0.0002f, Cur_Id.value, cur_parent_id, asso_cnt, Enums.ENUM_CONCEPT_TYPE.e_SEE_ALSO, 
                    				cachedInfo.pagerank);
                    	}
                    	else {
                    		sem = new SemanticConcept(seealso, 
                    				cachedInfo, ""/*HEREHERE*//*(String)multiSeeAlsoNE[s]*//*HEREHERE*/, "", 
                    				cachedInfo.pagerank, Cur_Id.value, cur_parent_id, asso_cnt, Enums.ENUM_CONCEPT_TYPE.e_SEE_ALSO, 
                    				score-0.0002f);
                    	}
                    	Cur_Id.inc();                      
                    }
                  }
                }
                else { // existing concept, update its weight to higher weight
                  cachedInfo = sem.cachedInfo;
                  if(params.hidden_pagerank_weighting==false) {
                	  sem.weight = sem.weight>score-0.0002f?sem.weight:score-0.0002f;
                  }	                    	  
                }
                if(sem!=null)
                  relatedConcepts.put(sem.name, sem);            
              }
              //else
                //System.out.println(multiSeeAlso[s].stringValue()+"...see-also not relevant!");
              //System.out.println();
            }
      	}
    	else {
    		System.out.println("Oops! unexpected null seealso ngrams for ("+title+"<->"+(String) doc.getFieldValue("title")+")");
    	}
      }
  }
  
  public HashMap<String, SemanticConcept> getSeeAlsoAsso(String title, SemanticSearchConfigParams params) {
	  HashMap<String, SemanticConcept> relatedConcepts = new HashMap<String, SemanticConcept>();
	  getSeeAlsoAsso(title, 0, new IntegerWrap(1), 0, relatedConcepts, params, 2);
	  return relatedConcepts;
  }
  
  private void getSeeAlsoAsso(String title, float score, IntegerWrap Cur_Id, int cur_parent_id, 
		  HashMap<String, SemanticConcept> relatedConcepts, SemanticSearchConfigParams params, 
		  int cur_level) {
	// TODO Auto-generated method stub
	  Integer key = titleIntMapping.get(title);
      CachedAssociationInfo assoInfo = cachedAssociationsInfo.get(key);
      if(assoInfo==null) {
    	  if(params.debug==true)
    		  System.out.println(title + "..no associations cached!");
      }
      else {
        for(int a=0; a<assoInfo.associations.size(); a++) {
          Integer[] assos = assoInfo.associations.get(a);
          if((params.hidden_min_confidence>0.0 && ((float)assos[1])/assoInfo.sup>=params.hidden_min_confidence) || 
                  (params.hidden_min_confidence<0.00001 && assos[1]>=params.hidden_min_asso_cnt)) // support > minimum support
          {
        	  String assoStr = titleStrMapping.get(assos[0]);            
             
              // check if already there
              SemanticConcept sem = relatedConcepts.get(assoStr); 
              if(sem==null) { // new concept
            	 CachedConceptInfo cachedInfo = cachedConceptsInfo.get(assoStr);
                if(cachedInfo==null) {
                	if(params.debug==true)
                		System.out.println(assoStr+"...see_also not found!");
                  if(params.hidden_relax_cache==true)
                    cachedInfo = new CachedConceptInfo(assoStr, 0, "", "", "", 0);
                }
                else
                	assoStr = cachedInfo.title;  // might be redirect so get original title
                
                // check if relevant concept
                boolean relevantTitle = true; //TODO: do we need to call isRelevantConcept(multiSeeAlso[s].stringValue());
                if(getTitleLength(assoStr)>params.hidden_max_seealso_ngrams)
                  relevantTitle = false;
                if(relevantTitle==true){
	                if(cachedInfo!=null)
	                {  
	                  if(cachedInfo.length==0 || cachedInfo.length>=params.hidden_min_seealso_length) { 
	                	  if(params.hidden_pagerank_weighting==false) {
	                    	  sem = new SemanticConcept(assoStr,
	                    			  cachedInfo, "M", "", score-0.0002f, Cur_Id.value, cur_parent_id, assos[1], 
	                    			  Enums.ENUM_CONCEPT_TYPE.e_SEE_ALSO, cachedInfo.pagerank);
	                	  }
	                	  else {
	                		  sem = new SemanticConcept(assoStr,
	                    			  cachedInfo, "M", "", cachedInfo.pagerank, Cur_Id.value, cur_parent_id, 
	                    			  assos[1], Enums.ENUM_CONCEPT_TYPE.e_SEE_ALSO, score-0.0002f);
	                	  }
	                    int parent_id = Cur_Id.value;
	                    Cur_Id.inc();
	                    if(cur_level<params.hidden_max_levels) {
	                    	getSeeAlsoAsso(assoStr, score-0.0002f, Cur_Id, parent_id, relatedConcepts, params, cur_level+1);
	                    }
	                  }
	                }
              	}
              }
              else { // existing concept, update its weight to higher weight
            	CachedConceptInfo cachedInfo = sem.cachedInfo;
            	if(params.hidden_pagerank_weighting==false) {
            		sem.weight = sem.weight>score-0.0002f?sem.weight:score-0.0002f;
                }
              }
              if(sem!=null)
                relatedConcepts.put(sem.name, sem);
            
            //else
              //System.out.println(assoStr+"...see-also not relevant!");
          }
          //else
            //System.out.println(assos[0]+"...see-also below threshold!");
        }
      }
  }

/*
   * @param concept source concept for which we search for related concepts
   * @param relatedConcepts related concepts retrieved
   * @param maxhits maximum hits in the initial wiki search
   * @param e_Method method to use for semantic concept retrieval (ESA,ESA_anchors, ESA_seealso, ESA_anchors_seealso)
   * @param enable_title_search whether we search in wiki titles as well as text or not
   */
//  public void retrieveRelatedConcepts1(String concept, HashMap<String,SemanticConcept> relatedConcepts, 
//      SemanticSearchConfigParams params) {
//    //TODO: 
//    /* do we need to intersect with technical dictionary
//     * do we need to score see also based on cross-reference/see also graph similarity (e.g., no of common titles in the see also graph)
//     * do we need to filter out titles with places, nationality,N_N,N_N_N,Adj_N_N...etc while indexing
//     * do we need to look at cross-references
//     * do we need to search in title too with boosting factor then remove exact match at the end
//     * do we need to add "" here or in the request
//     */
//    concept = QueryParser.escape(concept.toLowerCase().replace(" not ", " \\not ").replace(" or ", " \\or ").replace(" and ", " \\and "));
//    
//    // retrieve related concepts
//    String filterQueryString = "title_ngrams:[0 TO "+String.format("%d", params.hidden_max_title_ngrams)+"] "
//        + "AND length:["+String.format("%d", params.hidden_min_wiki_length)+" TO *]";
//    String queryString = "";
//    if(params.enable_title_search) {
//      queryString += "(title:"+concept+" OR "+params.hidden_wiki_search_field+":"+concept+")";
//    }
//    else {
//      queryString += "("+params.hidden_wiki_search_field+":"+concept+")";
//    }
//    if(params.hidden_wiki_extra_query.length()>0)
//    	filterQueryString += " "+ params.hidden_wiki_extra_query;
//    
//    if(params.debug==true)
//    	System.out.println("Query: "+ queryString);
//    
//    HttpSolrServer server = new HttpSolrServer(params.wikiUrl);
//    ModifiableSolrParams conceptsQParams = new ModifiableSolrParams();
//    conceptsQParams.set(CommonParams.Q, queryString);
//    conceptsQParams.set("defType","edismax");
//    conceptsQParams.set(CommonParams.FQ,filterQueryString);
//    conceptsQParams.set(CommonParams.ROWS, params.hidden_max_hits);
//    
//    // add target extract field
//    conceptsQParams.set("fl", new String[]{"score","title","anchor","seealso","seealso_ngrams"});
//    QueryResponse conceptsQResp;
//    try {
//      conceptsQResp = server.query(conceptsQParams);
//    
//      // loop on results and add to concepts
//      SolrDocumentList results = conceptsQResp.getResults();
//      if(results!=null && results.size()>0) {
//        int cur_id = 1;
//        int cur_parent_id = 0;
//        
//        for(int i=0; i<results.size(); i++) {
//          SolrDocument doc = results.get(i);
//          boolean relevant = false;
//          String title = (String) doc.getFieldValue("title");
//          float score = ((Float) doc.getFieldValue("score")).floatValue();
//          String ner = "";
//          /*HEREHERE*///String ner = indexReader.document(hits[i].doc).getField("title_ne").stringValue();/*HEREHERE*/
//          CachedConceptInfo cachedInfo = null;
//          CachedAssociationInfo cachedAssoInfo = null;
//          
//          //System.out.println(title.stringValue());
//          // check if relevant concept
//          boolean relevantTitle = true;//TODO: do we need to call isRelevantConcept(f.stringValue());
//          if(relevantTitle==true) {
//            relevant = true;            
//            // check if already there
//            SemanticConcept sem = relatedConcepts.get(title);
//            if(sem==null) { // new concept              
//              cachedInfo = cachedConceptsInfo.get(title);
//              if(cachedInfo==null) {
//                //if(params.debug==true)
//                System.out.println(title+"...title not found!");
//                if(params.hidden_relax_cache==true)
//                  cachedInfo = new CachedConceptInfo(title, 0, "", "", "", 0);
//              }
//              else if(title.compareTo(cachedInfo.title)!=0){
//            	  System.out.println("Oops! unexpected ("+title+"<->"+cachedInfo.title+")");
//            	  title = cachedInfo.title;
//              }
//              if(cachedInfo!=null) {
//            	  sem = new SemanticConcept(title, cachedInfo, ner, score,
//            				  cur_id, 0, 0, Enums.ENUM_CONCEPT_TYPE.e_TITLE, cachedInfo.pagerank);            		  
//            	  cur_parent_id = cur_id;
//            	  cur_id++;
//            	  
//                // get its associations
//                if(params.e_Method!=Enums.ENUM_SEMANTIC_METHOD.e_MSA && 
//                    params.e_Method!=Enums.ENUM_SEMANTIC_METHOD.e_MSA_ANCHORS) {
//                  Integer I = titleIntMapping.get(title);
//                  if(I!=null) {
//                    cachedAssoInfo = cachedAssociationsInfo.get(I);
//                  }
//                  else {
//                	  //if(params.debug==true)
//                	  System.out.println(title+"...title not in mappings!");
//                  }
//                }                              
//              }
//            }
//            else { // existing concept, update its weight to higher weight
//              cachedInfo = sem.cachedInfo;
//              sem.weight = sem.weight>score?sem.weight:score;
//              cur_parent_id = sem.id;
//            }
//            if(sem!=null)
//              relatedConcepts.put(sem.name, sem);            
//          }
//          else {
//        	  if(params.debug==true)
//        		  System.out.println(title+"...title not relevant!");
//          }
//          if(relevant==true && (params.e_Method==Enums.ENUM_SEMANTIC_METHOD.e_MSA_ANCHORS || 
//                  params.e_Method==Enums.ENUM_SEMANTIC_METHOD.e_MSA_ANCHORS_SEE_ALSO || 
//                      params.e_Method==Enums.ENUM_SEMANTIC_METHOD.e_MSA_ANCHORS_SEE_ALSO_ASSO)) // retrieve anchors
//          {
//            relevant = false;
//            Collection<Object> anchorValues = doc.getFieldValues("anchor");
//            if(anchorValues!=null) {
//              Object[] anchors = anchorValues.toArray();
//              for(int t=0; t<anchors.length; t++) {
//                String anchor = (String) anchors[t];
//                //System.out.println(f.stringValue());
//                // check if relevant concept
//                relevantTitle = true;//TODO: do we need to call isRelevantConcept(f.stringValue());
//                if(relevantTitle==true) {
//                  relevant = true;
//                  
//                  // check if already there
//                  SemanticConcept sem = relatedConcepts.get(anchor);
//                  if(sem==null) { // new concept                  
//                		  sem = new SemanticConcept(anchor, cachedInfo, ner, score-0.0001f,
//                				  cur_id, cur_parent_id, 0, Enums.ENUM_CONCEPT_TYPE.e_ANCHOR, cachedInfo.pagerank);
//                    cur_id++;
//                  }
//                  else { // existing concept, update its weight to higher weight
//                    cachedInfo = sem.cachedInfo;
//                    sem.weight = sem.weight>score-0.0001f?sem.weight:score-0.0001f;
//                  }
//                  if(sem!=null)
//                    relatedConcepts.put(sem.name, sem);                
//                }
//                else {
//                	if(params.debug==true)
//                		System.out.println(anchor+"...title not relevant!");
//                }
//              }
//            }
//          }
//          
//          //System.out.println();
//          if(params.hidden_relax_see_also==false || relevant) {
//            // force see also is enabled OR,
//            // the original title or one of its anchors is relevant
//            // in this case we can add its see_also
//            if(params.e_Method==Enums.ENUM_SEMANTIC_METHOD.e_MSA_SEE_ALSO || 
//                params.e_Method==Enums.ENUM_SEMANTIC_METHOD.e_MSA_ANCHORS_SEE_ALSO) { // add See also to the hit list
//              
//              Collection<Object> seeAlsoValues = doc.getFieldValues("seealso");
//              Collection<Object> seeAlsoNgramsValues = doc.getFieldValues("seealso_ngrams");
//              if(seeAlsoValues!=null) {
//            	  if(seeAlsoNgramsValues!=null) {
//	                Object[] multiSeeAlso = seeAlsoValues.toArray();
//	                /*HEREHERE*//*Object[] multiSeeAlsoNE = doc.getFieldValues("see_also_ne").toArray();*//*HEREHERE*/
//	                Object[] multiSeeAlsoNgrams = seeAlsoNgramsValues.toArray();              
//	                
//	                int min = Math.min(multiSeeAlsoNgrams.length,multiSeeAlso.length);
//	                for(int s=0; s<min; s++) {
//	                  //System.out.println(f.stringValue());
//	                  // check if relevant concept
//	                  relevantTitle = true; //TODO: do we need to call isRelevantConcept(multiSeeAlso[s].stringValue());
//	                  /*String re = "\\S+(\\s\\S+){0,"+String.valueOf(params.hidden_max_seealso_ngrams-1)+"}";
//	                  if(multiSeeAlso[s].stringValue().toLowerCase().matches(re)==false)
//	                    relevantTitle = false;
//	                  */
//	                  if(((Integer)multiSeeAlsoNgrams[s]).intValue()>params.hidden_max_seealso_ngrams)
//	                    relevantTitle = false;
//	                  
//	                  if(relevantTitle==true) {
//	                	String seealso = (String)multiSeeAlso[s];
//	                	cachedInfo = cachedConceptsInfo.get(seealso);
//	                    if(cachedInfo==null) {
//	                    	if(params.debug==true)
//	                    		System.out.println(seealso+"...see_also not found!");
//	                    	if(params.hidden_relax_cache==true)
//	                    		cachedInfo = new CachedConceptInfo(seealso, 0, "", "", "");
//	                    }
//	                    else
//	                    	seealso = cachedInfo.title;  // might be redirect so get original title
//	                    
//	                	// check if already there
//	                    SemanticConcept sem = relatedConcepts.get(seealso); 
//	                    if(sem==null) { // new concept
//	                      if(cachedInfo!=null) {  
//	                        // get see also association info
//	                        int asso_cnt = 0;
//	                        if(cachedAssoInfo!=null) {
//	                          Integer I = titleIntMapping.get(seealso);
//	                          if(I!=null) {
//	                            for(Integer[] info :cachedAssoInfo.associations) {
//	                              if(info[0].intValue()==I.intValue()) {
//	                                asso_cnt = info[1].intValue();
//	                                break;
//	                              }
//	                            }
//	                            if(asso_cnt==0) {
//	                            	if(params.debug==true)
//	                            		System.out.println(seealso+"...see_also not in associations!");
//	                            }
//	                          }
//	                          else {
//	                        	  if(params.debug==true)
//	                        		  System.out.println(seealso+"...see_also not in mappings!");
//	                          }
//	                        }
//	                        if((cachedInfo.length==0 || cachedInfo.length>=params.hidden_min_seealso_length) && 
//	                            (asso_cnt==0 || asso_cnt>=params.hidden_min_asso_cnt)) { // support > minimum support
//	                          sem = new SemanticConcept(seealso, 
//	                              cachedInfo, ""/*HEREHERE*//*(String)multiSeeAlsoNE[s]*//*HEREHERE*/, 
//	                              score-0.0002f, cur_id, cur_parent_id, asso_cnt, Enums.ENUM_CONCEPT_TYPE.e_SEE_ALSO);
//	                          cur_id++;                      
//	                        }
//	                      }
//	                    }
//	                    else { // existing concept, update its weight to higher weight
//	                      cachedInfo = sem.cachedInfo;
//	                      sem.weight = sem.weight>score-0.0002f?sem.weight:score-0.0002f;
//	                    }
//	                    if(sem!=null)
//	                      relatedConcepts.put(sem.name, sem);            
//	                  }
//	                  //else
//	                    //System.out.println(multiSeeAlso[s].stringValue()+"...see-also not relevant!");
//	                  //System.out.println();
//	                }
//              	}
//            	else {
//            		System.out.println("Oops! unexpected null seealso ngrams for ("+title+"<->"+(String) doc.getFieldValue("title")+")");
//            	}
//              }
//            }
//            else if(params.e_Method==Enums.ENUM_SEMANTIC_METHOD.e_MSA_SEE_ALSO_ASSO || 
//                params.e_Method==Enums.ENUM_SEMANTIC_METHOD.e_MSA_ANCHORS_SEE_ALSO_ASSO) { // add see also using association mining
//              Integer key = titleIntMapping.get(title);
//              CachedAssociationInfo assoInfo = cachedAssociationsInfo.get(key);
//              if(assoInfo==null) {
//            	  if(params.debug==true)
//            		  System.out.println(title + "..no associations cached!");
//              }
//              else {
//                for(int a=0; a<assoInfo.associations.size(); a++) {
//                  Integer[] assos = assoInfo.associations.get(a);
//                  if(assos[1]>=params.hidden_min_asso_cnt) // support > minimum support
//                  {
//                    String assoStr = titleStrMapping.get(assos[0]);
//                    
//                    // check if relevant concept
//                    relevantTitle = true; //TODO: do we need to call isRelevantConcept(multiSeeAlso[s].stringValue());
//                    if(getTitleLength(assoStr)>params.hidden_max_seealso_ngrams)
//                      relevantTitle = false;
//                    if(relevantTitle==true) {
//                      // check if already there
//                      SemanticConcept sem = relatedConcepts.get(assoStr); 
//                      if(sem==null) { // new concept
//                        cachedInfo = cachedConceptsInfo.get(assoStr);
//                        if(cachedInfo==null) {
//                        	if(params.debug==true)
//                        		System.out.println(assoStr+"...see_also not found!");
//                          if(params.hidden_relax_cache==true)
//                            cachedInfo = new CachedConceptInfo(assoStr, 0, "", "", "", pagerank);
//                        }
//                        if(cachedInfo!=null)
//                        {  
//                          if(cachedInfo.length==0 || cachedInfo.length>=params.hidden_min_seealso_length) { 
//                              sem = new SemanticConcept(assoStr, 
//                          
//                                cachedInfo, "M", 
//                              score-0.0002f, cur_id, cur_parent_id, assos[1], Enums.ENUM_CONCEPT_TYPE.e_SEE_ALSO);
//                            cur_id++;
//                          }
//                        }
//                      }
//                      else { // existing concept, update its weight to higher weight
//                        cachedInfo = sem.cachedInfo;
//                        sem.weight = sem.weight>score-0.0002f?sem.weight:score-0.0002f;
//                      }
//                      if(sem!=null)
//                        relatedConcepts.put(sem.name, sem);
//                    }
//                    //else
//                      //System.out.println(assoStr+"...see-also not relevant!");
//                  }
//                  //else
//                    //System.out.println(assos[0]+"...see-also below threshold!");
//                }
//              }
//            }
//          }
//        }
//      }
//      else {
//    	  if(params.debug==true)
//    		  System.out.println("No semantic results found :(");
//      }
//    } catch (SolrServerException e) {
//    	System.out.println("Error executing query: "+queryString);
//    	e.printStackTrace();      
//    }
//  }
  
  public int getTitleLength(String title) {
      // TODO Auto-generated method stub
      int index = title.length(), index1, index2;
      if((index1=title.indexOf(','))==-1)
          index1 = index;
      if((index2=title.indexOf('('))==-1)
          index2 = index;
      
      index = Math.min(index1, index2);
      String s[] = title.substring(0,index).split(" ");
      return s.length;    
  }

  /*
   * remove concepts that are irrelevant (only 1-2-3 word phrases are allowed)
   * @param relatedConcepts related concepts to be filtered
   */
  public void filterRelatedConcepts(HashMap<String,SemanticConcept> relatedConcepts, 
      SemanticSearchConfigParams params) {
    ArrayList<String> toRemove = new ArrayList<String>();
    for (SemanticConcept concept : relatedConcepts.values()) {
      if(isRelevantConcept(params.hidden_relax_listof,params.hidden_relax_disambig,params.hidden_max_title_ngrams,concept.name)==false) {
    	  if(params.debug==true)
    		  System.out.println(concept.name+"("+concept.id+")...Removed!");
        toRemove.add(concept.name);
      }
      else if(params.hidden_relax_ner==false && ArrayUtils.contains(new String[]{"P","L","O"},concept.ner)) { // check if not allowed NE
    	  if(params.debug==true)
    		  System.out.println(concept.name+"("+concept.id+")...Removed (NER)!");
        toRemove.add(concept.name);
      }
      else if(params.hidden_relax_categories==false) { // check if not allowed category
        for(int i=0; i<concept.cachedInfo.category.length; i++) {
          String c = concept.cachedInfo.category[i].toLowerCase();
          if(c.contains("companies") || 
              c.contains("manufacturers") || 
              c.contains("publishers") || 
              c.contains("births") || 
              c.contains("deaths") || 
              c.contains("anime") || 
              c.contains("movies") || 
              c.contains("theaters") || 
              c.contains("games") ||
              c.contains("channels")) {
        	  if(params.debug==true)
        		  System.out.println(concept.name+"("+concept.id+")...Removed (CATEGORY)!");
            toRemove.add(concept.name);
            break;
          }
        }
      }      
    }
    
    for(String concept : toRemove) {
      relatedConcepts.remove(concept);
    }
  }
  
  /*
   * remove concepts that are irrelevant (only 1-2-3 word phrases are allowed)
   * @param concept concept to be evaluated
   */
  protected boolean isRelevantConcept(boolean hidden_relax_listof, boolean hidden_relax_disambig,  
      int hidden_max_title_ngrams, String concept) {
    //TODO: 
    /* handle list of: (List of solar powered products),(List of types of solar cells)
     * handle File: (File:Jason Robinson.jpg)
     * handle names, places, adjectives
     * remove titles with (disambiguation)
     * keep only titles in technical dictionary
     */
    boolean relevant = true;
    String re;
    /*String re = "\\S+(\\s\\S+){0,"+String.valueOf(hidden_max_title_ngrams-1)+"}";
    if(concept.toLowerCase().matches(re)==false) {
      relevant = false;
    }*/
    if(relevant==true && hidden_relax_listof==false) {
      re = "list of.*";
      if(concept.toLowerCase().matches(re)==true) {
        relevant = false;
      }
    }
    if(relevant==true && hidden_relax_disambig==false) {
      re = ".*\\(disambiguation\\)";
      if(concept.toLowerCase().matches(re)==true) {
        relevant = false;
      }    
    }
    return relevant; 
  }
  
  public NamedList<Object> doSemanticSearch(String concept, SemanticSearchConfigParams params) {
    NamedList<Object> semanticConceptsInfo = new SimpleOrderedMap<Object>();//semanticConceptsInfo = null;
    //HashMap<String,SemanticConcept> relatedConcepts = null;
    if(params.concepts_num>0) {
	    // retrieve related semantic concepts      
	    HashMap<String,SemanticConcept> relatedConcepts = new HashMap<String,SemanticConcept>();
	    retrieveRelatedConcepts(concept, relatedConcepts, params);
	    
	    // remove irrelevant concepts
	    if(params.hidden_relax_filters==false)
	    	filterRelatedConcepts(relatedConcepts, params);
	    
	    if(relatedConcepts.size()>0) {        
	      // sort concepts
	      SemanticConcept sem[] = new SemanticConcept[relatedConcepts.size()];
	      sem = (SemanticConcept[])relatedConcepts.values().toArray(sem);
	      Arrays.sort(sem, Collections.reverseOrder());
	      
	      // add concepts to response
	      for(int i=0,j=0; i<params.concepts_num && i<sem.length && j<sem.length; j++) {
	        // remove a concept that exactly match original concept
	        if(params.hidden_relax_same_title==false && concept.compareToIgnoreCase(sem[j].name)==0) {
	        	if(params.debug==true)
	        		System.out.println(sem[j].name+"...Removed!");
	          continue;
	        }
	        if(params.ignored_concepts!=null && params.ignored_concepts.contains(sem[j].name)) {
	          // set ignored flag
	          sem[j].ignore = 1;
	        }
	        SimpleOrderedMap<Object> conceptInfo = sem[j].getInfo();
	        //semanticConceptsInfo.add(sem[j].name, sem[j].weight);
	        semanticConceptsInfo.add(sem[j].name, conceptInfo);
	        if(!params.abs_explicit || sem[j].e_concept_type==Enums.ENUM_CONCEPT_TYPE.e_TITLE)
	            i++;
	      }
	      
	      // add related concepts to the query
	      //if(params.hidden_relax_search==false)
	      //  qparams.set(CommonParams.Q, newQuery);
	      
	      //req.setParams(qparams);
	    }
  	}
    return semanticConceptsInfo;
  }
  
  public NamedList<Object> doSemanticRelatedness(String sentence1, String sentence2, SemanticSearchConfigParams params) {
    NamedList<Object> semanticConceptsInfo = null;
    
    if(params.hidden_relatedness_experiment==true) {
      // open concepts file and loop on concepts
      BufferedReader in = null;
      FileWriter out = null;
      try {
        in = new BufferedReader(new FileReader(params.in_path));
        out = new FileWriter(params.out_path);
        if(in!=null && out!=null) {
          String line;        
          line = in.readLine();
          while(line!=null) {
            // expected format (concept1,concept2)
            String[] concepts = line.split(",");
            
            // calculating similarity between each two concepts
            HashMap<String,SemanticConcept> relatedConcepts1 = new HashMap<String,SemanticConcept>();
            HashMap<String,SemanticConcept> relatedConcepts2 = new HashMap<String,SemanticConcept>();
            
            out.write(concepts[0]);
            for(int i=1; i<concepts.length; i++) {
              double similarity = getRelatedness(concepts[0], concepts[i], 
                  relatedConcepts1, relatedConcepts2, params);
              
              out.write(","+concepts[i]+","+similarity);
              relatedConcepts2.clear();
            }
            out.write("\n");
            out.flush();
            
            line = in.readLine();
            }
          in.close();
          out.close();
        }
      } catch (IOException e) {
        e.printStackTrace();
        if(in!=null)
			try {
				in.close();
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
        if(in!=null)
			try {
				out.close();
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}        
      }
    }
    else {
      if(sentence1.length()>0 && sentence2.length()>0) {
        HashMap<String,SemanticConcept> relatedConcepts1 = new HashMap<String,SemanticConcept>();
        HashMap<String,SemanticConcept> relatedConcepts2 = new HashMap<String,SemanticConcept>();
        
        double relatedness = getRelatedness(sentence1, sentence2, 
            relatedConcepts1, relatedConcepts2, params);
        
        semanticConceptsInfo = new SimpleOrderedMap<Object>();
        
        if(params.debug==true)
        	System.out.println("score: "+relatedness);
        relatedness = mapRelatednessScore(relatedness);        
        
        semanticConceptsInfo.add("relatedness", String.format("%.2f", relatedness));
      
        
        if(relatedConcepts1.size()>0 || relatedConcepts2.size()>0) {          
          // add concepts related to concept 1 to response
          SimpleOrderedMap<Object> conceptInfo1 = new SimpleOrderedMap<Object>();
          int ind=0;
          for(SemanticConcept c : relatedConcepts1.values()) {
            SimpleOrderedMap<Object> conceptInfo = c.getInfo();
            conceptInfo1.add(c.name, conceptInfo);
            ind++;
            if(ind==params.concepts_num || ind==relatedConcepts1.size())
              break;
          }
          semanticConceptsInfo.add(sentence1, conceptInfo1);
          
          // add concepts related to concept 2 to response
          SimpleOrderedMap<Object> conceptInfo2 = new SimpleOrderedMap<Object>();
          ind=0;
          for(SemanticConcept c : relatedConcepts2.values()) {
            SimpleOrderedMap<Object> conceptInfo = c.getInfo();
            conceptInfo2.add(c.name, conceptInfo);
            ind++;
            if(ind==params.concepts_num || ind==relatedConcepts2.size())
              break;
          }
          semanticConceptsInfo.add(sentence2, conceptInfo2);
        }
      }
      else { // invalid format (expected concept1,concept2)
        
      }
    }
    return semanticConceptsInfo;
  }
  
  public double mapRelatednessScore(double score) {
    double relatedness = score;
    double max = 0;
    double min = 0;
    double rmin = 0;
    double rmax = 0;
    if(relatedness>=0.35) { // [3.5-4]
      rmin = 0.35;
      rmax = 1;
      min = 3.5;
      max = 4;
    }
    else if(relatedness>=0.2) { // [2-3.5]
      rmin = 0.2;
      rmax = 0.349999999;
      min = 2;
      max = 3.5;
    }
    else if(relatedness>=0.01) { // [1-2]
      rmin = 0.01;
      rmax = 0.1999999999;
      min = 1;
      max = 2;
    }
    else { // [0-1]
      rmin = 0.0;
      rmax = 0.00999999999;
      min = 0;
      max = 1;
    }
    return 1+min+(max-min)*(relatedness-rmin)/(rmax-rmin);
  }

  public double getRelatedness(String concept1, String concept2, 
      HashMap<String,SemanticConcept> relatedConcepts1, 
      HashMap<String,SemanticConcept> relatedConcepts2, 
      SemanticSearchConfigParams params) {
    double similarity = 0;
    
    // retrieve related semantic concepts for concept 1
    if(params.debug==true)
    	System.out.println("Retrieving for concept: ("+concept1+")");
    retrieveRelatedConcepts(concept1, relatedConcepts1, params);
    
    // remove irrelevant concepts
    filterRelatedConcepts(relatedConcepts1, params);
    
    // retrieve related semantic concepts for concept 2
    if(params.debug==true)
    	System.out.println("Retrieving for concept: ("+concept2+")");
    retrieveRelatedConcepts(concept2, relatedConcepts2, params);
    
    // remove irrelevant concepts
    filterRelatedConcepts(relatedConcepts2, params);
    
    if(relatedConcepts1.size()>0 || relatedConcepts2.size()>0) {
      // keep only required number of concepts
      SemanticConcept[] sem = new SemanticConcept[relatedConcepts1.size()];
      sem = (SemanticConcept[])relatedConcepts1.values().toArray(sem);
      Arrays.sort(sem, Collections.reverseOrder());
      relatedConcepts1.clear();
      for(int i=0,j=0; i<sem.length && j<params.concepts_num; i++) {
        relatedConcepts1.put(sem[i].name, sem[i]);
        if(sem[i].e_concept_type==Enums.ENUM_CONCEPT_TYPE.e_TITLE)
          j++;
      }
      
      sem = new SemanticConcept[relatedConcepts2.size()];
      sem = (SemanticConcept[])relatedConcepts2.values().toArray(sem);
      Arrays.sort(sem, Collections.reverseOrder());
      relatedConcepts2.clear();
      for(int i=0,j=0; i<sem.length && j<params.concepts_num; i++) {
        relatedConcepts2.put(sem[i].name, sem[i]);
        if(sem[i].e_concept_type==Enums.ENUM_CONCEPT_TYPE.e_TITLE)
          j++;
      }
      if(params.e_Distance==Enums.ENUM_DISTANCE_METRIC.e_COSINE || 
          params.e_Distance==Enums.ENUM_DISTANCE_METRIC.e_COSINE_BIN || 
          params.e_Distance==Enums.ENUM_DISTANCE_METRIC.e_COSINE_NORM) {
        similarity = cosineSim(relatedConcepts1, relatedConcepts2, params);
      }
      else if(params.e_Distance==Enums.ENUM_DISTANCE_METRIC.e_Euclidean) {
        similarity = 1.0/(1.0+euclideanDist(relatedConcepts1, relatedConcepts2, params));        
      }
      else if(params.e_Distance==Enums.ENUM_DISTANCE_METRIC.e_WO) {
        try {
          similarity = WeightedOverlapSim(relatedConcepts1, relatedConcepts2, params);
        } catch (Exception e) {
          e.printStackTrace();
        }
      }
    }
    return similarity;
  }
  
  public double euclideanDist(
      HashMap<String,SemanticConcept> relatedConcepts1,
      HashMap<String,SemanticConcept> relatedConcepts2, SemanticSearchConfigParams params) {
    double distance = 0;
    // normalize both concepts vectors
    ArrayList<SemanticConcept> toAdd = new ArrayList<SemanticConcept>(); 
    // add concepts appearing in concept 1 and not concept 2
    for(String s : relatedConcepts1.keySet()) {
      if(relatedConcepts2.get(s)==null) {
        SemanticConcept c = new SemanticConcept(relatedConcepts1.get(s));
        c.weight = 0;
        toAdd.add(c);            
      }
    }
    for(int i=0; i<toAdd.size(); i++)
      relatedConcepts2.put(toAdd.get(i).name, toAdd.get(i));
    
    toAdd.clear();
    
    // add concepts appearing in concept 2 and not concept 1
    for(String s : relatedConcepts2.keySet()) {
      if(relatedConcepts1.get(s)==null) {
        SemanticConcept c = new SemanticConcept(relatedConcepts2.get(s));
        c.weight = 0;
        toAdd.add(c);
      }
    }
    for(int i=0; i<toAdd.size(); i++)
      relatedConcepts1.put(toAdd.get(i).name, toAdd.get(i));
    
    toAdd.clear();
    
    // calculate euclidean similarity
    for(SemanticConcept c1 : relatedConcepts1.values()) {
      SemanticConcept c2 = relatedConcepts2.get(c1.name);
      distance += Math.pow(c1.weight-c2.weight, 2.0);      
    }
    distance = Math.sqrt(distance);
    
    return distance;
  }

  public double WeightedOverlapSim(
      HashMap<String,SemanticConcept> relatedConcepts1,
      HashMap<String,SemanticConcept> relatedConcepts2, SemanticSearchConfigParams params) throws Exception {
    
    // sort the two vectors by weight
    SemanticConcept concepts1[] = new SemanticConcept[relatedConcepts1.size()];
    concepts1 = (SemanticConcept[])relatedConcepts1.values().toArray(concepts1);
    Arrays.sort(concepts1, Collections.reverseOrder());
    
    SemanticConcept concepts2[] = new SemanticConcept[relatedConcepts2.size()];
    concepts2 = (SemanticConcept[])relatedConcepts2.values().toArray(concepts2);
    Arrays.sort(concepts2, Collections.reverseOrder());
    
    // get overlap between two vectors
    Set<String> overlap = relatedConcepts1.keySet();
    overlap.retainAll(relatedConcepts2.keySet());
    if(params.debug==true)
    	System.out.println("common concepts: ("+String.valueOf(overlap.size())+")");
    
    int[] ranks1 = new int[overlap.size()];
    int[] ranks2 = new int[overlap.size()];
    int pos = 0;
    for(String s : overlap) {
      // get rank in first vector
      for(int i=0; i<concepts1.length; i++)
        if(s.compareTo(concepts1[i].name)==0) {
          ranks1[pos] = i+1;
          break;
        }
      
      // get rank in second vector
      for(int i=0; i<concepts2.length; i++)
        if(s.compareTo(concepts2[i].name)==0) {
          ranks2[pos] = i+1;
          break;
        }
      
      pos++;
    }
    if(pos<overlap.size())
      throw new Exception("overlap mismatch!");
    
    double num = 0.0, den = 0.0;
    for(int i=0; i<ranks1.length; i++) {
      num += 1.0/(ranks1[i]+ranks2[i]);
      den += 1.0/(2*(Math.min(ranks1[i],ranks2[i])));
    }
    return (num/den); 
  }

  public double cosineSim(HashMap<String,SemanticConcept> relatedConcepts1, 
      HashMap<String,SemanticConcept> relatedConcepts2, SemanticSearchConfigParams params) {
    double similarity=0, norm1=0, norm2=0, dot_product=0;
    if(params.e_Distance==Enums.ENUM_DISTANCE_METRIC.e_COSINE || 
        params.e_Distance==Enums.ENUM_DISTANCE_METRIC.e_COSINE_NORM) {

      if(params.e_Distance==Enums.ENUM_DISTANCE_METRIC.e_COSINE_NORM) { // normalize weights
        float maxWeight = Collections.max(relatedConcepts1.values()).weight;
        for(String s : relatedConcepts1.keySet()) {
          SemanticConcept sem = relatedConcepts1.get(s);
          sem.weight = sem.weight/maxWeight;
        }
         
        maxWeight = Collections.max(relatedConcepts2.values()).weight;
        for(String s : relatedConcepts2.keySet()) {
          SemanticConcept sem = relatedConcepts2.get(s);
          sem.weight = sem.weight/maxWeight;
        }
      }
      
      // normalize both concepts vectors
      ArrayList<SemanticConcept> toAdd = new ArrayList<SemanticConcept>(); 
      // add concepts appearing in concept 1 and not concept 2
      for(String s : relatedConcepts1.keySet()) {
        if(relatedConcepts2.get(s)==null) {
          SemanticConcept c = new SemanticConcept(relatedConcepts1.get(s));
          c.weight = 0;
          toAdd.add(c);            
        }
      }
      for(int i=0; i<toAdd.size(); i++)
        relatedConcepts2.put(toAdd.get(i).name, toAdd.get(i));
      
      toAdd.clear();
      
      // add concepts appearing in concept 2 and not concept 1
      for(String s : relatedConcepts2.keySet()) {
        if(relatedConcepts1.get(s)==null) {
          SemanticConcept c = new SemanticConcept(relatedConcepts2.get(s));
          c.weight = 0;
          toAdd.add(c);
        }
      }
      for(int i=0; i<toAdd.size(); i++)
        relatedConcepts1.put(toAdd.get(i).name, toAdd.get(i));
      
      toAdd.clear();
      
      // calculate cosine similarity
      for(SemanticConcept c1 : relatedConcepts1.values()) {
        SemanticConcept c2 = relatedConcepts2.get(c1.name);
        dot_product += c1.weight*c2.weight;
        norm1 += Math.pow(c1.weight, 2.0);
        norm2 += Math.pow(c2.weight, 2.0);
      }
      similarity = dot_product/(Math.sqrt(norm1)*Math.sqrt(norm2));
    }
    else if(params.e_Distance==Enums.ENUM_DISTANCE_METRIC.e_COSINE_BIN) {
      // calculate cosine similarity
      for(SemanticConcept c1 : relatedConcepts1.values()) {
        SemanticConcept c2 = relatedConcepts2.get(c1.name);
        if(c2!=null) {
          dot_product += 1;
        }
      }
      norm1 = relatedConcepts1.size();
      norm2 = relatedConcepts2.size();
      similarity = dot_product/(Math.sqrt(norm1)*Math.sqrt(norm2));
    }
    return similarity;
  }
}
