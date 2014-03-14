/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.locks;

import com.tc.abortable.AbortedOperationException;
import com.tc.net.ClientID;
import com.tc.object.ClearableCallback;

import java.util.Collection;

public interface RemoteLockManager extends ClearableCallback {
  public ClientID getClientID();

  public void lock(LockID lock, ThreadID thread, ServerLockLevel level);

  public void tryLock(LockID lock, ThreadID thread, ServerLockLevel level, long timeout);

  public void unlock(LockID lock, ThreadID thread, ServerLockLevel level);

  public void wait(LockID lock, ThreadID thread, long waitTime);

  public void interrupt(LockID lock, ThreadID thread);

  public void recallCommit(LockID lock, Collection<ClientServerExchangeLockContext> lockState, boolean batch);

  public void flush(LockID lock) throws AbortedOperationException;

  public boolean asyncFlush(LockID lock, LockFlushCallback callback);

  public void query(LockID lock, ThreadID thread);

  public void waitForServerToReceiveTxnsForThisLock(LockID lock) throws AbortedOperationException;

  public void shutdown();

  public boolean isShutdown();

}
