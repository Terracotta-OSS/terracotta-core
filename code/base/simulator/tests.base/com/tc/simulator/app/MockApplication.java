/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.simulator.app;

public class MockApplication implements Application {

  public String           globalId;
  public long             waitInterval;
  public boolean          throwException;
  public RuntimeException exception;
  public boolean          result;

  public void run() {
    try {
      Thread.sleep(waitInterval);
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    }
    if (this.throwException) { throw exception; }
  }

  public String getApplicationId() {
    return this.globalId;
  }

  public boolean interpretResult(Object o) {
    return result;
  }
}