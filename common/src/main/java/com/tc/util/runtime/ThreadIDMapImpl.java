/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.util.runtime;

import com.google.common.collect.MapMaker;
import com.tc.object.locks.ThreadID;

import java.util.Map;

public class ThreadIDMapImpl implements ThreadIDMap {
  private final Map<Long, ThreadID> id2ThreadIDMap = new MapMaker().weakValues().makeMap();

  public synchronized void addTCThreadID(final ThreadID tcThreadID) {
    id2ThreadIDMap.put(Long.valueOf(Thread.currentThread().getId()), tcThreadID);
  }

  public synchronized ThreadID getTCThreadID(final Long javaThreadId) {
    return id2ThreadIDMap.get(javaThreadId);
  }

  /** For testing only - not in interface */
  public synchronized int getSize() {
    return id2ThreadIDMap.size();
  }

}
