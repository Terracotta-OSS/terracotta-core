/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package org.terracotta.dso.views;

import com.terracottatech.config.DsoApplication;
import com.terracottatech.config.TransientFields;

public class TransientFieldsWrapper {
  private DsoApplication fApp;
  
  TransientFieldsWrapper(DsoApplication dsoApp) {
    fApp = dsoApp;
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
  }
}
