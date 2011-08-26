/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package org.terracotta.dso.views;

import com.terracottatech.config.DsoApplication;
import com.terracottatech.config.Include;
import com.terracottatech.config.InstrumentedClasses;

public class InstrumentedClassesWrapper {
  private DsoApplication fApp;
  
  InstrumentedClassesWrapper(DsoApplication dsoApp) {
    fApp = dsoApp;
  }
  
  int sizeOfIncludeArray() {
    InstrumentedClasses ic = fApp.getInstrumentedClasses();
    return ic != null ? ic.sizeOfIncludeArray() : 0;
  }
  
  Include getIncludeArray(int i) {
    InstrumentedClasses ic = fApp.getInstrumentedClasses();
    return ic != null ? ic.getIncludeArray(i) : null;
  }
  
  void removeInclude(int i) {
    InstrumentedClasses ic = fApp.getInstrumentedClasses();
    if(ic != null) {
      ic.removeInclude(i);
    }
    testRemove();
  }
  
  int sizeOfExcludeArray() {
    InstrumentedClasses ic = fApp.getInstrumentedClasses();
    return ic != null ? ic.sizeOfExcludeArray() : 0;
  }
  
  String getExcludeArray(int i) {
    InstrumentedClasses ic = fApp.getInstrumentedClasses();
    return ic != null ? ic.getExcludeArray(i) : null;
  }
  
  void removeExclude(int i) {
    InstrumentedClasses ic = fApp.getInstrumentedClasses();
    if(ic != null) {
      ic.removeExclude(i);
    }
    testRemove();
  }
  
  private void testRemove() {
    InstrumentedClasses ic = fApp.getInstrumentedClasses();
    if(ic != null) {
      if(ic.sizeOfExcludeArray() == 0 && ic.sizeOfIncludeArray() == 0) {
        fApp.unsetInstrumentedClasses();
      }
    }
  }
}
