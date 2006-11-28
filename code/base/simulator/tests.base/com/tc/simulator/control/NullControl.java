/*
 * Copyright (c) 2003-2006 Terracotta, Inc. All rights reserved.
 */
package com.tc.simulator.control;


public class NullControl implements Control {

  public void waitForStart(long timeout) {
    return;
  }

  public void notifyComplete() {
    return;
  }

  public boolean waitForAllComplete(long timeout) {
    return true;
  }

}
