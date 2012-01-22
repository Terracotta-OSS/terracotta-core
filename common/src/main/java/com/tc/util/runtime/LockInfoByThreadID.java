/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.util.runtime;

import com.tc.object.locks.ThreadID;

import java.util.ArrayList;

public interface LockInfoByThreadID {
  public void addLock(LockState lockState, ThreadID threadID, String lockID);

  public ArrayList getHeldLocks(ThreadID threadID);

  public ArrayList getWaitOnLocks(ThreadID threadID);

  public ArrayList getPendingLocks(ThreadID threadID);
}
