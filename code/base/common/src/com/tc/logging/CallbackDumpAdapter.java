/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.logging;


public class CallbackDumpAdapter implements CallbackOnExitHandler {

  private DumpHandler dumpHandler;
  
  public CallbackDumpAdapter( DumpHandler dumpHandler ) {
    this.dumpHandler = dumpHandler;
  }
  public void callbackOnExit() {
    dumpHandler.dumpToLogger();
  }

}
