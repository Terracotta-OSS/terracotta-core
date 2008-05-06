/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest;

public class TestState {
  public static final int RUNNING  = 0;
  public static final int STOPPING = 1;
  private int             state;

  public TestState() {
    state = RUNNING;
  }

  public TestState(boolean isRunning) {
    if (isRunning) {
      state = RUNNING;
    } else {
      state = STOPPING;
    }
  }

  public synchronized void setTestState(int state) {
    this.state = state;
  }

  public synchronized boolean isRunning() {
    return state == RUNNING;
  }
}
