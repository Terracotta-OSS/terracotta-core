/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package org.terracotta.dso.views;

import com.terracottatech.config.AdditionalBootJarClasses;
import com.terracottatech.config.DsoApplication;

public class AdditionalBootJarClassesWrapper {
  private DsoApplication fApp;
  private BootClassWrapper[] children;
  
  AdditionalBootJarClassesWrapper(DsoApplication dsoApp) {
    fApp = dsoApp;
  }
  
  BootClassWrapper[] createBootClassWrappers() {
    int count = sizeOfIncludeArray();

    children = new BootClassWrapper[count];
    for(int i = 0; i < count; i++) {
      children[i] = new BootClassWrapper(this, i);
    }
    
    return children;
  }
  
  BootClassWrapper[] getChildren() {
    return children;
  }
  
  BootClassWrapper getChildAt(int index) {
    return children != null ? children[index] : null;
  }
  
  int sizeOfIncludeArray() {
    AdditionalBootJarClasses abjc = fApp.getAdditionalBootJarClasses();
    return abjc != null ? abjc.sizeOfIncludeArray() : 0;
  }
  
  String getIncludeArray(int i) {
    AdditionalBootJarClasses abjc = fApp.getAdditionalBootJarClasses();
    return abjc != null ? abjc.getIncludeArray(i) : null;
  }
  
  void removeInclude(int i) {
    AdditionalBootJarClasses abjc = fApp.getAdditionalBootJarClasses();
    if(abjc != null) {
      abjc.removeInclude(i);
    }
    if(sizeOfIncludeArray() == 0) {
      fApp.unsetAdditionalBootJarClasses();
    }
  }
}
