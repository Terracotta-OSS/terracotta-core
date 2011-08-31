/**
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
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
