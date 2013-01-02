/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.util.runtime;

import com.tc.object.locks.ThreadID;

import java.util.ArrayList;

public class NullLockInfoByThreadIDImpl implements LockInfoByThreadID {

  @Override
  public ArrayList getHeldLocks(ThreadID threadID) {
    return new ArrayList();
  }

  @Override
  public ArrayList getPendingLocks(ThreadID threadID) {
    return new ArrayList();
  }

  @Override
  public ArrayList getWaitOnLocks(ThreadID threadID) {
    return new ArrayList();
  }

  @Override
  public void addLock(LockState lockState, ThreadID threadID, String lockID) {
    //
  }
}
