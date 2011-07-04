/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package org.terracotta.dso.views;

public class IncludesWrapper {
  InstrumentedClassesWrapper m_instrumentedClasses;
  IncludeWrapper[] children;
  
  IncludesWrapper(InstrumentedClassesWrapper instrumentedClasses) {
    m_instrumentedClasses = instrumentedClasses;
  }
  
  IncludeWrapper[] createIncludeWrappers() {
    int count = m_instrumentedClasses.sizeOfIncludeArray();
    children = new IncludeWrapper[count];
    
    for(int i = 0; i < count; i++) {
      children[i] = new IncludeWrapper(m_instrumentedClasses, i);
    }
    
    return children;
  }
  
  IncludeWrapper[] getChildren() {
    return children;
  }
  
  IncludeWrapper getChildAt(int index) {
    return children != null ? children[index] : null;
  }
  
  int sizeOfIncludeArray() {
    return m_instrumentedClasses.sizeOfIncludeArray();
  }
  
  public String toString() {
    return "Includes";
  }
}
