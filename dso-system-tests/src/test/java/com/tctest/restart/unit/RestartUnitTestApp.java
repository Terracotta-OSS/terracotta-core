/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tctest.restart.unit;

import com.tctest.restart.RestartTestApp;

public interface RestartUnitTestApp extends RestartTestApp {

  public void attemptLock();

  /**
   * Commands the app to release the lock.
   */
  public void fallThrough();

  public void doWait(long millis);

  public void doNotifyAll();

  public void doNotify();

  /**
   * Sets the shared lock, turning it into a managed object.
   */
  public void setDistributedSharedLock(Object lck);

  /**
   * Sets the shared lock, but doesn't turn it into a managed object.
   */
  public void setSharedLock(Object lck);

}
