/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
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
