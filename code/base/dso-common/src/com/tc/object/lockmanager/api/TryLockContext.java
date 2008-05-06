/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object.lockmanager.api;

import com.tc.net.groups.ClientID;
import com.tc.object.tx.TimerSpec;

public class TryLockContext extends WaitContext {
  public TryLockContext() {
    return;
  }

  public TryLockContext(LockID lockID, ClientID clientID, ThreadID threadID, int lockLevel, String lockType,
                        TimerSpec timeSpan) {
    super(lockID, clientID, threadID, lockLevel, lockType, timeSpan);
  }
}
