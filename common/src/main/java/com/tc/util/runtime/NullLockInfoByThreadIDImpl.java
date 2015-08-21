/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.util.runtime;

import com.tc.object.locks.ThreadID;

import java.util.Collections;
import java.util.List;

public class NullLockInfoByThreadIDImpl implements LockInfoByThreadID {

  @Override
  public List<String> getHeldLocks(ThreadID threadID) {
    return Collections.emptyList();
  }

  @Override
  public List<String> getPendingLocks(ThreadID threadID) {
    return Collections.emptyList();
  }

  @Override
  public List<String> getWaitOnLocks(ThreadID threadID) {
    return Collections.emptyList();
  }

  @Override
  public void addLock(LockState lockState, ThreadID threadID, String lockID) {
    //
  }
}
