/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package org.terracotta.dso.views;

public class ExcludesWrapper {
  InstrumentedClassesWrapper m_instrumentedClasses;
  ExcludeWrapper[] children;
  
  ExcludesWrapper(InstrumentedClassesWrapper instrumentedClasses) {
    m_instrumentedClasses = instrumentedClasses;
  }
  
  ExcludeWrapper[] createExcludeWrappers() {
    int count = m_instrumentedClasses.sizeOfExcludeArray();
    children = new ExcludeWrapper[count];
    
    for(int i = 0; i < count; i++) {
      children[i] = new ExcludeWrapper(m_instrumentedClasses, i);
    }
    
    return children;
  }

  ExcludeWrapper[] getChildren() {
    return children;
  }
  
  ExcludeWrapper getChildAt(int index) {
    return children != null ? children[index] : null;
  }

  int sizeOfExcludeArray() {
    return m_instrumentedClasses.sizeOfExcludeArray();
  }
  
  public String toString() {
    return "Excludes";
  }
}
