/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
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
