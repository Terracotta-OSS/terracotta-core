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
import com.tc.util.Assert;
import com.tc.util.concurrent.NoExceptionLinkedQueue;
import com.tc.util.concurrent.ThreadUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

/**
 * @author steve
 */
public class TestRemoteLockManager implements RemoteLockManager {
  public final LockResponder                   LOOPBACK_LOCK_RESPONDER = new LoopbackLockResponder();
  public final LockResponder                   NULL_LOCK_RESPONDER     = new LockResponder() {
                                                                         @Override
                                                                         public void respondToLockRequest(LockID lock,
                                                                                                          ThreadID thread,
                                                                                                          ServerLockLevel level) {
                                                                           return;
                                                                         }
                                                                       };
  private volatile ClientLockManager           lockManager;
  private final Map                            locks                   = new HashMap();
  private int                                  lockRequests            = 0;
  private int                                  unlockRequests          = 0;
  private int                                  flushCount              = 0;
  private boolean                              isGreedy                = false;
  public LockResponder                         lockResponder           = LOOPBACK_LOCK_RESPONDER;
  private final GroupID                        gid                     = new GroupID(0);

  public final NoExceptionLinkedQueue          lockRequestCalls        = new NoExceptionLinkedQueue();
  private final SessionProvider                sessionProvider;
  private boolean                              autoFlushLock           = true;
  private final Map<LockID, List<LockFlushCallback>> flushCallbacks          = new HashMap<LockID, List<LockFlushCallback>>();

  public TestRemoteLockManager(SessionProvider sessionProvider) {
    this.sessionProvider = sessionProvider;
  }

  @Override
  public void cleanup() {
    //
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

  @Override
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

  @Override
  public synchronized void unlock(LockID lockID, ThreadID threadID, ServerLockLevel level) {
    unlockRequests++;

    LinkedList myLocks = (LinkedList) locks.get(lockID);
    if (myLocks == null) return;
    Lock current = (Lock) myLocks.getFirst();
    if (current.threadID.equals(threadID) || isGreedy) {
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

  @Override
  public void wait(LockID lockID, ThreadID threadID, long timeout) {
    return;
  }

  @Override
  public void recallCommit(LockID lockID, Collection contexts, boolean batch) {
    // eng-422 batching has a race and should not be used
    Assert.assertFalse(batch);
    return;
  }

  @Override
  public synchronized void flush(LockID lockID) {
    flushCount++;
  }

  public synchronized void resetFlushCount() {
    flushCount = 0;
  }

  public synchronized int getFlushCount() {
    return flushCount;
  }

  @Override
  public boolean asyncFlush(LockID lockID, LockFlushCallback callback) {
    if (autoFlushLock) {
      return true;
    } else {
      List<LockFlushCallback> list = flushCallbacks.get(lockID);
      if (list == null) {
        list = new ArrayList<LockFlushCallback>();
        flushCallbacks.put(lockID, list);
      }
      list.add(callback);
      return false;
    }
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
    @Override
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

  @Override
  public void query(LockID lockID, ThreadID threadID) {
    throw new ImplementMe();
  }

  @Override
  public void interrupt(LockID lockID, ThreadID threadID) {
    //
  }

  @Override
  public void tryLock(final LockID lockID, final ThreadID threadID, final ServerLockLevel level, final long timeout) {
    Assert.assertEquals(0, timeout);
    Runnable runnable = new Runnable() {
      @Override
      public void run() {
        System.err.println("Sleeping in try lock for 30 seconds -- ");
        ThreadUtil.reallySleep(30000);
        lock(lockID, threadID, level);
      }
    };
    Thread t = new Thread(runnable);
    t.start();
  }

  @Override
  public ClientID getClientID() {
    return ClientID.NULL_ID;
  }

  @Override
  public void waitForServerToReceiveTxnsForThisLock(LockID lock) {
    flush(lock);
  }

  @Override
  public void shutdown() {
    //
  }

  @Override
  public boolean isShutdown() {
    return false;
  }

  public void setAutoFlushing(boolean autoFlush) {
    autoFlushLock = autoFlush;
  }

  public void flushPendingLocks() {
    for (Entry<LockID, List<LockFlushCallback>> entry : flushCallbacks.entrySet()) {
      LockID id = entry.getKey();
      for (LockFlushCallback callback : entry.getValue()) {
        callback.transactionsForLockFlushed(id);
      }
    }
    flushCallbacks.clear();
  }

}
