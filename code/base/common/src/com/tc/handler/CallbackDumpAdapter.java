/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.handler;

import com.tc.logging.CallbackOnExitHandler;
import com.tc.logging.CallbackOnExitState;
import com.tc.text.DumpLoggerWriter;
import com.tc.text.PrettyPrintable;
import com.tc.text.PrettyPrinterImpl;

import java.io.PrintWriter;

public class CallbackDumpAdapter implements CallbackOnExitHandler {

  private final PrettyPrintable dumpObject;

  public CallbackDumpAdapter(PrettyPrintable dumpObject) {
    this.dumpObject = dumpObject;
  }

  public void callbackOnExit(CallbackOnExitState state) {
    DumpLoggerWriter writer = new DumpLoggerWriter();
    PrintWriter pw = new PrintWriter(writer);
    PrettyPrinterImpl prettyPrinter = new PrettyPrinterImpl(pw);
    prettyPrinter.autoflush(false);
    prettyPrinter.visit(dumpObject);
    writer.flush();
  }
}
