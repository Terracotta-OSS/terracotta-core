/**
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
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
