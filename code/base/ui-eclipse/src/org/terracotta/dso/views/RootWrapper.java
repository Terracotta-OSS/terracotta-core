/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package org.terracotta.dso.views;

public class RootWrapper {
  private RootsWrapper m_parent;
  private int m_index;
  
  RootWrapper(RootsWrapper parent, int index) {
    m_parent = parent;
    m_index = index;
  }
  
  RootsWrapper getParent() {
    return m_parent;
  }
  
  String getFieldName() {
    return m_parent.getRootArray(m_index).getFieldName();
  }
  
  void remove() {
    m_parent.removeRoot(m_index);
  }
  
  public String toString() {
    return getFieldName();
  }
}
