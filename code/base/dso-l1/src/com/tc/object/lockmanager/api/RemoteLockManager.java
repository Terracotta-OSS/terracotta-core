/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.object.lockmanager.api;

import com.tc.object.tx.TimerSpec;

import java.util.Collection;

/**
 * Representation of the remote lock manager
 * 
 * @author steve 
 */
public interface RemoteLockManager {  
  public void flush(LockID lockID);
  
  public void queryLock(LockID lockID, ThreadID threadID);
  
  public void interrruptWait(LockID lockID, ThreadID threadID);
  
  public void requestLock(LockID lockID, ThreadID threadID, int lockType, String lockObjectType);
  
  public void tryRequestLock(LockID lockID, ThreadID threadID, TimerSpec timeout, int lockType, String lockObjectType);

  public void releaseLock(LockID lockID, ThreadID threadID);
  
  public void releaseLockWait(LockID lockID, ThreadID threadID, TimerSpec call);

  public void recallCommit(LockID lockID, Collection lockContext, Collection waitContext, Collection pendingRequests, Collection pendingTryLockRequests);

  public boolean isTransactionsForLockFlushed(LockID lockID, LockFlushCallback callback);
}
