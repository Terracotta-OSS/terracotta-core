/**
 * Copyright (c) 2003-2006 Terracotta, Inc. All rights reserved.
 */
package com.tc.util.concurrent;

/**
 * Inteface for passing around lifecycle state
 */
public interface LifeCycleState {
  
  public void start();
  
  public boolean isStopRequested();

  public boolean stopAndWait(long waitTime);
}
