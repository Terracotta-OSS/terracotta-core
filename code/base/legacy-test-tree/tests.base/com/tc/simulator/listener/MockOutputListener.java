/*
 * Copyright (c) 2003-2006 Terracotta, Inc. All rights reserved.
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
