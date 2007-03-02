/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.terracotta.session.util;

import com.tc.object.bytecode.Manager;
import com.tc.object.bytecode.ManagerUtil;

public class Lock {

  private final Timer  unlockTimer;
  private final Timer  lockTimer;
  private final String lockId;

  private boolean      isLocked = false;
  private final int    lockType;

  // for non-synchronous-write tests
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
    lockTimer = new Timer(false);
    unlockTimer = new Timer(false);
  }

  public void commitLock() {
    unlockTimer.start();
    ManagerUtil.commitLock(lockId);
    isLocked = false;
    unlockTimer.stop();
  }

  public void getWriteLock() {
    lockTimer.start();
    ManagerUtil.beginLock(lockId, lockType);
    isLocked = true;
    lockTimer.stop();
  }

  public boolean tryWriteLock() {
    lockTimer.start();
    isLocked = ManagerUtil.tryBeginLock(lockId, lockType);
    lockTimer.stop();
    return isLocked;
  }

  public Timer getLockTimer() {
    return lockTimer;
  }

  public Timer getUnlockTimer() {
    return unlockTimer;
  }

  public String getLockId() {
    return lockId;
  }

  public boolean isLocked() {
    return isLocked;
  }
}
