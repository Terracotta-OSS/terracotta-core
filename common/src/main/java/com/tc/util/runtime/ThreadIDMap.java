/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.util.runtime;

import com.tc.object.locks.ThreadID;

/**
 * Thread to TC Thread ID mapping
 */
public interface ThreadIDMap {

  public void addTCThreadID(ThreadID tcThreadID);

  public ThreadID getTCThreadID(Long javaThreadID);

}
