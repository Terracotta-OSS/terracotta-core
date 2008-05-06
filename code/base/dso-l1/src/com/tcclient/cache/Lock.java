/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tcclient.cache;

import com.tc.object.bytecode.Manager;
import com.tc.util.Assert;

/**
 * TODO: Merge with com.terracotta.session.util.Lock.java.
 *
 */
public class Lock {

  // Resources
  private final Manager manager;
  
  // Config
  private final String lockId;
  private final int    lockType;

  // State
  private boolean      isLocked = false;

  public Lock(final String lockId, Manager manager) {
    this(lockId, Manager.LOCK_TYPE_WRITE, manager);
  }

  public Lock(final String lockId, final int lockType, Manager manager) {
    this.manager = manager;
    
    if (lockType != Manager.LOCK_TYPE_SYNCHRONOUS_WRITE && lockType != Manager.LOCK_TYPE_WRITE) { 
      throw new AssertionError("Trying to set lockType to " + lockType
                               + " -- must be either write or synchronous-write"); 
    }    
    this.lockType = lockType;
    
    Assert.pre(lockId != null && lockId.length() > 0);
    this.lockId = lockId;
  }

  public void commitLock() {
    manager.commitLock(lockId);
    isLocked = false;
  }

  public void getWriteLock() {
    manager.beginLock(lockId, lockType);
    isLocked = true;
  }
  
  public void writeLock() {
    manager.beginLock(lockId, lockType);
  }

  public boolean tryWriteLock() {
    isLocked = manager.tryBeginLock(lockId, lockType);
    return isLocked;
  }

  public String getLockId() {
    return lockId;
  }

  public boolean isLocked() {
    return isLocked;
  }
}
