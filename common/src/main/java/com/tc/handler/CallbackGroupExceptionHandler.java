/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.handler;

import com.tc.lang.ServerExitStatus;
import com.tc.logging.CallbackOnExitHandler;
import com.tc.logging.CallbackOnExitState;
import com.tc.logging.TCLogger;

public class CallbackGroupExceptionHandler implements CallbackOnExitHandler {

  private final TCLogger logger;
  private final TCLogger consoleLogger;

  public CallbackGroupExceptionHandler(TCLogger logger, TCLogger consoleLogger) {
    this.logger = logger;
    this.consoleLogger = consoleLogger;
  }

  public void callbackOnExit(CallbackOnExitState state) {
    logger.error(state.getThrowable().getMessage(), state.getThrowable());
    consoleLogger.error(state.getThrowable().getMessage());
    System.exit(ServerExitStatus.EXITCODE_STARTUP_ERROR);
  }
}
