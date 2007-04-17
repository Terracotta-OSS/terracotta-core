/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package org.terracotta.dso.views;

import com.terracottatech.config.DistributedMethods;
import com.terracottatech.config.DsoApplication;

public class DistributedMethodsWrapper {
  private DsoApplication fApp;
  
  DistributedMethodsWrapper(DsoApplication dsoApp) {
    fApp = dsoApp;
  }
  
  int sizeOfMethodExpressionArray() {
    DistributedMethods distributedMethods = fApp.getDistributedMethods();
    return distributedMethods != null ? distributedMethods.sizeOfMethodExpressionArray() : 0;
  }
  
  String getMethodExpressionArray(int i) {
    DistributedMethods distributedMethods = fApp.getDistributedMethods();
    return distributedMethods != null ? distributedMethods.getMethodExpressionArray(i).getStringValue() : null;
  }
  
  void removeMethodExpression(int i) {
    DistributedMethods distributedMethods = fApp.getDistributedMethods();
    if(distributedMethods != null) {
      distributedMethods.removeMethodExpression(i);
    }
    if(sizeOfMethodExpressionArray() == 0) {
      fApp.unsetDistributedMethods();
    }
  }
}
