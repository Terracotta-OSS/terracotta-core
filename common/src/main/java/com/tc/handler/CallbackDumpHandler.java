/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.handler;

import com.tc.logging.CallbackOnExitHandler;
import com.tc.logging.CallbackOnExitState;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class CallbackDumpHandler implements CallbackOnExitHandler {
  private final List<CallbackDumpAdapter> dumpObjectList = new CopyOnWriteArrayList<CallbackDumpAdapter>();

  public void registerForDump(CallbackDumpAdapter dumpObject) {
    this.dumpObjectList.add(dumpObject);
  }

  public void dump() {
    for (CallbackDumpAdapter dumpObject : this.dumpObjectList) {
      dumpObject.callbackOnExit(null);
    }
  }

  public void callbackOnExit(CallbackOnExitState state) {
    dump();
  }
}
