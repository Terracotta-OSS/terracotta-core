/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.objectserver.lockmanager.impl;

import com.tc.async.api.Sink;
import com.tc.object.tx.WaitInvocation;

public class TryLockContextImpl extends LockWaitContextImpl {
  public TryLockContextImpl(ServerThreadContext threadContext, Lock lock, WaitInvocation call, int lockLevel,
                            Sink lockResponseSink) {
    super(threadContext, lock, call, lockLevel, lockResponseSink);
  }

  public void waitTimeout() {
    this.getLock().tryRequestLockTimeout(this);
  }
}
