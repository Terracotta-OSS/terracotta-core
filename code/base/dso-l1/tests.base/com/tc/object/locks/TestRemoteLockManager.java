/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object.locks;

import com.tc.exception.ImplementMe;
import com.tc.net.ClientID;
import com.tc.net.GroupID;
import com.tc.object.session.SessionProvider;
import com.tc.object.tx.TransactionID;
import com.tc.util.concurrent.NoExceptionLinkedQueue;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;

/**
 * @author steve
 */
public class TestRemoteLockManager implements RemoteLockManager {
  public final LockResponder          LOOPBACK_LOCK_RESPONDER = new LoopbackLockResponder();
  public final LockResponder          NULL_LOCK_RESPONDER     = new LockResponder() {
                                                                public void respondToLockRequest(LockID lock,
                                                                                                 ThreadID thread,
                                                                                                 ServerLockLevel level) {
                                                                  return;
                                                                }
                                                              };
  private volatile ClientLockManager  lockManager;
  private final Map                   locks                   = new HashMap();
  private int                         lockRequests            = 0;
  private int                         unlockRequests          = 0;
  private int                         flushCount              = 0;
  private boolean                     isGreedy                = false;
  public LockResponder                lockResponder           = LOOPBACK_LOCK_RESPONDER;
  private final GroupID               gid                     = new GroupID(0);

  public final NoExceptionLinkedQueue lockRequestCalls        = new NoExceptionLinkedQueue();
  private final SessionProvider       sessionProvider;

  public TestRemoteLockManager(SessionProvider sessionProvider) {
    this.sessionProvider = sessionProvider;
  }

  public void setClientLockManager(ClientLockManager lockManager) {
    this.lockManager = lockManager;
  }

  public ClientLockManager getClientLockManager() {
    return lockManager;
  }

  public synchronized int getLockRequestCount() {
    return this.lockRequests;
  }

  public synchronized int getUnlockRequestCount() {
    return this.unlockRequests;
  }

  // *************************
  // This manager doesn't implement lock upgrades correctly. Lock
  // upgrades/downgrades are always awarded. Locks held
  // by other transactions are not taken into consideration
  // *************************

  public synchronized void lock(LockID lockID, ThreadID threadID, ServerLockLevel level) {
    lockRequests++;
    lockRequestCalls.put(new Object[] { lockID, threadID, level });

    if (isGreedy) {
      threadID = ThreadID.VM_ID;
    }

    if (!locks.containsKey(lockID)) {
      locks.put(lockID, new LinkedList(Arrays.asList(new Object[] { new Lock(threadID, level) })));
      lockResponder.respondToLockRequest(lockID, threadID, level);
      return;
    }

    LinkedList myLocks = (LinkedList) locks.get(lockID);
    for (Iterator iter = myLocks.iterator(); iter.hasNext();) {
      // allow lock upgrades/downgrades
      Lock lock = (Lock) iter.next();
      if (lock.threadID.equals(threadID)) {
        lock.upCount();
        lockResponder.respondToLockRequest(lockID, threadID, level);
        return;
      }
    }

    myLocks.addLast(new Lock(threadID, level));
  }

  public synchronized void makeLocksGreedy() {
    isGreedy = true;
  }

  public synchronized void makeLocksNotGreedy() {
    isGreedy = false;
  }

  public synchronized void unlock(LockID lockID, ThreadID threadID, ServerLockLevel level) {
    unlockRequests++;

    LinkedList myLocks = (LinkedList) locks.get(lockID);

    if (myLocks == null) return;

    Lock current = (Lock) myLocks.getFirst();
    if (current.threadID.equals(threadID)) {
      int count = current.downCount();
      if (count == 0) {
        myLocks.removeFirst();
      } else {
        return;
      }
    }

    if (myLocks.isEmpty()) {
      locks.remove(lockID);
      return;
    }

    Lock lock = (Lock) myLocks.remove(0);
    lockResponder.respondToLockRequest(lockID, lock.threadID, lock.level);
  }

  public void wait(LockID lockID, ThreadID threadID, long timeout) {
    return;
  }

  public void recallCommit(LockID lockID, Collection contexts, boolean batch) {
    return;
  }

  public synchronized void flush(LockID lockID, ServerLockLevel level) {
    flushCount++;
  }

  public synchronized void resetFlushCount() {
    flushCount = 0;
  }

  public synchronized int getFlushCount() {
    return flushCount;
  }

  public boolean asyncFlush(LockID lockID, LockFlushCallback callback, ServerLockLevel level) {
    return true;
  }

  public void notify(LockID lockID, TransactionID transactionID, boolean all) {
    // TODO: this really should get called by a test at some point. At such
    // time, you probably want to record the
    // request and return
    throw new ImplementMe();
  }

  public interface LockResponder {
    void respondToLockRequest(LockID lock, ThreadID thread, ServerLockLevel level);
  }

  private class LoopbackLockResponder implements LockResponder {
    public void respondToLockRequest(final LockID lock, final ThreadID thread, final ServerLockLevel level) {
      new Thread() {
        @Override
        public void run() {
          lockManager.award(gid, sessionProvider.getSessionID(gid), lock, thread, level);
        }
      }.start();
    }
  }

  private static class Lock {
    final ThreadID        threadID;
    final ServerLockLevel level;
    int                   count = 1;

    Lock(ThreadID threadID, ServerLockLevel level) {
      this.threadID = threadID;
      this.level = level;
    }

    int upCount() {
      return ++count;
    }

    int downCount() {
      return --count;
    }
  }

  public void query(LockID lockID, ThreadID threadID) {
    throw new ImplementMe();
  }

  public void interrupt(LockID lockID, ThreadID threadID) {
    //
  }

  public void tryLock(LockID lockID, ThreadID threadID, ServerLockLevel level, long timeout) {
    //
  }

  public ClientID getClientID() {
    return ClientID.NULL_ID;
  }

  public void waitForServerToReceiveTxnsForThisLock(LockID lock) {
    flush(lock, ServerLockLevel.WRITE);
  }

  public void shutdown() {
    //
  }

  public boolean isShutdown() {
    return false;
  }

}
