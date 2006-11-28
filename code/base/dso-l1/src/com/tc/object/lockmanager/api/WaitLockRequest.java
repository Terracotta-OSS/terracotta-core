/*
 * Created on May 11, 2005 TODO To change the template for this generated file go to Window - Preferences - Java - Code
 * Style - Code Templates
 */
package com.tc.object.lockmanager.api;

import com.tc.object.tx.WaitInvocation;

public class WaitLockRequest extends LockRequest {

  private final WaitInvocation call;

  public WaitLockRequest(LockID lockID, ThreadID threadID, int lockType, WaitInvocation call ) {
    super(lockID, threadID, lockType);
    this.call = call;
  }

  public WaitInvocation getWaitInvocation() {
    return call;
  }
}
