/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package org.terracotta.dso.views;

public class ExcludeWrapper {
  private InstrumentedClassesWrapper m_parent;
  private int m_index;
  
  ExcludeWrapper(InstrumentedClassesWrapper parent, int index) {
    m_parent = parent;
    m_index = index;
  }
  
  void remove() {
    m_parent.removeExclude(m_index);
  }
  
  InstrumentedClassesWrapper getParent() {
    return m_parent;
  }
  
  public String toString() {
    return m_parent.getExcludeArray(m_index);
  }
}
