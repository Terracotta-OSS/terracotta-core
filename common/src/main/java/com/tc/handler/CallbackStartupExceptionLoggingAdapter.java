/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.handler;

import com.tc.logging.CallbackOnExitHandler;
import com.tc.logging.CallbackOnExitState;

public class CallbackStartupExceptionLoggingAdapter implements CallbackOnExitHandler {

  private String extraMessage;

  public CallbackStartupExceptionLoggingAdapter() {
    this("");
  }

  public CallbackStartupExceptionLoggingAdapter(String extraMessage) {
    this.extraMessage = extraMessage;
  }

  public void callbackOnExit(CallbackOnExitState state) {
    System.err.println("");
    System.err.println("");
    System.err.println("Fatal Terracotta startup exception:");
    System.err.println("");
    System.err.println(" " + state.getThrowable().getMessage() + extraMessage);
    System.err.println("");
    System.err.println("Server startup failed.");
  }
}
