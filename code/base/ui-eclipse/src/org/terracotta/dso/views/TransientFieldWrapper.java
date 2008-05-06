/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package org.terracotta.dso.views;

public class TransientFieldWrapper {
  private TransientFieldsWrapper m_parent;
  private int m_index;
  
  TransientFieldWrapper(TransientFieldsWrapper parent, int index) {
    m_parent = parent;
    m_index = index;
  }
  
  TransientFieldsWrapper getParent() {
    return m_parent;
  }
  
  String getFieldName() {
    return m_parent.getFieldNameArray(m_index);
  }
  
  void remove() {
    m_parent.removeFieldName(m_index);
  }
  
  public String toString() {
    return getFieldName();
  }
}
