/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.locks;

import com.tc.exception.TCLockUpgradeNotSupportedError;
import com.tc.net.ClientID;
import com.tc.object.locks.LockStateNode.LockHold;
import com.tc.object.locks.LockStateNode.LockWaiter;
import com.tc.object.locks.LockStateNode.PendingLockHold;
import com.tc.object.locks.LockStateNode.PendingTryLockHold;
import com.tc.object.msg.ClientHandshakeMessage;
import com.tc.util.SynchronizedSinglyLinkedList;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

class ClientLockImpl extends SynchronizedSinglyLinkedList<LockStateNode> implements ClientLock {

  private static final Set<LockLevel> WRITE_LEVELS  = EnumSet.of(LockLevel.WRITE, LockLevel.SYNCHRONOUS_WRITE);
  private static final Set<LockLevel> READ_LEVELS   = EnumSet.of(LockLevel.READ);

  private static final int            BLOCKING_LOCK = Integer.MIN_VALUE;

  private final LockID                lock;

  private ClientGreediness            greediness    = ClientGreediness.FREE;

  private volatile byte               gcCycleCount  = 0;
  private volatile boolean            pinned;

  public ClientLockImpl(LockID lock) {
    this.lock = lock;
  }

  /*
   * Try to acquire this lock locally - if successful then return, otherwise queue the request and potentially call out
   * to the server.
   */
  public void lock(RemoteLockManager remote, ThreadID thread, LockLevel level) throws GarbageLockException {
    markUsed();
    if (!tryAcquireLocally(thread, level).isSuccess()) {
      acquireQueued(remote, thread, level);
    }
  }

  /*
   * Try to acquire this lock locally - if successful then return, otherwise queue the request and potentially call out
   * to the server
   */
  public void lockInterruptibly(RemoteLockManager remote, ThreadID thread, LockLevel level)
      throws InterruptedException, GarbageLockException {
    markUsed();
    if (Thread.interrupted()) { throw new InterruptedException(); }
    if (!tryAcquireLocally(thread, level).isSuccess()) {
      acquireQueuedInterruptibly(remote, thread, level);
    }
  }

  /*
   * Try lock would normally just be: <code>return tryAcquire(remote, thread, level, 0).isSuccess();</code> <p> However
   * because the existing contract on tryLock requires us to wait for the server if the lock attempt is delegated things
   * get a little more complicated.
   */
  public boolean tryLock(RemoteLockManager remote, ThreadID thread, LockLevel level) throws GarbageLockException {
    markUsed();
    LockAcquireResult result = tryAcquireLocally(thread, level);
    if (result.isKnownResult()) {
      return result.isSuccess();
    } else {
      try {
        return acquireQueuedTimeout(remote, thread, level, 0);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        return false;
      }
    }
  }

  /*
   * Try to acquire locally - if we fail then queue the request and defer to the server.
   */
  public boolean tryLock(RemoteLockManager remote, ThreadID thread, LockLevel level, long timeout)
      throws InterruptedException, GarbageLockException {
    markUsed();
    if (Thread.interrupted()) { throw new InterruptedException(); }
    return tryAcquireLocally(thread, level).isSuccess() || acquireQueuedTimeout(remote, thread, level, timeout);
  }

  /*
   * Release the lock and unpark an acquire if release tells us that queued acquires may now succeed.
   */
  public void unlock(RemoteLockManager remote, ThreadID thread, LockLevel level) {
    markUsed();
    if (release(remote, thread, level)) {
      unparkFirstQueuedAcquire();
    }
  }

  /*
   * Find a lock waiter in the state and unpark it - while concurrently checking for a write hold by the notifying
   * thread
   */
  public boolean notify(RemoteLockManager remote, ThreadID thread, Object waitObject) {
    markUsed();
    return notify(thread, false);
  }

  /*
   * Find all the lock waiters in the state and unpark them.
   */
  public boolean notifyAll(RemoteLockManager remote, ThreadID thread, Object waitObject) {
    markUsed();
    return notify(thread, true);
  }

