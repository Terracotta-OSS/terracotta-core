/*
 * Copyright (c) 2003-2006 Terracotta, Inc. All rights reserved.
 */
package com.tc.objectserver.lockmanager.api;

/**
 * LockManager's management interface
 */
public interface LockManagerMBean {
  
  public LockMBean[] getAllLocks();

  public DeadlockChain[] scanForDeadlocks();

}
