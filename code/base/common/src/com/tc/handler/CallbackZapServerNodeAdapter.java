/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.handler;

import com.tc.logging.CallbackOnExitHandler;
import com.tc.logging.CallbackOnExitState;
import com.tc.logging.TCLogger;

public class CallbackZapServerNodeAdapter implements CallbackOnExitHandler {
  private TCLogger consoleLogger;

  public CallbackZapServerNodeAdapter(TCLogger consoleLogger) {
    this.consoleLogger = consoleLogger;
  }

  public void callbackOnExit(CallbackOnExitState state) {
    consoleLogger.warn(state.getThrowable().getMessage());
    // if any cleanup actions before restart, do it here based on the Zap reason type
  }
}