  private boolean notify(ThreadID thread, boolean all) {
    boolean result;
    Collection<LockWaiter> waiters = new ArrayList<LockWaiter>();

    synchronized (this) {
      if (!isLockedBy(thread, WRITE_LEVELS)) { throw new IllegalMonitorStateException(); }

      if (greediness.isFree()) {
        // other L1s may be waiting (let server decide who to notify)
        result = true;
      } else {
        for (Iterator<LockStateNode> it = iterator(); it.hasNext();) {
          LockStateNode s = it.next();
          if (s instanceof LockWaiter) {
            it.remove();
            // move this waiters reacquire nodes into the queue - we must do this before returning to ensure
            // transactional correctness on notifies.
            waiters.add((LockWaiter) s);
            addPendingAcquires((LockWaiter) s);
            if (!all) {
              result = false;
              break;
            }
          }
        }
        result = true;
      }
    }

    for (LockWaiter waiter : waiters) {
      waiter.unpark();
    }

    return result;
  }

  public void wait(RemoteLockManager remote, WaitListener listener, ThreadID thread, Object waitObject)
      throws InterruptedException {
    wait(remote, listener, thread, waitObject, 0);
  }

  /*
   * Waiting involves unlocking all the write lock holds, sleeping on the original condition, until wake up, and then
   * re-acquiring the original locks in their original order. This code is extraordinarily sensitive to the order of
   * operations...
   */
  public void wait(RemoteLockManager remote, WaitListener listener, ThreadID thread, Object waitObject, long timeout)
      throws InterruptedException {
    markUsed();
    if (Thread.interrupted()) { throw new InterruptedException(); }

    if (!isLockedBy(thread, WRITE_LEVELS)) { throw new IllegalMonitorStateException(); }

    LockWaiter waiter = null;
    try {
      boolean flush;
      synchronized (this) {
        flush = flushOnUnlockAll(thread);
        if (!flush) {
          waiter = releaseAllAndPushWaiter(remote, thread, waitObject, timeout);
        }
      }

      if (flush) {
        remote.flush(lock, greediness.getFlushLevel());
        waiter = releaseAllAndPushWaiter(remote, thread, waitObject, timeout);
      }

      unparkFirstQueuedAcquire();
      waitOnLockWaiter(remote, thread, waiter, listener);
    } finally {
      moveWaiterToPending(waiter);
      acquireAll(remote, thread, waiter.getReacquires());
    }
  }

  private synchronized LockWaiter releaseAllAndPushWaiter(RemoteLockManager remote, ThreadID thread, Object waitObject,
                                                          long timeout) {
    Stack<LockHold> holds = releaseAll(remote, thread);
    LockWaiter waiter = new LockWaiter(thread, waitObject, holds, timeout);
    addLast(waiter);

    if (greediness.isFree()) {
      remote.wait(lock, thread, timeout);
    } else if (greediness.isRecalled() && canRecallNow()) {
      greediness = recallCommit(remote);
    }

    return waiter;
  }

  private synchronized Stack<LockHold> releaseAll(RemoteLockManager remote, ThreadID thread) {
    Stack<LockHold> holds = new Stack<LockHold>();
    for (Iterator<LockStateNode> it = iterator(); it.hasNext();) {
      LockStateNode node = it.next();
      if ((node instanceof LockHold) && node.getOwner().equals(thread)) {
        it.remove();
        holds.push((LockHold) node);
      }
    }
    return holds;
  }

  private void waitOnLockWaiter(RemoteLockManager remote, ThreadID thread, LockWaiter waiter, WaitListener listener)
      throws InterruptedException {
    listener.handleWaitEvent();
    try {
      if (waiter.getTimeout() == 0) {
        waiter.park();
      } else {
        waiter.park(waiter.getTimeout());
      }
    } catch (InterruptedException e) {
      synchronized (this) {
        if (greediness.isFree()) {
          remote.interrupt(lock, thread);
        }
        moveWaiterToPending(waiter);
      }
      throw e;
    }
  }

  private void acquireAll(RemoteLockManager remote, ThreadID thread, Stack<PendingLockHold> acquires) {
    while (!acquires.isEmpty()) {
      PendingLockHold qa = acquires.pop();
      try {
        acquireQueued(remote, thread, qa.getLockLevel(), qa);
      } catch (GarbageLockException e) {
        throw new AssertionError("GarbageLockException thrown while reacquiring locks after wait");
      }
    }
  }

