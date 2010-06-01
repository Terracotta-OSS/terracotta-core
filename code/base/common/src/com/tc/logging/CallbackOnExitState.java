/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.logging;

public class CallbackOnExitState {
  private volatile boolean restartNeeded = false;
  private final Throwable  throwable;

  public CallbackOnExitState(Throwable t) {
    this.throwable = t;
  }

  public Throwable getThrowable() {
    return throwable;
  }

  public void setRestartNeeded() {
    this.restartNeeded = true;
  }

  public boolean isRestartNeeded() {
    return this.restartNeeded;
  }

  @Override
  public String toString() {
    return "CallbackOnExitState[Throwable: " + throwable.getClass() + "; RestartNeeded: " + isRestartNeeded() + "]";
  }
}
