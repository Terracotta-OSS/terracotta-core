/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.logging;

import com.tc.handler.LockInfoDumpHandler;
import com.tc.util.runtime.LockInfoByThreadID;
import com.tc.util.runtime.LockInfoByThreadIDImpl;
import com.tc.util.runtime.ThreadDumpUtil;
import com.tc.util.runtime.ThreadIDMap;

public class ThreadDumpHandler implements CallbackOnExitHandler {

  private static final TCLogger     logger = TCLogging.getLogger(ThreadDumpHandler.class);
  private final LockInfoDumpHandler lockInfoDumpHandler;

  public ThreadDumpHandler(LockInfoDumpHandler lockInfoDumpHandler) {
    this.lockInfoDumpHandler = lockInfoDumpHandler;
  }

  public void callbackOnExit(CallbackOnExitState state) {
    LockInfoByThreadID lockInfo = new LockInfoByThreadIDImpl();
    ThreadIDMap threadIDMap = this.lockInfoDumpHandler.getThreadIDMap();
    lockInfoDumpHandler.addAllLocksTo(lockInfo);
    logger.error(ThreadDumpUtil.getThreadDump(lockInfo, threadIDMap));
  }
}
