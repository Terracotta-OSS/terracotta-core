/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package org.terracotta.dso.views;

public class IncludesWrapper {
  InstrumentedClassesWrapper m_instrumentedClasses;
  
  IncludesWrapper(InstrumentedClassesWrapper instrumentedClasses) {
    m_instrumentedClasses = instrumentedClasses;
  }
  
  IncludeWrapper[] createIncludeWrappers() {
    int count = m_instrumentedClasses.sizeOfIncludeArray();
    IncludeWrapper[] result = new IncludeWrapper[count];
    
    for(int i = 0; i < count; i++) {
      result[i] = new IncludeWrapper(m_instrumentedClasses, i);
    }
    
    return result;
  }
  
  int sizeOfIncludeArray() {
    return m_instrumentedClasses.sizeOfIncludeArray();
  }
  
  public String toString() {
    return "Includes";
  }
}
