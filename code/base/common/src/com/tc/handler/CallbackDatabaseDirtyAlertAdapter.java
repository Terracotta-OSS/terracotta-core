/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.handler;

import com.tc.logging.CallbackOnExitHandler;
import com.tc.logging.CallbackOnExitState;
import com.tc.logging.TCLogger;

public class CallbackDatabaseDirtyAlertAdapter implements CallbackOnExitHandler {

  private final TCLogger logger;
  private final TCLogger consoleLogger;

  public CallbackDatabaseDirtyAlertAdapter(TCLogger logger, TCLogger consoleLogger) {
    this.logger = logger;
    this.consoleLogger = consoleLogger;
  }

  public void callbackOnExit(CallbackOnExitState state) {
    logger.warn(state.getThrowable().getStackTrace());
    consoleLogger.warn(state.getThrowable().getMessage());
  }
}
