/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.object.lockmanager.api;

import com.tc.object.tx.TimerSpec;

public class WaitLockRequest extends LockRequest {

  private final TimerSpec call;

  public WaitLockRequest(LockID lockID, ThreadID threadID, int lockLevel, String lockType, TimerSpec call ) {
    super(lockID, threadID, lockLevel, lockType);
    this.call = call;
  }

  public TimerSpec getTimerSpec() {
    return call;
  }

  public String toString() {
    return getClass().getName() + "[" + lockID() + ", " + threadID() + ", lockLevel=" + lockLevel() + ", " + call + "]";
  }
}
