package wiki.toolbox.semantic;

import org.apache.solr.common.util.SimpleOrderedMap;

public class SemanticConcept implements Comparable<SemanticConcept> {
  public CachedConceptInfo cachedInfo = null;
  public String name = "";
  public String ner = ""; // NE label ("P" --> person, "o" --> organization, "L" --> location, "M" --> misc)
  public float weight = 0.0f;
  public float weight2 = 0; // this is either tfidf score or pagerank
  public int id = 0;
  public int parent_id = 0;
  public int asso_cnt = 0;
  public Enums.ENUM_CONCEPT_TYPE e_concept_type = Enums.ENUM_CONCEPT_TYPE.e_UNKNOWN;
  public int ignore = 0;
  public String cats = ""; // categories
    
  public SemanticConcept() {    
  }
  
  public SemanticConcept(String name, CachedConceptInfo cachedInfo, String ne, String cats, float w, 
    int id, int parent_id, int asso, Enums.ENUM_CONCEPT_TYPE type, float w2) {
    this.name = name;
    this.cachedInfo = cachedInfo;
    ner = ne;
    weight = w;
    weight2 = w2;
    e_concept_type = type;
    this.id = id;
    this.parent_id = parent_id;
    this.asso_cnt = asso;
    this.cats = cats;
  }

  public SemanticConcept(SemanticConcept c) {
    this.name = c.name;
    this.cachedInfo = c.cachedInfo;
    ner = c.ner;
    weight = c.weight;
    weight2 = c.weight2;
    e_concept_type = c.e_concept_type;
    this.id = c.id;
    this.parent_id = c.parent_id;
    this.asso_cnt = c.asso_cnt;
    this.cats = cats;
  }

  @Override
  public int compareTo(SemanticConcept o) {
    if (((SemanticConcept)o).weight<this.weight)
      return 1;
    else if (((SemanticConcept)o).weight>this.weight)
      return -1;
    else {
       if (this.asso_cnt==0) {
         if(((SemanticConcept)o).asso_cnt==0)
           return 0;
         else 
           return 1;
       }
       else if (((SemanticConcept)o).asso_cnt==0)
        return -1;
      else if  (((SemanticConcept)o).asso_cnt<this.asso_cnt)
        return 1;
      else if (((SemanticConcept)o).asso_cnt>this.asso_cnt)
        return -1;
      else 
        return 0;
    }
  }

  public SimpleOrderedMap<Object> getInfo() {
    SimpleOrderedMap<Object> conceptInfo = new SimpleOrderedMap<Object>();
    conceptInfo.add("weight", weight);
    conceptInfo.add("weight2", weight2);
    conceptInfo.add("id", id);
    conceptInfo.add("p_id", parent_id);
    conceptInfo.add("asso_cnt", asso_cnt);
    conceptInfo.add("type", Enums.getConceptTypeString(e_concept_type));
    conceptInfo.add("docno", cachedInfo.docno);
    conceptInfo.add("length", cachedInfo.length);
    conceptInfo.add("ignore", ignore);
    conceptInfo.add("anchors", cachedInfo.anchors);
    conceptInfo.add("redirects", cachedInfo.redirects);
    conceptInfo.add("cats", cats);
    
    return conceptInfo;
  }
}

