/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.object.lockmanager.api;

import com.tc.object.tx.TimerSpec;

public interface ThreadLockManager {

  public LockID lockIDFor(String lockName);

  public void lock(LockID lockID, int lockLevel, String lockObjectType, String contextInfo);
  
  public boolean tryLock(LockID lockID, TimerSpec timeout, int lockLevel, String lockObjectType);

  public void wait(LockID lockID, TimerSpec call, Object object, WaitListener waitListener) throws InterruptedException;

  public Notify notify(LockID lockID, boolean all);

  public void unlock(LockID lockID);

  public boolean isLocked(LockID lockID, int lockLevel);
  
  public int localHeldCount(LockID lockID, int lockLevel);
  
  public int queueLength(LockID lockId);
  
  public int waitLength(LockID lockId);
}
