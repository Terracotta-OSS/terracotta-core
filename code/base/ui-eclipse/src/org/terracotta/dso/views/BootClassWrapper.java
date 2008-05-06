/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package org.terracotta.dso.views;

public class BootClassWrapper {
  private AdditionalBootJarClassesWrapper m_parent;
  private int m_index;
  
  BootClassWrapper(AdditionalBootJarClassesWrapper parent, int index) {
    m_parent = parent;
    m_index = index;
  }
  
  AdditionalBootJarClassesWrapper getParent() {
    return m_parent;
  }
  
  String getClassName() {
    return m_parent.getIncludeArray(m_index);
  }
  
  void remove() {
    m_parent.removeInclude(m_index);
  }
  
  public String toString() {
    return getClassName();
  }
}
