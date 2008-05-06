/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package org.terracotta.dso.views;

import com.terracottatech.config.DistributedMethods;
import com.terracottatech.config.DsoApplication;

public class DistributedMethodsWrapper {
  private DsoApplication fApp;
  private DistributedMethodWrapper[] children;
  
  DistributedMethodsWrapper(DsoApplication dsoApp) {
    fApp = dsoApp;
  }
  
  DistributedMethodWrapper[] createDistributedMethodWrappers() {
    int count = sizeOfMethodExpressionArray();

    children = new DistributedMethodWrapper[count];
    for(int i = 0; i < count; i++) {
      children[i] = new DistributedMethodWrapper(this, i);
    }
    
    return children;
  }

  DistributedMethodWrapper[] getChildren() {
    return children;
  }
  
  DistributedMethodWrapper getChildAt(int index) {
    return children != null ? children[index] : null;
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
