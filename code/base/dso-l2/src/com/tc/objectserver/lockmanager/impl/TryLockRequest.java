/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.objectserver.lockmanager.impl;

import com.tc.async.api.Sink;
import com.tc.object.tx.WaitInvocation;

public class TryLockRequest extends Request {
  private WaitInvocation timeout;
  
  /**
   * Create a new tryLock request
   * 
   * @param threadContext the open transaction associated with this request
   * @param lockLevel the lock level that will be in lock response message to the client
   * @param lockResponseSink the sink that accepts the lock response events
   * @param timeout the waiting time for the request to be rejected
   */
  public TryLockRequest(ServerThreadContext txn, int lockLevel, Sink lockResponseSink, WaitInvocation timeout) {
    super(txn, lockLevel, lockResponseSink);
    this.timeout = timeout;
  }
  
  public WaitInvocation getTimeout() {
    return timeout;
  }
  
  public String toString() {
    String str = super.toString();
    return str;
  }

}
