/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.simulator.listener;

import com.tc.simulator.app.ErrorContext;

import java.util.ArrayList;
import java.util.List;


public class MockResultsListener implements ResultsListener {

  public boolean dumpErrors = false;
  public List errors = new ArrayList();
  public boolean notifyStartTimeoutCalled;
  public boolean notifyExecutionTimeoutCalled;
  public Object result;
  
  public void notifyStartTimeout() {
    this.notifyStartTimeoutCalled = true;
  }
  
  public void notifyExecutionTimeout() {
    this.notifyExecutionTimeoutCalled = true;
  }

  public synchronized void notifyError(ErrorContext ectxt) {
    if (dumpErrors) ectxt.dump(System.err);
    errors.add(ectxt);
  }
  
  public void notifyResult(Object theResult) {
    this.result = theResult;
  }

  public void setGlobalId(long globalId) {
    return;
  }
}