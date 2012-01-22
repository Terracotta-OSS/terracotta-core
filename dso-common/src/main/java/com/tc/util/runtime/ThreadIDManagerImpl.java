/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.util.runtime;

import com.tc.object.locks.ThreadID;
import com.tc.util.VicariousThreadLocal;

public class ThreadIDManagerImpl implements ThreadIDManager {

  private final ThreadLocal threadID;
  private long              threadIDSequence;
  private final ThreadIDMap threadIDMap;

  public ThreadIDManagerImpl(final ThreadIDMap threadIDMap) {
    this.threadID = new VicariousThreadLocal();
    this.threadIDMap = threadIDMap;
  }

  public ThreadID getThreadID() {
    ThreadID rv = (ThreadID) threadID.get();
    if (rv == null) {
      rv = new ThreadID(nextThreadID(), Thread.currentThread().getName());
      threadIDMap.addTCThreadID(rv);
      threadID.set(rv);
    }
    return rv;
  }

  private synchronized long nextThreadID() {
    return ++threadIDSequence;
  }
}
