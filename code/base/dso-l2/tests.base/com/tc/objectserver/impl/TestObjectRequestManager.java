/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.impl;

import com.tc.objectserver.api.ObjectRequestManager;
import com.tc.objectserver.context.ManagedObjectRequestContext;
import com.tc.util.concurrent.NoExceptionLinkedQueue;

public class TestObjectRequestManager implements ObjectRequestManager {

  public final NoExceptionLinkedQueue startCalls = new NoExceptionLinkedQueue();

  public void requestObjects(ManagedObjectRequestContext responseContext, int maxReachableObjects) {
    // NOP
  }

  public void start() {
    startCalls.put(new Object());
  }

}
