/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tctest.restart;

import EDU.oswego.cs.dl.util.concurrent.CyclicBarrier;
import EDU.oswego.cs.dl.util.concurrent.FutureResult;

public interface RestartTestApp {

  public void setStartBarrier(CyclicBarrier startBarrier);
  
  public void setID(int id);

  public int getID();

  /**
   * Initial, unstarted state
   */
  public boolean isInit();

  /**
   * The app has started
   */
  public boolean isStart();

  /**
   * The app has acquired the lock
   */
  public boolean isHolder();

  /**
   * The app is waiting on the lock
   */
  public boolean isWaiter();
  
  /**
   * The app is in the end state.
   */
  public boolean isEnd();

  /**
   * Resets the app to the INIT state.
   */
  public void reset();

  public String getStateName();

  public void blockShutdown(FutureResult blocker) throws Exception;

}
