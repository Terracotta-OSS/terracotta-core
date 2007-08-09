/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object.event;

import com.tc.object.dmi.DmiDescriptor;
import com.tcclient.object.DistributedMethodCall;

public interface DmiManager {

  boolean distributedInvoke(Object receiver, String method, Object[] params, boolean runOnAllNodes);

  void distributedInvokeCommit();

  void invoke(DistributedMethodCall dmc);

  DistributedMethodCall extract(DmiDescriptor dd);

}