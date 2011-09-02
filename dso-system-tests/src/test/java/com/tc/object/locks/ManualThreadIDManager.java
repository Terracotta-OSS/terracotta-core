/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.locks;

import com.tc.util.runtime.ThreadIDManager;

public class ManualThreadIDManager implements ThreadIDManager {
  
  private ThreadLocal<ThreadID> threadID = new ThreadLocal<ThreadID>();
  
  public void setThreadID(ThreadID thread) {
    threadID.set(thread);
  }
  
  public ThreadID getThreadID() {
    return threadID.get();
  }    
}
