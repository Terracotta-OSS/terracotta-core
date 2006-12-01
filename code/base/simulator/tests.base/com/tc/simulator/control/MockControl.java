/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.simulator.control;

import com.tc.util.TCTimeoutException;


public class MockControl implements Control {
  
  public boolean waitForStartCalled;
  public boolean throwTimeoutExceptionInWaitForStart;

  public boolean notifyCompleteCalled;
  
  public boolean waitForAllCompleteCalled;
  public boolean waitForAllCompleteResult;
  public boolean throwTimeoutExceptionInWaitForAllComplete;

  public void waitForStart(long timeout) throws TCTimeoutException {
    waitForStartCalled = true;
    if (throwTimeoutExceptionInWaitForStart) {
      throw new TCTimeoutException(timeout);
    }
  }

  public void notifyComplete() {
    notifyCompleteCalled = true;
  }

  public boolean waitForAllComplete(long timeout) {
    waitForAllCompleteCalled = true;
    return waitForAllCompleteResult;
  }

}