  public synchronized Collection<ClientServerExchangeLockContext> getStateSnapshot(ClientID client) {
    Collection<ClientServerExchangeLockContext> contexts = new ArrayList<ClientServerExchangeLockContext>();

    switch (greediness) {
      case GARBAGE:
        break;
      default:
        ClientServerExchangeLockContext c = greediness.toContext(lock, client);
        if (c != null) contexts.add(c);
    }

    for (LockStateNode s : this) {
      ClientServerExchangeLockContext c = s.toContext(lock, client);
      if (c != null) contexts.add(c);
    }

    return contexts;
  }

  public synchronized int pendingCount() {
    int penders = 0;
    for (LockStateNode s : this) {
      if (s instanceof PendingLockHold) {
        penders++;
      }
    }
    return penders;
  }

  public synchronized int waitingCount() {
    int waiters = 0;
    for (LockStateNode s : this) {
      if (s instanceof LockWaiter) {
        waiters++;
      }
    }
    return waiters;
  }

  public synchronized boolean isLocked(LockLevel level) {
    for (LockStateNode s : this) {
      if ((s instanceof LockHold) && (((LockHold) s).getLockLevel().equals(level))) { return true; }
    }
    return false;
  }

  public synchronized boolean isLockedBy(ThreadID thread, LockLevel level) {
    for (LockStateNode s : this) {
      if ((s instanceof LockHold) && (((LockHold) s).getLockLevel().equals(level) || (level == null))
          && s.getOwner().equals(thread)) { return true; }
    }
    return false;
  }

  public synchronized boolean isLockedBy(ThreadID thread, Set<LockLevel> levels) {
    for (LockStateNode s : this) {
      if ((s instanceof LockHold) && s.getOwner().equals(thread) && levels.contains(((LockHold) s).getLockLevel())) { return true; }
    }
    return false;
  }

  public synchronized int holdCount(LockLevel level) {
    int holders = 0;
    for (LockStateNode s : this) {
      if ((s instanceof LockHold) && ((LockHold) s).getLockLevel().equals(level)) {
        holders++;
      } else if (s instanceof LockWaiter) {
        break;
      } else if (s instanceof PendingLockHold) {
        break;
      }
    }
    return holders;
  }

  public void pinLock() {
    pinned = true;
  }

  public void unpinLock() {
    pinned = false;
  }

  /*
   * Called by the stage thread (the transaction apply thread) when the server wishes to notify a thread waiting on this
   * lock
   */
  public void notified(ThreadID thread) {
    LockWaiter waiter = null;
    synchronized (this) {
      for (Iterator<LockStateNode> it = iterator(); it.hasNext();) {
        LockStateNode s = it.next();
        if ((s instanceof LockWaiter) && s.getOwner().equals(thread)) {
          it.remove();
          // move the waiting nodes reacquires into the queue in this thread so we can be certain that the lock state
          // has changed by the time the server gets the txn ack.
          waiter = (LockWaiter) s;
          addPendingAcquires(waiter);
          break;
        }
      }
    }

    if (waiter != null) {
      waiter.unpark();
    }
  }

  /*
   * Move the given waiters reacquire nodes into the queue
   */
  private synchronized void moveWaiterToPending(LockWaiter waiter) {
    if ((waiter != null) && (remove(waiter) != null)) {
      addPendingAcquires(waiter);
    }
  }

  private synchronized void addPendingAcquires(LockWaiter waiter) {
    for (PendingLockHold reacquire : waiter.getReacquires()) {
      addLast(reacquire);
    }
  }

  /**
   * ClientLockImpl ignores the interest level of the recall request. Instead it will always recall the lock as long as
   * there are no held write locks locally (and assuming we don't decide to lease it). This gives us the benefit of not
   * blocking future read recalls if a write recall is pending. This can be a problem when the write recall was
   * triggered by a tryLock that no longer requires the lock.
   */
  public synchronized boolean recall(final RemoteLockManager remote, final ServerLockLevel interest, int lease) {
    // transition the greediness state
    greediness = greediness.recalled(this, lease, interest);

    if (greediness.isRecalled()) {
      greediness = doRecall(remote);
      return false;
    } else if (greediness.isGreedy()) {
      return true;
    } else {
      return false;
    }
  }

