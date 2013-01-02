/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.util.concurrent;

public class NullLifeCycleState implements LifeCycleState {

  public NullLifeCycleState() {
    super();
  }

  @Override
  public void start()  {
    return;
  }
  
  @Override
  public boolean isStopRequested() {
    return false;
  }

  @Override
  public boolean stopAndWait(long waitTime) {
    return true;
  }

}
