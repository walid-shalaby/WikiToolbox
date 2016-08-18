package wiki.toolbox.semantic;

public class Enums {
	public static enum ENUM_SEMANTIC_METHOD {
	  e_UNKNOWN, 
	  e_MSA,
	  e_MSA_ANCHORS,
	  e_MSA_SEE_ALSO,
	  e_MSA_SEE_ALSO_ASSO,
	  e_MSA_ANCHORS_SEE_ALSO, 
	  e_MSA_ANCHORS_SEE_ALSO_ASSO
	}

	public static Enums.ENUM_SEMANTIC_METHOD getSemanticMethod(String conceptMethod) {
	    if(conceptMethod.compareTo("Explicit")==0)
	      return Enums.ENUM_SEMANTIC_METHOD.e_MSA;
	    else if (conceptMethod.compareTo("MSA_anchors")==0)
	      return Enums.ENUM_SEMANTIC_METHOD.e_MSA_ANCHORS;
	    else if (conceptMethod.compareTo("MSA_seealso")==0)
	      return Enums.ENUM_SEMANTIC_METHOD.e_MSA_SEE_ALSO;
	    else if (conceptMethod.compareTo("MSA_anchors_seealso")==0)
	      return Enums.ENUM_SEMANTIC_METHOD.e_MSA_ANCHORS_SEE_ALSO;
	    else if (conceptMethod.compareTo("MSA_seealso_asso")==0)
	      return Enums.ENUM_SEMANTIC_METHOD.e_MSA_SEE_ALSO_ASSO;
	    else if (conceptMethod.compareTo("MSA_anchors_seealso_asso")==0)
	      return Enums.ENUM_SEMANTIC_METHOD.e_MSA_ANCHORS_SEE_ALSO_ASSO;    
	    else return Enums.ENUM_SEMANTIC_METHOD.e_UNKNOWN;
	}
	  
	public static enum ENUM_DISTANCE_METRIC {
	  e_COSINE, 
	  e_COSINE_BIN,
	  e_COSINE_NORM,
	  e_Euclidean,
	  e_WO, 
	  e_UNKNOWN
	}
	public static Enums.ENUM_DISTANCE_METRIC getDistanceMethod(String distancestr) {		
		if(distancestr.compareToIgnoreCase("cosine")==0)
	        return Enums.ENUM_DISTANCE_METRIC.e_COSINE;
	      else if(distancestr.compareToIgnoreCase("bin")==0)
	        return Enums.ENUM_DISTANCE_METRIC.e_COSINE_BIN;
	      else if(distancestr.compareToIgnoreCase("cosinenorm")==0)
	        return Enums.ENUM_DISTANCE_METRIC.e_COSINE_NORM;
	      else if(distancestr.compareToIgnoreCase("wo")==0)
	        return Enums.ENUM_DISTANCE_METRIC.e_WO;
	      else if(distancestr.compareToIgnoreCase("euclidean")==0)
	        return Enums.ENUM_DISTANCE_METRIC.e_Euclidean;
	      else
	    	  return Enums.ENUM_DISTANCE_METRIC.e_UNKNOWN;
	}
	public static enum ENUM_CONCEPT_TYPE {
	  e_UNKNOWN, 
	  e_TITLE,
	  e_ANCHOR,
	  e_SEE_ALSO,
	  e_ASSOCIATION
	}

	public static enum ENUM_ANALYTIC_TYPE {
	  e_UNKNOWN, 
	  e_SEARCH,
	  e_TECH_EXPLORE,
	  e_TECH_LANDSCAPE,
	  e_CI,
	  e_PRIOR,
	  e_RELATEDNESS,
	  e_RELEVANCY
	}
	
	public static enum ENUM_IPC_FILTER {
		  e_NONE, 
		  e_IPC_SECTION,
		  e_IPC_CLASS,
		  e_IPC_SUBCLASS,
		  e_IPC_GROUP,
		  e_IPC_SUBGROUP
		}
		
		public static String getConceptTypeString(ENUM_CONCEPT_TYPE e) {
	    if(e==ENUM_CONCEPT_TYPE.e_TITLE)
	      return "title";
	    else if(e==ENUM_CONCEPT_TYPE.e_ANCHOR)
	      return "anchor";
	    else if(e==ENUM_CONCEPT_TYPE.e_SEE_ALSO)
	      return "seealso";
	    else if(e==ENUM_CONCEPT_TYPE.e_ASSOCIATION)
	      return "association";
	    else return "unknown";
	}
}