  /*
   * Called by the stage thread to indicate that the tryLock attempt has failed.
   */
  public void refuse(ThreadID thread, ServerLockLevel level) {
    PendingLockHold acquire;
    synchronized (this) {
      acquire = getQueuedAcquire(thread, level);
      if (acquire != null) {
        acquire.refused();
      }
    }

    if (acquire != null) {
      acquire.unpark();
    }
  }

  /*
   * Called by the stage thread when the server has awarded a lock (either greedy or per thread).
   */
  public void award(RemoteLockManager remote, ThreadID thread, ServerLockLevel level) throws GarbageLockException {
    if (ThreadID.VM_ID.equals(thread)) {
      synchronized (this) {
        greediness = greediness.awarded(level);
      }
      unparkFirstQueuedAcquire();
    } else {
      PendingLockHold acquire;
      synchronized (this) {
        acquire = getQueuedAcquire(thread, level);
        if (acquire == null) {
          remote.unlock(lock, thread, level);
        } else {
          acquire.awarded();
        }
      }

      if (acquire != null) {
        acquire.unpark();
      }
    }
  }

  /**
   * Our locks behave in a slightly bizarre way - we don't queue very strictly, if the head of the acquire queue fails,
   * we allow acquires further down to succeed. This is different to the JDK RRWL - suspect this is a historical
   * accident. I'm currently experimenting with a more strict queuing policy to see if it can pass all our tests
   */
  static enum LockAcquireResult {
    /**
     * Acquire succeeded - other threads may succeed now too.
     */
    SHARED_SUCCESS,
    /**
     * Acquire succeeded - other threads will fail in acquire
     */
    SUCCESS,
    /**
     * Acquire was refused - other threads might succeed though.
     */
    FAILURE,
    /**
     * Acquire was delegated to the server - used by tryLock.
     */
    USED_SERVER,
    /**
     * Unknown
     */
    UNKNOWN;

    public boolean isShared() {
      // because of our loose queuing everything except a exclusive acquire is `shared'
      return this != SUCCESS;
      // return this == SUCCEEDED_SHARED;
    }

    public boolean isSuccess() {
      return (this == SUCCESS) | (this == SHARED_SUCCESS);
    }

    public boolean isFailure() {
      return this == FAILURE;
    }

    public boolean usedServer() {
      return this == USED_SERVER;
    }

    public boolean isKnownResult() {
      return isSuccess() || isFailure();
    }
  }

  /*
   * Try to acquire the lock (optionally with delegation to the server)
   */
  private LockAcquireResult tryAcquire(RemoteLockManager remote, ThreadID thread, LockLevel level, long timeout,
                                       PendingLockHold node) throws GarbageLockException {
    // try to do things locally first...
    LockAcquireResult result = tryAcquireLocally(thread, level);
    if (result.isKnownResult()) {
      return result;
    } else {
      synchronized (this) {
        if (!node.canDelegate()) {
          // no server delegation - just return local result
          return result;
        } else {
          // delegate to server
          ServerLockLevel requestLevel = ServerLockLevel.fromClientLockLevel(level);
          greediness = greediness.requested(requestLevel);
          if (greediness.isFree()) {
            switch ((int) timeout) {
              case ClientLockImpl.BLOCKING_LOCK:
                remote.lock(lock, thread, requestLevel);
                node.delegated("Called remote.lock(...)...");
                break;
              default:
                remote.tryLock(lock, thread, requestLevel, timeout);
                node.delegated("Called remote.tryLock(...)...");
                break;
            }
            return LockAcquireResult.USED_SERVER;
          } else if (greediness.isRecalled()) {
            // drop through to trigger recall
          } else {
            node.delegated("Waiting For Recall...");
            return LockAcquireResult.USED_SERVER;
          }
        }
      }

      remote.flush(lock, greediness.getFlushLevel());

      synchronized (this) {
        if (greediness.isRecalled() && canRecallNow()) {
          greediness = recallCommit(remote);
        }
        node.delegated("Waiting For Recall...");
        return LockAcquireResult.USED_SERVER;
      }
    }
  }

