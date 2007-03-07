/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.object.lockmanager.api;

import com.tc.object.tx.WaitInvocation;

public interface ThreadLockManager {

  public LockID lockIDFor(String lockName);

  public void lock(LockID lockID, int lockLevel);
  
  public boolean tryLock(LockID lockID, int lockLevel);

  public void wait(LockID lockID, WaitInvocation call, Object object, WaitListener waitListener) throws InterruptedException;

  public Notify notify(LockID lockID, boolean all);

  public void unlock(LockID lockID);

  public boolean isLocked(LockID lockID);
  
  public int localHeldCount(LockID lockID, int lockLevel);
  
  public int queueLength(LockID lockId);
  
  public int waitLength(LockID lockId);
}
