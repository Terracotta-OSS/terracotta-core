/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.handler;

import com.tc.logging.CallbackOnExitHandler;
import com.tc.logging.CallbackOnExitState;
import com.tc.logging.TCLogger;

public class CallbackDatabaseDirtyAlertAdapter implements CallbackOnExitHandler {

  private static final String CONSOLE_MESSAGE = "This Terracotta server instance shut down and failed to wipe its database and restart "
                                               + "because the database may be corrupt. The database must be manually wiped before the "
                                               + "Terracotta server instance can be started and allowed to rejoin the cluster.\n";

  private final TCLogger      logger;
  private final TCLogger      consoleLogger;

  public CallbackDatabaseDirtyAlertAdapter(TCLogger logger, TCLogger consoleLogger) {
    this.logger = logger;
    this.consoleLogger = consoleLogger;
  }

  public void callbackOnExit(CallbackOnExitState state) {
    logger.warn(state.getThrowable().getMessage(), state.getThrowable());
    consoleLogger.warn(CONSOLE_MESSAGE);
  }
}
