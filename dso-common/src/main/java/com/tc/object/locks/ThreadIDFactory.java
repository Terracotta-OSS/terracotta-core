/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.locks;

import com.google.common.collect.MapMaker;

import java.util.Map;

public class ThreadIDFactory {
  private static final Map<Long, ThreadID> threadIDMap = new MapMaker().weakValues().makeMap();

  public ThreadID getOrCreate(long tid) {
    ThreadID threadID = threadIDMap.get(tid);
    if(threadID == null) {
      threadID = new ThreadID(tid);
      threadIDMap.put(tid, threadID);
    }
    return threadID;
  }
}
