package wiki.toolbox.semantic;

import java.util.HashSet;

public class CachedConceptInfo {
	  public String[] category; // first two categories of the title 
	  public String docno;
	  public int length;
	  public float pagerank;
	  public String title;
	  public HashSet<String> redirects;
	  public HashSet<String> anchors;
	  
	  public CachedConceptInfo() {
		  pagerank = 0.0f;
	  }
	  
	  public CachedConceptInfo(String title, int len, String no, String cat1, String cat2, float pr) {
	    docno = no;
	    length = len;
	    pagerank = pr;
	    category = new String[2];
	    category[0] = cat1;
	    category[1] = cat2;
	    this.title = title;	    
	    redirects = new HashSet<String>();
	    anchors = new HashSet<String>();
	  }
	}
