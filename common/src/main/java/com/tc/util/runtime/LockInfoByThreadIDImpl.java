/*
 *
 *  The contents of this file are subject to the Terracotta Public License Version
 *  2.0 (the "License"); You may not use this file except in compliance with the
 *  License. You may obtain a copy of the License at
 *
 *  http://terracotta.org/legal/terracotta-public-license.
 *
 *  Software distributed under the License is distributed on an "AS IS" basis,
 *  WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 *  the specific language governing rights and limitations under the License.
 *
 *  The Covered Software is Terracotta Core.
 *
 *  The Initial Developer of the Covered Software is
 *  Terracotta, Inc., a Software AG company
 *
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
