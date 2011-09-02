/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest.runner;

public class DistributedTestRunnerConfig {
  private static final boolean DEBUG        = false;

  private final long           defaultExecutionTimeout;
  private final long           startTimeout = 30 * 1000;
  private long                 executionTimeout;

  public DistributedTestRunnerConfig(long defaultExecutionTimeoutInSec) {
    defaultExecutionTimeout = defaultExecutionTimeoutInSec * 1000;
    executionTimeout = this.defaultExecutionTimeout;
    debugPrintln("***** setting default execution timeout and execution timeout: " + defaultExecutionTimeout);
  }

  public long executionTimeout() {
    debugPrintln("***** getting execution timeout: " + executionTimeout);
    return this.executionTimeout;
  }

  public long startTimeout() {
    debugPrintln("***** getting start timeout: " + startTimeout);
    return this.startTimeout;
  }

  public void setExecutionTimeout(long executionTimeout) {
    if (executionTimeout > defaultExecutionTimeout) { throw new AssertionError(
                                                                               "Cannot set execution timeout to be greater than the test timeout."); }
    debugPrintln("***** setting execution timeout: " + executionTimeout);
    this.executionTimeout = executionTimeout;
  }

  private void debugPrintln(String s) {
    if (DEBUG) {
      System.err.println(s);
    }
  }
}
