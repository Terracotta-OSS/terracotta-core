/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
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
  
  boolean isSetFieldName() {
    return m_parent.getRootArray(m_index).isSetFieldName();
  }
  
  String getFieldName() {
    return m_parent.getRootArray(m_index).getFieldName();
  }
  
  boolean isSetFieldExpression() {
    return m_parent.getRootArray(m_index).isSetFieldExpression();
  }
  
  String getFieldExpression() {
    return m_parent.getRootArray(m_index).getFieldExpression();
  }
  
  void remove() {
    m_parent.removeRoot(m_index);
  }
  
  public String toString() {
    return isSetFieldName() ? getFieldName() : getFieldExpression();
  }
}
