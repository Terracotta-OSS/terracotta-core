/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package org.terracotta.dso.views;

import com.terracottatech.config.DsoApplication;
import com.terracottatech.config.TransientFields;

public class TransientFieldsWrapper {
  private DsoApplication fApp;
  private TransientFieldWrapper[] children;
  
  TransientFieldsWrapper(DsoApplication dsoApp) {
    fApp = dsoApp;
  }

  TransientFieldWrapper[] createTransientFieldWrappers() {
    int count = sizeOfFieldNameArray();
    children = new TransientFieldWrapper[count];
    
    for(int i = 0; i < count; i++) {
      children[i] = new TransientFieldWrapper(this, i);
    }
    
    return children;
  }

  TransientFieldWrapper[] getChildren() {
    return children;
  }
  
  TransientFieldWrapper getChildAt(int index) {
    return children != null ? children[index] : null;
  }
  
  int sizeOfFieldNameArray() {
    TransientFields transientFields = fApp.getTransientFields();
    return transientFields != null ? transientFields.sizeOfFieldNameArray() : 0;
  }
  
  String getFieldNameArray(int i) {
    TransientFields transientFields = fApp.getTransientFields();
    return transientFields != null ? transientFields.getFieldNameArray(i) : null;
  }
  
  void removeFieldName(int i) {
    TransientFields transientFields = fApp.getTransientFields();
    if(transientFields != null) {
      transientFields.removeFieldName(i);
    }
    if(sizeOfFieldNameArray() == 0) {
      fApp.unsetTransientFields();
    }
  }
}
