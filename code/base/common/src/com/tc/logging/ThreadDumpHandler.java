/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.logging;

import com.tc.util.runtime.ThreadDumpUtil;

public class ThreadDumpHandler implements CallbackOnExitHandler {

  private static final TCLogger logger = TCLogging.getLogger(ThreadDumpHandler.class);

  public void callbackOnExit(CallbackOnExitState state) {
    logger.error(ThreadDumpUtil.getThreadDump());
  }
}
