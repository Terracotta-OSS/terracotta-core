/*
 * All content copyright (c) 2003-2009 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.util.runtime;

import com.tc.object.lockmanager.api.ThreadID;
import com.tc.util.Assert;

import java.util.Map;
import java.util.WeakHashMap;

public class ThreadIDMapJdk15 implements ThreadIDMap {
  private final Map<Thread, ThreadID> threadIDMap = new WeakHashMap<Thread, ThreadID>();

  public synchronized void addTCThreadID(ThreadID tcThreadID) {
    Object prev = threadIDMap.put(Thread.currentThread(), tcThreadID);
    Assert.assertNull(prev);
  }

  public synchronized ThreadID getTCThreadID(Thread thread) {
    return threadIDMap.get(thread);
  }
  
  /** For testing only - not in interface */
  public synchronized int getSize() {
    return threadIDMap.size();
  }
}
