/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.simulator.control;

public class NullControl implements Control {

  public void waitForStart() {
    return;
  }

  public void notifyComplete() {
    return;
  }

  public boolean waitForAllComplete(long timeout) {
    return true;
  }

  public void notifyMutationComplete() {
    return;
  }

  public boolean waitForMutationComplete(long timeout) {
    return true;
  }

  public void notifyValidationStart() {
    return;
  }

  public boolean waitForValidationStart(long timeout) {
    return true;
  }

}
