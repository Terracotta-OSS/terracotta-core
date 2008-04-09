/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.objectserver.lockmanager.api;

/**
 * LockManager's management interface
 */
public interface LockManagerMBean {
  
  public LockMBean[] getAllLocks();

  public DeadlockChain[] scanForDeadlocks();

}
