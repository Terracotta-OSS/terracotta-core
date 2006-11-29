/*
 * Copyright (c) 2003-2006 Terracotta, Inc. All rights reserved.
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
