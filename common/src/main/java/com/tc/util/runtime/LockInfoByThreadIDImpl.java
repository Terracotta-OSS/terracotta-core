/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.util.runtime;

import com.tc.object.locks.ThreadID;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class LockInfoByThreadIDImpl implements LockInfoByThreadID {

  private final Map<ThreadID, List<String>> heldLocks    = new LinkedHashMap<ThreadID, List<String>>();
  private final Map<ThreadID, List<String>> waitOnLocks  = new LinkedHashMap<ThreadID, List<String>>();
  private final Map<ThreadID, List<String>> pendingLocks = new LinkedHashMap<ThreadID, List<String>>();

  @Override
  public List<String> getHeldLocks(ThreadID threadID) {
    return lockList(heldLocks.get(threadID));
  }

  @Override
  public List<String> getWaitOnLocks(ThreadID threadID) {
    return lockList(waitOnLocks.get(threadID));
  }

  @Override
  public List<String> getPendingLocks(ThreadID threadID) {
    return lockList(pendingLocks.get(threadID));
  }

  private List<String> lockList(List<String> lockList) {
    if (lockList == null) {
      return Collections.emptyList();
    } else {
      return lockList;
    }
  }

  @Override
  public void addLock(LockState lockState, ThreadID threadID, String value) {
    if (lockState == LockState.HOLDING) {
      addLockTo(heldLocks, threadID, value);
    } else if (lockState == LockState.WAITING_ON) {
      addLockTo(waitOnLocks, threadID, value);
    } else if (lockState == LockState.WAITING_TO) {
      addLockTo(pendingLocks, threadID, value);
    } else {
      throw new AssertionError("Unexpected Lock type : " + lockState);
    }
  }

  private void addLockTo(Map<ThreadID, List<String>> lockMap, ThreadID threadID, String value) {
    List<String> lockArray = lockMap.get(threadID);
    if (lockArray == null) {
      List<String> al = new ArrayList<String>();
      al.add(value);
      lockMap.put(threadID, al);
    } else {
      lockArray.add(value);
      lockMap.put(threadID, lockArray);
    }
  }
}
