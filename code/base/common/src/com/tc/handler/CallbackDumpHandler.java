/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.handler;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class CallbackDumpHandler {
  private final List<CallbackDumpAdapter> dumpObjectList = new CopyOnWriteArrayList<CallbackDumpAdapter>();

  public void registerForDump(CallbackDumpAdapter dumpObject) {
    this.dumpObjectList.add(dumpObject);
  }

  public void dump() {
    for (CallbackDumpAdapter dumpObject : this.dumpObjectList) {
      dumpObject.callbackOnExit(null);
    }
  }
}