  /*
   * Attempt to acquire the lock at the given level locally
   */
  private LockAcquireResult tryAcquireLocally(ThreadID thread, LockLevel level) throws GarbageLockException {
    // if this is a concurrent acquire then just let it through.
    if (level == LockLevel.CONCURRENT) { return LockAcquireResult.SHARED_SUCCESS; }

    synchronized (this) {
      LockAcquireResult result = tryAcquireUsingThreadState(thread, level);
      if (result.isKnownResult()) {
        return result;
      } else if (greediness.canAward(level)) {
        addFirst(new LockHold(thread, level));
        return level.isWrite() ? LockAcquireResult.SUCCESS : LockAcquireResult.SHARED_SUCCESS;
      } else {
        return LockAcquireResult.UNKNOWN;
      }
    }
  }

  private LockAcquireResult tryAcquireUsingThreadState(ThreadID thread, LockLevel level) {
    // What can we glean from local lock state
    LockHold newHold = new LockHold(thread, level);
    for (Iterator<LockStateNode> it = iterator(); it.hasNext();) {
      LockStateNode s = it.next();
      LockAcquireResult result = s.allowsHold(newHold);
      if (result.isKnownResult()) {
        if (result.isSuccess()) {
          addFirst(newHold);
        } else {
          // Lock upgrade not supported check
          if (level.isWrite() && isLockedBy(thread, READ_LEVELS)) { throw new TCLockUpgradeNotSupportedError(); }
        }
        return result;
      }
    }

    // Lock upgrade not supported check
    if (level.isWrite() && isLockedBy(thread, READ_LEVELS)) { throw new TCLockUpgradeNotSupportedError(); }

    return LockAcquireResult.UNKNOWN;
  }

  /*
   * Unlock and return true if acquires might now succeed.
   */
  private boolean release(RemoteLockManager remote, ThreadID thread, LockLevel level) {
    // concurrent unlocks are implicitly okay - we don't monitor concurrent locks
    if (level == LockLevel.CONCURRENT) {
      // concurrent unlocks do not change the state - no reason why queued acquires would succeed
      return false;
    }

    LockHold unlock = null;
    synchronized (this) {
      for (Iterator<LockStateNode> it = iterator(); it.hasNext();) {
        LockStateNode s = it.next();
        if (s instanceof LockHold) {
          LockHold hold = (LockHold) s;
          if (hold.getOwner().equals(thread) && hold.getLockLevel().equals(level)) {
            unlock = hold;
            break;
          }
        }
      }

      if (unlock == null) { throw new IllegalMonitorStateException(); }

      if (!unlock.getLockLevel().isSyncWrite() && !flushOnUnlock(unlock)) { return release(remote, unlock); }
    }

    if (unlock.getLockLevel().isSyncWrite()) {
      // wait for the server to receive all transactions for this lock
      remote.waitForServerToReceiveTxnsForThisLock(lock);
    }

    if (flushOnUnlock(unlock)) {
      remote.flush(lock, greediness.getFlushLevel());
    }
    return release(remote, unlock);
  }

  private synchronized boolean release(RemoteLockManager remote, LockHold unlock) {
    remove(unlock);
    if (greediness.isFree()) {
      remoteUnlock(remote, unlock);
    } else if (greediness.isRecalled() && canRecallNow()) {
      greediness = recallCommit(remote);
    }

    // this is wrong - but shouldn't break anything
    return true;
  }

  private void remoteUnlock(RemoteLockManager remote, LockHold unlock) {
    for (LockStateNode s : this) {
      if (s == unlock) continue;

      if (s instanceof LockHold && s.getOwner().equals(unlock.getOwner())) {
        LockHold hold = (LockHold) s;
        if (unlock.getLockLevel().isWrite()) {
          if (hold.getLockLevel().isWrite()) { return; }
        } else {
          return;
        }
      }
    }

    remote.unlock(lock, unlock.getOwner(), ServerLockLevel.fromClientLockLevel(unlock.getLockLevel()));
  }

