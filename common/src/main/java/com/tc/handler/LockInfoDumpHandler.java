/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.handler;

import com.tc.util.runtime.LockInfoByThreadID;
import com.tc.util.runtime.ThreadIDMap;

public interface LockInfoDumpHandler {

  public ThreadIDMap getThreadIDMap();

  public void addAllLocksTo(LockInfoByThreadID lockInfo);

}
