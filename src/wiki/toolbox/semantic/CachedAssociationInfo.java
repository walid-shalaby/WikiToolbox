package wiki.toolbox.semantic;

import java.util.ArrayList;

public class CachedAssociationInfo {
  public ArrayList<Integer[]> associations = null;
  public CachedAssociationInfo() {
    associations = new ArrayList<Integer[]>();
  }
  
  public void addAssociation(int idx, int c) {
    // each association is represented by its index and its count
    associations.add(new Integer[]{idx,c});
  }
}
