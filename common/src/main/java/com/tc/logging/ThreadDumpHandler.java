/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
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
