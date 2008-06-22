/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.handler;

import com.tc.logging.CallbackOnExitHandler;
import com.tc.logging.CallbackOnExitState;
import com.tc.logging.DumpHandler;

public class CallbackDumpAdapter implements CallbackOnExitHandler {

  private DumpHandler dumpHandler;

  public CallbackDumpAdapter(DumpHandler dumpHandler) {
    this.dumpHandler = dumpHandler;
  }

  public void callbackOnExit(CallbackOnExitState state) {
    dumpHandler.dumpToLogger();
  }
}
