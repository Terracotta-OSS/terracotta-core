/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.simulator.control;

public class MockControl implements Control {
  public boolean waitForStartCalled;
  public boolean notifyCompleteCalled;
  public boolean waitForAllCompleteCalled;
  public boolean waitForAllCompleteResult;
  public boolean throwTimeoutExceptionInWaitForAllComplete;

  public void waitForStart() {
    waitForStartCalled = true;
  }

  public void notifyComplete() {
    notifyCompleteCalled = true;
  }

  public boolean waitForAllComplete(long timeout) {
    waitForAllCompleteCalled = true;
    return waitForAllCompleteResult;
  }

  public void notifyMutationComplete() {
    throw new AssertionError("This method should be implemented");
  }

  public boolean waitForMutationComplete(long timeout) {
    throw new AssertionError("This method should be implemented");
  }

  public void notifyValidationStart() {
    throw new AssertionError("This method should be implemented");
  }

  public boolean waitForValidationStart(long timeout) {
    throw new AssertionError("This method should be implemented");
  }

}