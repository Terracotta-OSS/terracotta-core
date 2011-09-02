/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object.event;

import com.tc.object.dmi.DmiDescriptor;
import com.tcclient.object.DistributedMethodCall;

/**
 * DMI Manager
 */
public interface DmiManager {

  /**
   * Perform distributed invocation
   * @param receiver Receiver object
   * @param method Method name
   * @param params Parameter values
   * @param runOnAllNodes True to run everywhere, false for local
   * @return True if valid
   */
  boolean distributedInvoke(Object receiver, String method, Object[] params, boolean runOnAllNodes);

  /**
   * Commit distributed invocation.
   */
  void distributedInvokeCommit();

  /**
   * Invoke distributed method call description
   * @param dmc Description of DMI call
   */
  void invoke(DistributedMethodCall dmc);

  /**
   * Extract method call from descriptor
   * @param dd Descriptor
   * @return Method call info
   */
  DistributedMethodCall extract(DmiDescriptor dd);

}