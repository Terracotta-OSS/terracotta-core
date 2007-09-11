/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.object.appevent;

import com.tc.util.NonPortableReason;

public class NonPortableLogicalInvokeContext extends NonPortableEventContext {

  private static final long serialVersionUID = -7205127466022191118L;

  private final String logicalMethod;
  private transient final Object[] params;
  private final int paramIndex;

  public NonPortableLogicalInvokeContext(Object pojo, String threadName,
                                         String clientId, String logicalMethod, Object[] params, int index) {
    super(pojo, threadName, clientId);
    this.logicalMethod = logicalMethod;
    this.params = params;
    this.paramIndex = index;
  }

  public String getLogicalMethod() {
    return logicalMethod;
  }

  public Object[] getParameters() {
    return params;
  }
  
  public int getParameterIndex() {
    return paramIndex;
  }
  
  public void addDetailsTo(NonPortableReason reason) {
    super.addDetailsTo(reason);
    reason.addDetail("Logically-managed class name", getTargetClassName());
    reason.addDetail("Logical method name", logicalMethod);
  }

}