  private synchronized boolean flushOnUnlock(LockHold unlock) {
    if (!greediness.flushOnUnlock()) { return false; }

    for (LockStateNode s : this) {
      if (s == unlock) continue;

      if (s instanceof LockHold && s.getOwner().equals(unlock.getOwner())) {
        if (((LockHold) s).getLockLevel().isWrite()) return false;
        if (unlock.getLockLevel().isRead()) return false;
      }
    }
    return true;
  }

  private synchronized boolean flushOnUnlockAll(ThreadID thread) {
    if (greediness.flushOnUnlock()) { return true; }

    for (LockStateNode s : this) {
      if (s instanceof LockHold && s.getOwner().equals(thread)) {
        if (((LockHold) s).getLockLevel().isSyncWrite()) return true;
      }
    }
    return false;
  }

  /*
   * Conventional acquire queued - uses a LockSupport based queue object.
   */
  private void acquireQueued(RemoteLockManager remote, ThreadID thread, LockLevel level) throws GarbageLockException {
    final PendingLockHold node = new PendingLockHold(thread, level);
    addLast(node);
    acquireQueued(remote, thread, level, node);
  }

  /*
   * Generic acquire - uses an already existing queued node - used during wait notify
   */
  private void acquireQueued(RemoteLockManager remote, ThreadID thread, LockLevel level, PendingLockHold node)
      throws GarbageLockException {
    boolean interrupted = false;
    try {
      for (;;) {
        // try to acquire before sleeping
        LockAcquireResult result = tryAcquire(remote, thread, level, BLOCKING_LOCK, node);
        if (result.isShared()) {
          unparkNextQueuedAcquire(node);
        }
        if (result.isSuccess()) {
          remove(node);
          return;
        }

        // park the thread and wait for unpark
        node.park();
        if (Thread.interrupted()) {
          interrupted = true;
        }
      }
    } catch (RuntimeException ex) {
      abortAndRemove(remote, node);
      unparkFirstQueuedAcquire();
      throw ex;
    } catch (TCLockUpgradeNotSupportedError e) {
      abortAndRemove(remote, node);
      unparkFirstQueuedAcquire();
      throw e;
    } finally {
      if (interrupted) {
        Thread.currentThread().interrupt();
      }
    }
  }

  /*
   * Just like acquireQueued but throws InterruptedException if unparked by interrupt rather then saving the interrupt
   * state
   */
  private void acquireQueuedInterruptibly(RemoteLockManager remote, ThreadID thread, LockLevel level)
      throws InterruptedException, GarbageLockException {
    final PendingLockHold node = new PendingLockHold(thread, level);
    addLast(node);
    try {
      for (;;) {
        LockAcquireResult result = tryAcquire(remote, thread, level, BLOCKING_LOCK, node);
        if (result.isShared()) {
          unparkNextQueuedAcquire(node);
        }
        if (result.isSuccess()) {
          remove(node);
          return;
        }

        node.park();
        if (Thread.interrupted()) {
          break;
        }
      }
    } catch (RuntimeException ex) {
      abortAndRemove(remote, node);
      unparkFirstQueuedAcquire();
      throw ex;
    } catch (TCLockUpgradeNotSupportedError e) {
      abortAndRemove(remote, node);
      unparkFirstQueuedAcquire();
      throw e;
    }
    // Arrive here only if interrupted
    abortAndRemove(remote, node);
    throw new InterruptedException();
  }

