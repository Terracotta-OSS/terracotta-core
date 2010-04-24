/*
 * All content copyright (c) 2003-2009 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.util.runtime;

import com.tc.object.locks.ThreadID;
import com.tc.util.Assert;

import java.util.Map;
import java.util.WeakHashMap;

public class ThreadIDMapJdk15 implements ThreadIDMap {
  private final Map<Thread, ThreadID> threadIDMap    = new WeakHashMap<Thread, ThreadID>();
  private final Map<Long, ThreadID>   id2ThreadIDMap = new WeakHashMap<Long, ThreadID>();

  public synchronized void addTCThreadID(final ThreadID tcThreadID) {
    final Thread currentThread = Thread.currentThread();
    final Long javaThreadID = new Long(currentThread.getId());
    Object prev = threadIDMap.put(currentThread, tcThreadID);
    Assert.assertNull(javaThreadID + " should not exist before", prev);
    prev = this.id2ThreadIDMap.put(javaThreadID, tcThreadID);
  }

  public synchronized ThreadID getTCThreadID(final Long javaThreadID) {
    return this.id2ThreadIDMap.get(javaThreadID);
  }

  /** For testing only - not in interface */
  public synchronized int getSize() {
    return this.threadIDMap.size();
  }

}
