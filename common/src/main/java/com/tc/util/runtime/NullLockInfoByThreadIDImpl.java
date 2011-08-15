/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.util.runtime;

import com.tc.object.locks.ThreadID;

import java.util.ArrayList;

public class NullLockInfoByThreadIDImpl implements LockInfoByThreadID {

  public ArrayList getHeldLocks(ThreadID threadID) {
    return new ArrayList();
  }

  public ArrayList getPendingLocks(ThreadID threadID) {
    return new ArrayList();
  }

  public ArrayList getWaitOnLocks(ThreadID threadID) {
    return new ArrayList();
  }

  public void addLock(LockState lockState, ThreadID threadID, String lockID) {
    //
  }
}
