/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.locks;

import com.tc.net.ClientID;

import java.util.Collection;

public interface RemoteLockManager {
  public ClientID getClientID();

  public void lock(LockID lock, ThreadID thread, ServerLockLevel level);

  public void tryLock(LockID lock, ThreadID thread, ServerLockLevel level, long timeout);

  public void unlock(LockID lock, ThreadID thread, ServerLockLevel level);

  public void wait(LockID lock, ThreadID thread, long waitTime);

  public void interrupt(LockID lock, ThreadID thread);

  public void recallCommit(LockID lock, Collection<ClientServerExchangeLockContext> lockState, boolean batch);

  public void flush(LockID lock, ServerLockLevel level);

  public boolean asyncFlush(LockID lock, LockFlushCallback callback, ServerLockLevel level);

  public void query(LockID lock, ThreadID thread);

  public void waitForServerToReceiveTxnsForThisLock(LockID lock);

  public void shutdown();

  public boolean isShutdown();
}
