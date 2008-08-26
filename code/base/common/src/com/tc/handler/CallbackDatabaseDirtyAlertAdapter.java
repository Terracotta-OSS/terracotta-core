/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.handler;

import com.tc.logging.CallbackOnExitHandler;
import com.tc.logging.CallbackOnExitState;
import com.tc.logging.TCLogger;

import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;

public class CallbackDatabaseDirtyAlertAdapter implements CallbackOnExitHandler {

  private final TCLogger logger;
  private final TCLogger consoleLogger;

  public CallbackDatabaseDirtyAlertAdapter(TCLogger logger, TCLogger consoleLogger) {
    this.logger = logger;
    this.consoleLogger = consoleLogger;
  }

  public void callbackOnExit(CallbackOnExitState state) {
    printToLogger(state);
    consoleLogger.warn(state.getThrowable().getMessage());
  }

  private void printToLogger(CallbackOnExitState state) {
    ByteArrayOutputStream stream = new ByteArrayOutputStream();
    state.getThrowable().printStackTrace(new PrintWriter(stream, true));
    try {
      logger.warn(stream.toString("UTF-8"));
    } catch (UnsupportedEncodingException e) {
      //
    }
  }
}
