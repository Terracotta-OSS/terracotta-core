/**
 * Copyright (c) 2003-2006 Terracotta, Inc. All rights reserved.
 */
package com.tc.util.concurrent;

public class NullLifeCycleState implements LifeCycleState {

  public NullLifeCycleState() {
    super();
  }

  public void start()  {
    return;
  }
  
  public boolean isStopRequested() {
    return false;
  }

  public boolean stopAndWait(long waitTime) {
    return true;
  }

}
