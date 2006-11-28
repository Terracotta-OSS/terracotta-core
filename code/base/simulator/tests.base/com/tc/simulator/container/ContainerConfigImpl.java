/*
 * Copyright (c) 2003-2006 Terracotta, Inc. All rights reserved.
 */
package com.tc.simulator.container;

public class ContainerConfigImpl implements ContainerConfig {
  
  private final int executionCount;
  private final boolean isMaster;
  private final int executionTimeout;

  public ContainerConfigImpl(int executionCount, int executionTimeout, boolean isMaster) {
    this.executionCount = executionCount;
    this.executionTimeout = executionTimeout;
    this.isMaster = isMaster;
    
  }

  public int getApplicationInstanceCount() {
    return executionCount;
  }

  public long getContainerStartTimeout() {
    return 30 * 1000;
  }

  public long getApplicationStartTimeout() {
    return 30 * 1000;
  }

  public long getApplicationExecutionTimeout() {
    return executionTimeout;
  }

  public boolean dumpErrors() {
    return false;
  }

  public boolean dumpOutput() {
    return false;
  }

  public boolean aggregate() {
    return false;
  }

  public boolean stream() {
    return false;
  }

  public boolean isMaster() {
    return isMaster;
  }

}
