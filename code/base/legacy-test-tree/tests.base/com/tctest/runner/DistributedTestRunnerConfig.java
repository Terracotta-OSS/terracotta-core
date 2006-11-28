/*
 * Created on Jun 2, 2005
 */
package com.tctest.runner;

public class DistributedTestRunnerConfig {

  public long startTimeout     = 30 * 1000;
  public long executionTimeout = 13 * 60 * 1000;
  
  public long executionTimeout() {
    return this.executionTimeout;
  }

  public long startTimeout() {
    return this.startTimeout;
  }

}
