/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.object.lockmanager.impl;

import com.tc.object.lockmanager.api.ClientLockManager;
import com.tc.object.lockmanager.api.LockID;
import com.tc.object.lockmanager.api.Notify;
import com.tc.object.lockmanager.api.ThreadID;
import com.tc.object.lockmanager.api.ThreadLockManager;
import com.tc.object.lockmanager.api.WaitListener;
import com.tc.object.tx.WaitInvocation;

public class ThreadLockManagerImpl implements ThreadLockManager {

  private final ClientLockManager lockManager;
  private final ThreadLocal       threadID;
  private long                    threadIDSequence;

  public ThreadLockManagerImpl(ClientLockManager lockManager) {
    this.lockManager = lockManager;
    this.threadID = new ThreadLocal();
  }

  public LockID lockIDFor(String lockName) {
    return lockManager.lockIDFor(lockName);
  }

  public int queueLength(LockID lockID) {
    return lockManager.queueLength(lockID, getThreadID());
  }

  public int waitLength(LockID lockID) {
    return lockManager.waitLength(lockID, getThreadID());
  }

  public int heldCount(LockID lockID, int lockLevel) {
    return lockManager.heldCount(lockID, lockLevel, getThreadID());
  }

  public boolean isLocked(LockID lockID) {
    return lockManager.isLocked(lockID, getThreadID());
  }

  public void lock(LockID lockID, int lockLevel) {
    lockManager.lock(lockID, getThreadID(), lockLevel);
  }

  public boolean tryLock(LockID lockID, int lockLevel) {
    return lockManager.tryLock(lockID, getThreadID(), lockLevel);
  }

  public void wait(LockID lockID, WaitInvocation call, Object object, WaitListener waitListener) {
    lockManager.wait(lockID, getThreadID(), call, object, waitListener);
  }

  public Notify notify(LockID lockID, boolean all) {
    // XXX: HACK HACK HACK: this is here because notifies need to be attached to transactions.
    // this needs to be refactored. --Orion (10/26/05)
    return lockManager.notify(lockID, getThreadID(), all);
  }

  public void unlock(LockID lockID) {
    lockManager.unlock(lockID, getThreadID());
  }

  private ThreadID getThreadID() {
    ThreadID rv = (ThreadID) threadID.get();
    if (rv == null) {
      rv = new ThreadID(nextThreadID());
      threadID.set(rv);
    }

    return rv;
  }

  private synchronized long nextThreadID() {
    return ++threadIDSequence;
  }

}
