/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tcsimulator.listener;


import com.tc.simulator.listener.OutputListener;

import java.io.PrintStream;

public final class OutputListenerObject implements OutputListener {
  private PrintStream       out;
  private PrintStream       err;

  public OutputListenerObject() {
    this.out = System.out;
    this.err = System.err;
  }

  public void println(Object o) {
    this.out.println(o);
  }

  public void printerr(Object o) {
    this.err.println(o);
  }
}