/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object.lockmanager.api;

import com.tc.exception.ImplementMe;
import com.tc.net.protocol.tcm.MessageChannel;
import com.tc.object.session.SessionProvider;
import com.tc.object.tx.TransactionID;
import com.tc.object.tx.WaitInvocation;
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
                                                                public void respondToLockRequest(LockRequest request) {
                                                                  return;
                                                                }
                                                              };
  private ClientLockManager           lockManager;
  private Map                         locks                   = new HashMap();
  private int                         lockRequests            = 0;
  private int                         unlockRequests          = 0;
  private int                         flushCount              = 0;
  private boolean                     isGreedy                = false;
  public LockResponder                lockResponder           = LOOPBACK_LOCK_RESPONDER;

  public final NoExceptionLinkedQueue lockRequestCalls        = new NoExceptionLinkedQueue();
  private final SessionProvider       sessionProvider;

  public TestRemoteLockManager(SessionProvider sessionProvider) {
    this.sessionProvider = sessionProvider;
  }

  public MessageChannel getChannel() {
    throw new ImplementMe();
  }

  public void setClientLockManager(ClientLockManager lockManager) {
    this.lockManager = lockManager;
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

  public synchronized void requestLock(LockID lockID, ThreadID threadID, int lockType) {
    lockRequests++;
    lockRequestCalls.put(new Object[] { lockID, threadID, new Integer(lockType) });

    LockRequest request;
    if (isGreedy) {
      request = new LockRequest(lockID, ThreadID.VM_ID, LockLevel.makeGreedy(lockType));
    } else {
      request = new LockRequest(lockID, threadID, lockType);
    }

    if (!locks.containsKey(lockID)) {
      locks.put(lockID, new LinkedList(Arrays.asList(new Object[] { new Lock(threadID, lockType) })));
      lockResponder.respondToLockRequest(request);
      return;
    }

    LinkedList myLocks = (LinkedList) locks.get(lockID);
    for (Iterator iter = myLocks.iterator(); iter.hasNext();) {
      // allow lock upgrades/downgrades
      Lock lock = (Lock) iter.next();
      if (lock.threadID.equals(threadID)) {
        lock.upCount();
        lockResponder.respondToLockRequest(request);
        return;
      }
    }

    myLocks.addLast(new Lock(request.threadID(), request.lockLevel()));
  }

  public synchronized void makeLocksGreedy() {
    isGreedy = true;
  }

  public synchronized void makeLocksNotGreedy() {
    isGreedy = false;
  }

  public synchronized void releaseLock(LockID lockID, ThreadID threadID) {
    unlockRequests++;

    LinkedList myLocks = (LinkedList) locks.get(lockID);

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
    lockResponder.respondToLockRequest(new LockRequest(lockID, lock.threadID, lock.type));
  }

  public void releaseLockWait(LockID lockID, ThreadID threadID, WaitInvocation call) {
    return;
  }

  public void recallCommit(LockID lockID, Collection lockContext, Collection waitContext, Collection pendingRequests, Collection pendingTryLockRequests) {
    return;
  }

  public synchronized void flush(LockID lockID) {
    flushCount++;
  }

  public synchronized void resetFlushCount() {
    flushCount = 0;
  }

  public synchronized int getFlushCount() {
    return flushCount;
  }

  public boolean isTransactionsForLockFlushed(LockID lockID, LockFlushCallback callback) {
    return true;
  }

  public void notify(LockID lockID, TransactionID transactionID, boolean all) {
    // TODO: this really should get called by a test at some point. At such
    // time, you probably want to record the
    // request and return
    throw new ImplementMe();
  }

  public interface LockResponder {
    void respondToLockRequest(LockRequest request);
  }

  private class LoopbackLockResponder implements LockResponder {
    public void respondToLockRequest(LockRequest request) {
      lockManager.awardLock(sessionProvider.getSessionID(), request.lockID(), request.threadID(), request.lockLevel());
    }
  }

  private static class Lock {
    final ThreadID threadID;
    final int      type;
    int            count = 1;

    Lock(ThreadID threadID, int type) {
      this.threadID = threadID;
      this.type = type;
    }

    int upCount() {
      return ++count;
    }

    int downCount() {
      return --count;
    }
  }

  public void queryLock(LockID lockID, ThreadID threadID) {
    throw new ImplementMe();
  }

  public void interrruptWait(LockID lockID, ThreadID threadID) {
    throw new ImplementMe();
  }

  public void tryRequestLock(LockID lockID, ThreadID threadID, WaitInvocation timeout, int lockType) {
    //
  }
}
