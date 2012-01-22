/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.appevent;

import com.tc.util.NonPortableReason;

/**
 * Event context for a non-portable event on a logical invoke.
 */
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

  /**
   * @return Get logical method being called
   */
  public String getLogicalMethod() {
    return logicalMethod;
  }

  /**
   * @return Get parameters being passed on method call
   */
  public Object[] getParameters() {
    return params;
  }
  
  /**
   * @return Get parameter index that was non-portable
   */
  public int getParameterIndex() {
    return paramIndex;
  }
  
  public void addDetailsTo(NonPortableReason reason) {
    super.addDetailsTo(reason);
    reason.addDetail("Logically-managed class name", getTargetClassName());
    reason.addDetail("Logical method name", logicalMethod);
  }

}
