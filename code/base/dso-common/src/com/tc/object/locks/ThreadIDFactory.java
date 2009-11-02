/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.locks;

import java.util.WeakHashMap;

public class ThreadIDFactory {
  private static final WeakHashMap<Long, ThreadID> threadIDMap = new WeakHashMap<Long, ThreadID>();

  public ThreadID getOrCreate(long tid) {
    ThreadID threadID = threadIDMap.get(tid);
    if(threadID == null) {
      threadID = new ThreadID(tid);
      threadIDMap.put(tid, threadID);
    }
    return threadID;
  }
}
