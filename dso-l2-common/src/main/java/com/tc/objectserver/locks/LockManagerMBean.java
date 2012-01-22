/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.locks;

/**
 * LockManager's management interface
 */
public interface LockManagerMBean {

  public LockMBean[] getAllLocks();

}
