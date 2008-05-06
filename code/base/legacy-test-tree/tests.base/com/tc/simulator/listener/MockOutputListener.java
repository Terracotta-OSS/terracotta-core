/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.simulator.listener;

import java.io.PrintStream;


public class MockOutputListener implements OutputListener {
  
  public PrintStream out = System.out;
  public PrintStream err = System.err;
  
  public void setGlobalId(long globalId) {
    return;
  }

  public void println(Object o) {
    out.println(o);
  }

  public void printerr(Object o) {
    err.println(o);
  }
  
}
