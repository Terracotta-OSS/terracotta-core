/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.object.lockmanager.api;

import com.tc.object.tx.WaitInvocation;

public class TryLockRequest extends WaitLockRequest {
  public TryLockRequest(LockID lockID, ThreadID threadID, int lockType, WaitInvocation call ) {
    super(lockID, threadID, lockType, call);
    call.mark();
  }
}
