/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tcclient.cache;

import com.tc.object.bytecode.Manager;
import com.tc.object.bytecode.ManagerUtil;
import com.tc.util.Assert;

/**
 * TODO: Merge with com.terracotta.session.util.Lock.java.
 *
 */
public class Lock {

  private final String lockId;

  private boolean      isLocked = false;
  private final int    lockType;

  public Lock(final String lockId) {
    this(lockId, Manager.LOCK_TYPE_WRITE);
  }

  public Lock(final String lockId, final int lockType) {
    if (lockType != Manager.LOCK_TYPE_SYNCHRONOUS_WRITE && lockType != Manager.LOCK_TYPE_WRITE) { throw new AssertionError(
                                                                                                                           "Trying to set lockType to "
                                                                                                                               + lockType
                                                                                                                               + " -- must be either write or synchronous-write"); }

    this.lockType = lockType;
    Assert.pre(lockId != null && lockId.length() > 0);
    this.lockId = lockId;
  }

  public void commitLock() {
    ManagerUtil.commitLock(lockId);
    isLocked = false;
  }

  public void getWriteLock() {
    ManagerUtil.beginLock(lockId, lockType);
    isLocked = true;
  }
  
  public void writeLock() {
    ManagerUtil.beginLock(lockId, lockType);
  }

  public boolean tryWriteLock() {
    isLocked = ManagerUtil.tryBeginLock(lockId, lockType);
    return isLocked;
  }

  public String getLockId() {
    return lockId;
  }

  public boolean isLocked() {
    return isLocked;
  }
}
