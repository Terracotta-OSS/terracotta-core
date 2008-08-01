/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object.lockmanager.impl;

import com.tc.object.lockmanager.api.ClientLockManager;
import com.tc.object.lockmanager.api.LockID;
import com.tc.object.lockmanager.api.Notify;
import com.tc.object.lockmanager.api.ThreadID;
import com.tc.object.lockmanager.api.ThreadLockManager;
import com.tc.object.lockmanager.api.WaitListener;
import com.tc.object.tx.TimerSpec;
import com.tc.util.runtime.NullThreadIDMap;
import com.tc.util.runtime.ThreadIDMap;

public class ThreadLockManagerImpl implements ThreadLockManager {

  private final ClientLockManager lockManager;
  private final ThreadLocal       threadID;
  private long                    threadIDSequence;
  private final ThreadIDMap       threadIDMap;

  public ThreadLockManagerImpl(ClientLockManager lockManager) {
    this(lockManager, new NullThreadIDMap());
  }

  public ThreadLockManagerImpl(ClientLockManager lockManager, ThreadIDMap thMap) {
    this.lockManager = lockManager;
    this.threadID = new ThreadLocal();
    this.threadIDMap = thMap;
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

  public int localHeldCount(LockID lockID, int lockLevel) {
    return lockManager.localHeldCount(lockID, lockLevel, getThreadID());
  }

  public boolean isLocked(LockID lockID, int lockLevel) {
    return lockManager.isLocked(lockID, getThreadID(), lockLevel);
  }

  public void lock(LockID lockID, int lockLevel, String lockObjectType, String contextInfo) {
    lockManager.lock(lockID, getThreadID(), lockLevel, lockObjectType, contextInfo);
  }

  public boolean tryLock(LockID lockID, TimerSpec timeout, int lockLevel, String lockObjectType) {
    return lockManager.tryLock(lockID, getThreadID(), timeout, lockLevel, lockObjectType);
  }

  public void wait(LockID lockID, TimerSpec call, Object object, WaitListener waitListener) throws InterruptedException {
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
      rv = new ThreadID(nextThreadID(), Thread.currentThread().getName());
      threadIDMap.addTCThreadID(rv);
      threadID.set(rv);
    }
    return rv;
  }

  private synchronized long nextThreadID() {
    return ++threadIDSequence;
  }

}
