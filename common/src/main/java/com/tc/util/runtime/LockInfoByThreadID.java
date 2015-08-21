/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.util.runtime;

import com.tc.object.locks.ThreadID;

import java.util.List;

public interface LockInfoByThreadID {
  public void addLock(LockState lockState, ThreadID threadID, String lockID);

  public List<String> getHeldLocks(ThreadID threadID);

  public List<String> getWaitOnLocks(ThreadID threadID);

  public List<String> getPendingLocks(ThreadID threadID);
}
