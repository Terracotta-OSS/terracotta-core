/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package org.terracotta.dso.views;

public class ExcludesWrapper {
  InstrumentedClassesWrapper m_instrumentedClasses;
  
  ExcludesWrapper(InstrumentedClassesWrapper instrumentedClasses) {
    m_instrumentedClasses = instrumentedClasses;
  }
  
  ExcludeWrapper[] createExcludeWrappers() {
    int count = m_instrumentedClasses.sizeOfExcludeArray();
    ExcludeWrapper[] result = new ExcludeWrapper[count];
    
    for(int i = 0; i < count; i++) {
      result[i] = new ExcludeWrapper(m_instrumentedClasses, i);
    }
    
    return result;
  }
  
  int sizeOfExcludeArray() {
    return m_instrumentedClasses.sizeOfExcludeArray();
  }
  
  public String toString() {
    return "Excludes";
  }
}
