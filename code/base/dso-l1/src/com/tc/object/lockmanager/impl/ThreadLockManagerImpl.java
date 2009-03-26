/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object.lockmanager.impl;

import com.tc.object.lockmanager.api.ClientLockManager;
import com.tc.object.lockmanager.api.LockID;
import com.tc.object.lockmanager.api.Notify;
import com.tc.object.lockmanager.api.ThreadLockManager;
import com.tc.object.lockmanager.api.WaitListener;
import com.tc.object.tx.TimerSpec;
import com.tc.util.runtime.ThreadIDManager;

public class ThreadLockManagerImpl implements ThreadLockManager {

  private final ClientLockManager lockManager;
  private final ThreadIDManager   threadIDManager;

  public ThreadLockManagerImpl(final ClientLockManager lockManager, final ThreadIDManager threadIDManager) {
    this.lockManager = lockManager;
    this.threadIDManager = threadIDManager;
  }

  public LockID lockIDFor(final String lockName) {
    return lockManager.lockIDFor(lockName);
  }

  public int queueLength(final LockID lockID) {
    return lockManager.queueLength(lockID, threadIDManager.getThreadID());
  }

  public int waitLength(final LockID lockID) {
    return lockManager.waitLength(lockID, threadIDManager.getThreadID());
  }

  public int localHeldCount(final LockID lockID, final int lockLevel) {
    return lockManager.localHeldCount(lockID, lockLevel, threadIDManager.getThreadID());
  }

  public boolean isLocked(final LockID lockID, final int lockLevel) {
    return lockManager.isLocked(lockID, threadIDManager.getThreadID(), lockLevel);
  }

  public void lock(final LockID lockID, final int lockLevel, final String lockObjectType, final String contextInfo) {
    lockManager.lock(lockID, threadIDManager.getThreadID(), lockLevel, lockObjectType, contextInfo);
  }

  public void lockInterruptibly(final LockID lockID, final int lockLevel, final String lockObjectType, final String contextInfo) throws InterruptedException {
    lockManager.lockInterruptibly(lockID, threadIDManager.getThreadID(), lockLevel, lockObjectType, contextInfo);
  }

  public boolean tryLock(final LockID lockID, final TimerSpec timeout, final int lockLevel, final String lockObjectType) {
    return lockManager.tryLock(lockID, threadIDManager.getThreadID(), timeout, lockLevel, lockObjectType);
  }

  public void wait(final LockID lockID, final TimerSpec call, final Object object, final WaitListener waitListener) throws InterruptedException {
    lockManager.wait(lockID, threadIDManager.getThreadID(), call, object, waitListener);
  }

  public Notify notify(final LockID lockID, final boolean all) {
    // XXX: HACK HACK HACK: this is here because notifies need to be attached to transactions.
    // this needs to be refactored. --Orion (10/26/05)
    return lockManager.notify(lockID, threadIDManager.getThreadID(), all);
  }

  public void unlock(final LockID lockID) {
    lockManager.unlock(lockID, threadIDManager.getThreadID());
  }

  public void pinLock(LockID lockId) {
    lockManager.pinLock(lockId);
  }

  public void unpinLock(LockID lockId) {
    lockManager.unpinLock(lockId);
  }

  public void evictLock(LockID lockId) {
    lockManager.evictLock(lockId);
  }
}
