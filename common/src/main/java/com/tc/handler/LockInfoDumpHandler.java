/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.handler;

import com.tc.util.runtime.LockInfoByThreadID;
import com.tc.util.runtime.ThreadIDMap;

public interface LockInfoDumpHandler {

  public ThreadIDMap getThreadIDMap();

  public void addAllLocksTo(LockInfoByThreadID lockInfo);

}
