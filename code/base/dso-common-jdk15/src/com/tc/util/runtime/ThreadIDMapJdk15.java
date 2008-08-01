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
  private final Map<Long, Long> threadIDMap = new HashMap<Long, Long>();

  public synchronized void addTCThreadID(ThreadID tcThreadID) {
    Object prev = threadIDMap.put(Thread.currentThread().getId(), tcThreadID.toLong());
    Assert.assertNull(prev);
  }

  public synchronized Long getTCThreadID(long vmThreadID) {
    return threadIDMap.get(vmThreadID);
  }
}
