/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package org.terracotta.dso.views;

import com.terracottatech.config.AdditionalBootJarClasses;
import com.terracottatech.config.DsoApplication;

public class AdditionalBootJarClassesWrapper {
  private DsoApplication fApp;
  
  AdditionalBootJarClassesWrapper(DsoApplication dsoApp) {
    fApp = dsoApp;
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
