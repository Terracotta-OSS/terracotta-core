/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.object.appevent;

import com.tc.util.NonPortableReason;

public class NonPortableLogicalInvokeContext extends NonPortableEventContext {

  private static final long serialVersionUID = -7205127466022191118L;

  private final String logicalMethod;

  public NonPortableLogicalInvokeContext(String targetClassName, String threadName,
                                         String clientId, String logicalMethod) {
    super(targetClassName, threadName, clientId);
    this.logicalMethod = logicalMethod;
  }

  public String getLogicalMethod() {
    return logicalMethod;
  }

  public void addDetailsTo(NonPortableReason reason) {
    super.addDetailsTo(reason);
    reason.addDetail("Logically-managed class name", getTargetClassName());
    reason.addDetail("Logical method name", logicalMethod);
  }

}
