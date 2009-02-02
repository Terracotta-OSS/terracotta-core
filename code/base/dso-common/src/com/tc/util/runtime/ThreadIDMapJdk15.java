/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.util.runtime;

import com.tc.object.lockmanager.api.ThreadID;
import com.tc.util.Assert;

import java.util.HashMap;
import java.util.Map;

public class ThreadIDMapJdk15 implements ThreadIDMap {
  private final Map<Long, ThreadID> threadIDMap = new HashMap<Long, ThreadID>();

  public synchronized void addTCThreadID(ThreadID tcThreadID) {
    Object prev = threadIDMap.put(Thread.currentThread().getId(), tcThreadID);
    Assert.assertNull(prev);
  }

  public synchronized ThreadID getTCThreadID(long vmThreadID) {
    return threadIDMap.get(vmThreadID);
  }
}