  /*
   * Acquire queued - waiting for at most timeout milliseconds.
   */
  private boolean acquireQueuedTimeout(RemoteLockManager remote, ThreadID thread, LockLevel level, long timeout)
      throws InterruptedException, GarbageLockException {
    long lastTime = System.currentTimeMillis();
    final PendingTryLockHold node = new PendingTryLockHold(thread, level, timeout);
    addLast(node);
    try {
      while (!node.isRefused()) {
        LockAcquireResult result = tryAcquire(remote, thread, level, timeout, node);
        if (result.isShared()) {
          unparkNextQueuedAcquire(node);
        }
        if (result.isSuccess()) {
          remove(node);
          return true;
        } else {
          if (node.canDelegate() && timeout <= 0) {
            abortAndRemove(remote, node);
            return false;
          }
        }

        if (!node.canDelegate()) {
          node.park();
        } else {
          node.park(timeout);
        }
        if (Thread.interrupted()) {
          abortAndRemove(remote, node);
          throw new InterruptedException();
        }
        long now = System.currentTimeMillis();
        timeout -= now - lastTime;
        // possibility of changing node timeout here...
        lastTime = now;
      }
      remove(node);
      LockAcquireResult result = tryAcquireLocally(thread, level);
      if (result.isShared()) {
        unparkFirstQueuedAcquire();
      }
      return result.isSuccess();
    } catch (RuntimeException ex) {
      abortAndRemove(remote, node);
      unparkFirstQueuedAcquire();
      throw ex;
    } catch (TCLockUpgradeNotSupportedError e) {
      abortAndRemove(remote, node);
      unparkFirstQueuedAcquire();
      throw e;
    }
  }

  private synchronized void abortAndRemove(RemoteLockManager remote, PendingLockHold node) {
    node = (PendingLockHold) remove(node);
    if (node != null && node.isAwarded()) {
      remote.unlock(lock, node.getOwner(), ServerLockLevel.fromClientLockLevel(node.getLockLevel()));
    }
  }

  /*
   * Unpark the first queued acquire
   */
  private void unparkFirstQueuedAcquire() {
    PendingLockHold firstAcquire = getFirstQueuedAcquire();
    if (firstAcquire != null) {
      firstAcquire.unpark();
    }
  }

  /*
   * Unpark the next queued acquire (after supplied node)
   */
  private void unparkNextQueuedAcquire(LockStateNode node) {
    PendingLockHold nextAcquire = getNextQueuedAcquire(node);
    if (nextAcquire != null) {
      nextAcquire.unpark();
    }
  }

  private synchronized PendingLockHold getFirstQueuedAcquire() {
    for (LockStateNode current : this) {
      if (current instanceof PendingLockHold) { return (PendingLockHold) current; }
    }
    return null;
  }

  private synchronized PendingLockHold getNextQueuedAcquire(LockStateNode node) {
    LockStateNode current = node.getNext();
    while (current != null) {
      if (current instanceof PendingLockHold) { return (PendingLockHold) current; }
      current = current.getNext();
    }
    return null;
  }

  private synchronized PendingLockHold getQueuedAcquire(ThreadID thread, ServerLockLevel level) {
    for (LockStateNode s : this) {
      if ((s instanceof PendingLockHold) && s.getOwner().equals(thread)
          && level.equals(ServerLockLevel.fromClientLockLevel(((PendingLockHold) s).getLockLevel()))) { return (PendingLockHold) s; }
    }
    return null;
  }

  private synchronized ClientGreediness doRecall(final RemoteLockManager remote) {
    if (canRecallNow()) {
      LockFlushCallback callback = new LockFlushCallback() {
        public void transactionsForLockFlushed(LockID id) {
          synchronized (ClientLockImpl.this) {
            if (greediness.isRecallInProgress()) {
              greediness = recallCommit(remote);
            }
          }
        }
      };

      if (remote.asyncFlush(lock, callback, greediness.getFlushLevel())) {
        return recallCommit(remote);
      } else {
        return greediness.recallInProgress();
      }
    } else {
      return this.greediness;
    }
  }

  private synchronized ClientGreediness recallCommit(RemoteLockManager remote) {
    if (greediness.isFree()) {
      return greediness;
    } else {
      Collection<ClientServerExchangeLockContext> contexts = getRecallCommitStateSnapshot(remote.getClientID());

      ClientGreediness postRecallCommitGreediness = greediness.recallCommitted();
      for (LockStateNode node : this) {
        if (node instanceof PendingLockHold) {
          if (postRecallCommitGreediness.isGreedy()) {
            ((PendingLockHold) node).allowDelegation();
          } else {
            // these nodes have now contacted the server
            ((PendingLockHold) node).delegated("Attached To Recall Commit Message...");
          }
        }
      }

      remote.recallCommit(lock, contexts);

      greediness = greediness.recallCommitted();

      if (greediness.isGreedy()) {
        unparkFirstQueuedAcquire();
      }

      return greediness;
    }
  }

