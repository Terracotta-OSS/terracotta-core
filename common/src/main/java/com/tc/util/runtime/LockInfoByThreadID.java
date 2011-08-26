/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
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