  private synchronized boolean canRecallNow() {
    for (LockStateNode s : this) {
      if (s instanceof LockHold && ((LockHold) s).getLockLevel().isWrite()) { return false; }
    }
    return true;
  }

  public synchronized boolean tryMarkAsGarbage(final RemoteLockManager remote) {
    if (!pinned && isEmpty() && gcCycleCount > 0) {
      greediness = greediness.markAsGarbage();
      if (greediness.isGarbage()) {
        return true;
      } else {
        recall(remote, ServerLockLevel.WRITE, -1);
        return false;
      }
    } else {
      gcCycleCount = (byte) Math.max(Byte.MAX_VALUE, gcCycleCount++);
      return false;
    }
  }

  private void markUsed() {
    gcCycleCount = 0;
  }

  public synchronized void initializeHandshake(ClientID client, ClientHandshakeMessage message) {
    Collection<ClientServerExchangeLockContext> contexts = getFilteredStateSnapshot(client, true);

    for (LockStateNode node : this) {
      if (node instanceof PendingLockHold) {
        // these nodes have now contacted the server
        ((PendingLockHold) node).delegated("Attached To Handshake Message...");
      }
    }

    for (ClientServerExchangeLockContext c : contexts) {
      message.addLockContext(c);
    }
  }

  private synchronized Collection<ClientServerExchangeLockContext> getFilteredStateSnapshot(ClientID client,
                                                                                            boolean greedy) {
    Collection<ClientServerExchangeLockContext> legacyState = new ArrayList();

    Map<ThreadID, ClientServerExchangeLockContext> holds = new HashMap<ThreadID, ClientServerExchangeLockContext>();
    Map<ThreadID, ClientServerExchangeLockContext> pends = new HashMap<ThreadID, ClientServerExchangeLockContext>();

    for (ClientServerExchangeLockContext context : getStateSnapshot(client)) {
      switch (context.getState()) {
        case HOLDER_READ:
          if (holds.get(context.getThreadID()) == null) {
            holds.put(context.getThreadID(), context);
          }
          break;
        case HOLDER_WRITE:
          holds.put(context.getThreadID(), context);
          break;
        case PENDING_READ:
        case TRY_PENDING_READ:
          if (pends.get(context.getThreadID()) == null) {
            pends.put(context.getThreadID(), context);
          }
          break;
        case PENDING_WRITE:
        case TRY_PENDING_WRITE:
          pends.put(context.getThreadID(), context);
          break;
        case WAITER:
          legacyState.add(context);
          break;
        case GREEDY_HOLDER_READ:
        case GREEDY_HOLDER_WRITE:
          if (greedy) { return Collections.singletonList(context); }
          break;
      }
    }
    legacyState.addAll(holds.values());
    legacyState.addAll(pends.values());

    return legacyState;
  }

  private synchronized Collection<ClientServerExchangeLockContext> getRecallCommitStateSnapshot(ClientID client) {
    ClientGreediness postRecallGreediness = greediness.recallCommitted();
    if (postRecallGreediness.isGreedy()) {
      List<ClientServerExchangeLockContext> contexts = new ArrayList<ClientServerExchangeLockContext>();
      contexts.add(postRecallGreediness.toContext(lock, client));
      return contexts;
    } else {
      return getFilteredStateSnapshot(client, false);
    }
  }

  public synchronized String toString() {
    StringBuilder sb = new StringBuilder();

    sb.append("ClientLockImpl : ").append(lock).append('\n');
    sb.append("GC Cycle Count : ").append(gcCycleCount).append('\n');
    sb.append("Greediness : ").append(greediness).append('\n');
    sb.append("State:").append('\n');
    for (LockStateNode s : this) {
      sb.append('\t').append(s).append('\n');
    }

    return sb.toString();
  }
}
