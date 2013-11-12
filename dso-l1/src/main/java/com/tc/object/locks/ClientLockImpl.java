/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.locks;

import com.tc.abortable.AbortableOperationManager;
import com.tc.abortable.AbortedOperationException;
import com.tc.exception.PlatformRejoinException;
import com.tc.exception.TCLockUpgradeNotSupportedError;
import com.tc.exception.TCNotRunningException;
import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.net.ClientID;
import com.tc.object.locks.LockStateNode.LockHold;
import com.tc.object.locks.LockStateNode.LockWaiter;
import com.tc.object.locks.LockStateNode.PendingLockHold;
import com.tc.object.locks.LockStateNode.PendingTryLockHold;
import com.tc.object.msg.ClientHandshakeMessage;
import com.tc.util.AbortedOperationUtil;
import com.tc.util.FindbugsSuppressWarnings;
import com.tc.util.SynchronizedSinglyLinkedList;
import com.tc.util.Util;

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
  private static final TCLogger       LOGGER        = TCLogging.getLogger(ClientLockImpl.class);

  private static final Set<LockLevel> WRITE_LEVELS  = EnumSet.of(LockLevel.WRITE, LockLevel.SYNCHRONOUS_WRITE);
  private static final Set<LockLevel> READ_LEVELS   = EnumSet.of(LockLevel.READ);

  private static final int            BLOCKING_LOCK = Integer.MIN_VALUE;

  private final LockID                lock;

  /*
   * FindBugs believes that the access to greediness in RecallCallback is not synchronized as it doesn't notice the
   * "synchronized (ClientLockImpl.this) {"
   */
  @FindbugsSuppressWarnings("IS2_INCONSISTENT_SYNC")
  private ClientGreediness            greediness    = ClientGreediness.FREE;

  private volatile byte               gcCycleCount  = 0;
  private volatile boolean            pinned;

  public ClientLockImpl(final LockID lock) {
    this.lock = lock;
  }

  @Override
  public synchronized void cleanup() {
    notifyAll();
    for (final Iterator<LockStateNode> it = iterator(); it.hasNext();) {
      LockStateNode lockState = it.next();
      removeAndUnpark(lockState, it);
    }
    greediness = ClientGreediness.FREE;
  }

  private void removeAndUnpark(LockStateNode lockState, Iterator<LockStateNode> it) {
    try {
      lockState.setrejoinInProgress(true);
      it.remove();
      lockState.unpark();
    } catch (AssertionError e) {
      // some impl of LockStateNode (like LockHold) throws AssertionError on unpark()
    }
  }

  /*
   * Try to acquire this lock locally - if successful then return, otherwise queue the request and potentially call out
   * to the server.
   */
  @Override
  public void lock(final AbortableOperationManager abortableOperationManager, final RemoteLockManager remote,
                   final ThreadID thread, final LockLevel level) throws GarbageLockException, AbortedOperationException {
    markUsed();
    if (!tryAcquireLocally(remote, abortableOperationManager, thread, level).isSuccess()) {
      acquireQueued(abortableOperationManager, remote, thread, level);
    }
  }

  /*
   * Try to acquire this lock locally - if successful then return, otherwise queue the request and potentially call out
   * to the server
   */
  @Override
  public void lockInterruptibly(final AbortableOperationManager abortableOperationManager,
                                final RemoteLockManager remote, final ThreadID thread, final LockLevel level)
      throws InterruptedException, GarbageLockException, AbortedOperationException {
    markUsed();
    if (Thread.interrupted()) {
      handleInterrupt(abortableOperationManager);
    }
    if (!tryAcquireLocally(remote, abortableOperationManager, thread, level).isSuccess()) {
      acquireQueuedInterruptibly(abortableOperationManager, remote, thread, level);
    }
  }

  /*
   * Try lock would normally just be: <code>return tryAcquire(remote, thread, level, 0).isSuccess();</code> <p> However
   * because the existing contract on tryLock requires us to wait for the server if the lock attempt is delegated things
   * get a little more complicated.
   */
  @Override
  public boolean tryLock(final AbortableOperationManager abortableOperationManager, final RemoteLockManager remote,
                         final ThreadID thread, final LockLevel level) throws GarbageLockException,
      AbortedOperationException {
    markUsed();
    final LockAcquireResult result = tryAcquireLocally(remote, abortableOperationManager, thread, level);
    if (result.isKnownResult()) {
      return result.isSuccess();
    } else {
      try {
        return acquireQueuedTimeout(abortableOperationManager, remote, thread, level, 0);
      } catch (final InterruptedException e) {
        Thread.currentThread().interrupt();
        return false;
      }
    }
  }

  /*
   * Try to acquire locally - if we fail then queue the request and defer to the server.
   */
  @Override
  public boolean tryLock(final AbortableOperationManager abortableOperationManager, final RemoteLockManager remote,
                         final ThreadID thread, final LockLevel level, final long timeout) throws InterruptedException,
      GarbageLockException, AbortedOperationException {
    markUsed();
    if (Thread.interrupted()) {
      handleInterrupt(abortableOperationManager);
    }
    return tryAcquireLocally(remote, abortableOperationManager, thread, level).isSuccess()
           || acquireQueuedTimeout(abortableOperationManager, remote, thread, level, timeout);
  }

  /*
   * Release the lock and unpark an acquire if release tells us that queued acquires may now succeed.
   */
  @Override
  public void unlock(final RemoteLockManager remote, final ThreadID thread, final LockLevel level)
      throws AbortedOperationException {
    markUsed();
    if (release(remote, thread, level)) {
      unparkFirstQueuedAcquire();
    }
  }

  /*
   * Find a lock waiter in the state and unpark it - while concurrently checking for a write hold by the notifying
   * thread
   */
  @Override
  public boolean notify(final RemoteLockManager remote, final ThreadID thread, final Object waitObject) {
    markUsed();
    return notify(thread, false);
  }

  /*
   * Find all the lock waiters in the state and unpark them.
   */
  @Override
  public boolean notifyAll(final RemoteLockManager remote, final ThreadID thread, final Object waitObject) {
    markUsed();
    return notify(thread, true);
  }

  private boolean notify(final ThreadID thread, final boolean all) {
    boolean result;
    final Collection<LockWaiter> waiters = new ArrayList<LockWaiter>();

    synchronized (this) {
      if (!isLockedBy(thread, WRITE_LEVELS)) { throw new IllegalMonitorStateException(); }

      if (this.greediness.isFree()) {
        // other L1s may be waiting (let server decide who to notify)
        result = true;
      } else {
        for (final Iterator<LockStateNode> it = iterator(); it.hasNext();) {
          final LockStateNode s = it.next();
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

    for (final LockWaiter waiter : waiters) {
      waiter.unpark();
    }

    return result;
  }

  @Override
  public void wait(final AbortableOperationManager abortableOperationManager, final RemoteLockManager remote,
                   final WaitListener listener, final ThreadID thread, final Object waitObject)
      throws InterruptedException, AbortedOperationException {
    wait(abortableOperationManager, remote, listener, thread, waitObject, 0);
  }

  /*
   * Waiting involves unlocking all the write lock holds, sleeping on the original condition, until wake up, and then
   * re-acquiring the original locks in their original order. This code is extraordinarily sensitive to the order of
   * operations...
   */
  @Override
  public void wait(final AbortableOperationManager abortableOperationManager, final RemoteLockManager remote,
                   final WaitListener listener, final ThreadID thread, final Object waitObject, final long timeout)
      throws InterruptedException, AbortedOperationException {
    markUsed();
    if (Thread.interrupted()) {
      handleInterrupt(abortableOperationManager);
    }

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
        while (true) {
          ServerLockLevel flushLevel;
          boolean noLocksHeld;
          synchronized (this) {
            flushLevel = this.greediness.getFlushLevel();
            noLocksHeld = noLocksHeld(null, thread);
          }

          // TODO: This doesn't seem right.. rethink wait behavior...
          remote.flush(this.lock, noLocksHeld);

          synchronized (this) {
            if (flushLevel.equals(this.greediness.getFlushLevel())) {
              waiter = releaseAllAndPushWaiter(remote, thread, waitObject, timeout);
              break;
            } else {
              LOGGER.info("Retrying flush on " + lock + " as flush level moved from " + flushLevel + " to "
                          + this.greediness.getFlushLevel() + " during flush operation");
            }
          }
        }
      }

      unparkFirstQueuedAcquire();
      waitOnLockWaiter(remote, thread, waiter, listener);
    } finally {
      // if (waiter != null) {
      if (waiter != null && !waiter.isRejoinInProgress()) {
        moveWaiterToPending(waiter);
        acquireAll(abortableOperationManager, remote, thread, waiter.getReacquires());
      } else if (!isLockedBy(thread, WRITE_LEVELS)) {
        LOGGER.fatal("Potential lock reacquire failure after wait by " + thread + " in:\n" + this);
      }
    }
  }

  private synchronized LockWaiter releaseAllAndPushWaiter(final RemoteLockManager remote, final ThreadID thread,
                                                          final Object waitObject, final long timeout) {
    final Stack<LockHold> holds = releaseAll(remote, thread);
    final LockWaiter waiter = new LockWaiter(thread, waitObject, holds, timeout);
    addLast(waiter);

    if (this.greediness.isFree()) {
      remote.wait(this.lock, thread, timeout);
    } else if (this.greediness.isRecalled() && canRecallNow()) {
      this.greediness = recallCommit(remote, false);
    }

    return waiter;
  }

  private synchronized Stack<LockHold> releaseAll(final RemoteLockManager remote, final ThreadID thread) {
    final Stack<LockHold> holds = new Stack<LockHold>();
    for (final Iterator<LockStateNode> it = iterator(); it.hasNext();) {
      final LockStateNode node = it.next();
      if ((node instanceof LockHold) && node.getOwner().equals(thread)) {
        it.remove();
        holds.push((LockHold) node);
      }
    }
    return holds;
  }

  private void waitOnLockWaiter(final RemoteLockManager remote, final ThreadID thread, final LockWaiter waiter,
                                final WaitListener listener) throws InterruptedException {
    listener.handleWaitEvent();
    try {
      if (waiter.getTimeout() == 0) {
        waiter.park();
      } else {
        waiter.park(waiter.getTimeout());
      }
    } catch (final InterruptedException e) {
      synchronized (this) {
        if (this.greediness.isFree()) {
          remote.interrupt(this.lock, thread);
        }
        moveWaiterToPending(waiter);
      }
      throw e;
    }
    if (waiter.isRejoinInProgress()) { throw new PlatformRejoinException(); }
  }

  private void acquireAll(final AbortableOperationManager abortableOperationManager, final RemoteLockManager remote,
                          final ThreadID thread, final Stack<PendingLockHold> acquires) {
    Stack<PendingLockHold> acquiresClone = (Stack<PendingLockHold>) acquires.clone();
    while (!acquires.isEmpty()) {
      final PendingLockHold qa = acquires.pop();
      try {
        acquireQueued(abortableOperationManager, remote, thread, qa.getLockLevel(), qa);
      } catch (final GarbageLockException e) {
        throw new AssertionError("GarbageLockException thrown while reacquiring locks after wait");
      } catch (AbortedOperationException e) {
        // If it came here, it failed because of flush, hence this means no lock was taken from the stack and hence
        // throwing AbortedOperationException is fine
        // TODO: looks wrong, for each PendingLockHold in acquires which sees AbortedOperationException
        // we remove all PendingLockHold present in acquiresClone
        for (PendingLockHold pending : acquiresClone) {
          remove(pending);
        }
      }
    }
  }

  @Override
  public synchronized Collection<ClientServerExchangeLockContext> getStateSnapshot(final ClientID client) {
    final Collection<ClientServerExchangeLockContext> contexts = new ArrayList<ClientServerExchangeLockContext>();

    switch (this.greediness) {
      case GARBAGE:
        break;
      default:
        final ClientServerExchangeLockContext c = this.greediness.toContext(this.lock, client);
        if (c != null) {
          contexts.add(c);
        }
    }

    for (final LockStateNode s : this) {
      final ClientServerExchangeLockContext c = s.toContext(this.lock, client);
      if (c != null) {
        contexts.add(c);
      }
    }

    return contexts;
  }

  @Override
  public synchronized int pendingCount() {
    int penders = 0;
    for (final LockStateNode s : this) {
      if (s instanceof PendingLockHold) {
        penders++;
      }
    }
    return penders;
  }

  @Override
  public synchronized int waitingCount() {
    int waiters = 0;
    for (final LockStateNode s : this) {
      if (s instanceof LockWaiter) {
        waiters++;
      }
    }
    return waiters;
  }

  @Override
  public synchronized boolean isLocked(final LockLevel level) {
    for (final LockStateNode s : this) {
      if ((s instanceof LockHold) && (((LockHold) s).getLockLevel().equals(level))) { return true; }
    }
    return false;
  }

  @Override
  public synchronized boolean isLockedBy(final ThreadID thread, final LockLevel level) {
    for (final LockStateNode s : this) {
      if ((s instanceof LockHold) && (((LockHold) s).getLockLevel().equals(level) || (level == null))
          && s.getOwner().equals(thread)) { return true; }
    }
    return false;
  }

  public synchronized boolean isLockedBy(final ThreadID thread, final Set<LockLevel> levels) {
    for (final LockStateNode s : this) {
      if ((s instanceof LockHold) && s.getOwner().equals(thread) && levels.contains(((LockHold) s).getLockLevel())) { return true; }
    }
    return false;
  }

  @Override
  public synchronized int holdCount(final LockLevel level) {
    int holders = 0;
    for (final LockStateNode s : this) {
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

  @Override
  public void pinLock() {
    this.pinned = true;
  }

  @Override
  public void unpinLock() {
    this.pinned = false;
  }

  /*
   * Called by the stage thread (the transaction apply thread) when the server wishes to notify a thread waiting on this
   * lock
   */
  @Override
  public void notified(final ThreadID thread) {
    LockWaiter waiter = null;
    synchronized (this) {
      for (final Iterator<LockStateNode> it = iterator(); it.hasNext();) {
        final LockStateNode s = it.next();
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
  private synchronized void moveWaiterToPending(final LockWaiter waiter) {
    if ((waiter != null) && (remove(waiter) != null)) {
      addPendingAcquires(waiter);
    }
  }

  private synchronized void addPendingAcquires(final LockWaiter waiter) {
    if (!waiter.isRejoinInProgress()) {
      Stack<PendingLockHold> reacquires = waiter.getReacquires();
      java.util.ListIterator<PendingLockHold> it = reacquires.listIterator(reacquires.size());
      while (it.hasPrevious()) {
        addLast(it.previous());
      }
    }
  }

  /**
   * ClientLockImpl ignores the interest level of the recall request. Instead it will always recall the lock as long as
   * there are no held write locks locally (and assuming we don't decide to lease it). This gives us the benefit of not
   * blocking future read recalls if a write recall is pending. This can be a problem when the write recall was
   * triggered by a tryLock that no longer requires the lock.
   */
  @Override
  public synchronized boolean recall(final RemoteLockManager remote, final ServerLockLevel interest, final int lease,
                                     boolean batch) {
    // transition the greediness state
    this.greediness = this.greediness.recalled(this, lease, interest);

    if (this.greediness.isRecalled()) {
      this.greediness = doRecall(remote, batch);
      return false;
    } else if (this.greediness.isGreedy()) {
      return true;
    } else {
      return false;
    }
  }

  /*
   * Called by the stage thread to indicate that the tryLock attempt has failed.
   */
  @Override
  public void refuse(final ThreadID thread, final ServerLockLevel level) {
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
  @Override
  public void award(final RemoteLockManager remote, final ThreadID thread, final ServerLockLevel level)
      throws GarbageLockException {
    if (ThreadID.VM_ID.equals(thread)) {
      synchronized (this) {
        this.greediness = this.greediness.awarded(level);
      }
      unparkFirstQueuedAcquire();
    } else {
      PendingLockHold acquire;
      synchronized (this) {
        acquire = getQueuedAcquire(thread, level);
        if (acquire == null) {
          remote.unlock(this.lock, thread, level);
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
     * unlock happened for the same thread and waiting for flush
     */
    WAIT_FOR_FLUSH,
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
      return isSuccess() || isFailure() || isWaitingForFlush();
    }

    public boolean isWaitingForFlush() {
      return this == WAIT_FOR_FLUSH;
    }
  }

  /*
   * Try to acquire the lock (optionally with delegation to the server)
   */
  private LockAcquireResult tryAcquire(final AbortableOperationManager abortableOperationManager,
                                       final RemoteLockManager remote, final ThreadID thread, final LockLevel level,
                                       final long timeout, final PendingLockHold node) throws GarbageLockException,
      AbortedOperationException {
    // try to do things locally first...
    final LockAcquireResult result = tryAcquireLocally(remote, abortableOperationManager, thread, level);
    if (result.isKnownResult()) {
      return result;
    } else {
      synchronized (this) {
        if (!node.canDelegate()) {
          // no server delegation - just return local result
          return result;
        } else {
          // delegate to server
          final ServerLockLevel requestLevel = ServerLockLevel.fromClientLockLevel(level);
          this.greediness = this.greediness.requested(requestLevel);
          if (this.greediness.isFree()) {
            switch ((int) timeout) {
              case ClientLockImpl.BLOCKING_LOCK:
                remote.lock(this.lock, thread, requestLevel);
                node.delegated("Called remote.lock(...)...");
                break;
              default:
                remote.tryLock(this.lock, thread, requestLevel, timeout);
                node.delegated("Called remote.tryLock(...)...");
                break;
            }
            return LockAcquireResult.USED_SERVER;
          } else if (this.greediness.isRecalled()) {
            // drop through to trigger recall
          } else {
            node.delegated("Waiting For Recall...");
            return LockAcquireResult.USED_SERVER;
          }
        }
      }

      while (true) {
        ServerLockLevel flushLevel;
        boolean noLocksHeld;
        synchronized (this) {
          flushLevel = this.greediness.getFlushLevel();
          noLocksHeld = noLocksHeld(null, null);
        }

        remote.flush(lock, noLocksHeld);

        synchronized (this) {
          if (flushLevel.equals(this.greediness.getFlushLevel())) {
            if (this.greediness.isRecalled() && canRecallNow()) {
              this.greediness = recallCommit(remote, false);
            }
            node.delegated("Waiting For Recall...");
            return LockAcquireResult.USED_SERVER;
          } else {
            LOGGER.info("Retrying flush on " + lock + " as flush level moved from " + flushLevel + " to "
                        + this.greediness.getFlushLevel() + " during flush operation");
          }
        }
      }
    }
  }

  /*
   * Attempt to acquire the lock at the given level locally
   */
  private LockAcquireResult tryAcquireLocally(final RemoteLockManager remote,
                                              final AbortableOperationManager abortableOperationManager,
                                              final ThreadID thread, final LockLevel level)
      throws GarbageLockException, AbortedOperationException {
    // if this is a concurrent acquire then just let it through.
    if (level == LockLevel.CONCURRENT) { return LockAcquireResult.SHARED_SUCCESS; }

    synchronized (this) {
      LockAcquireResult result = tryAcquireUsingThreadState(remote, thread, level);
      boolean interrupted = false;
      while (result.isWaitingForFlush()) {
        try {
          ClientLockImpl.this.wait();
        } catch (InterruptedException e) {
          AbortedOperationUtil.throwExceptionIfAborted(abortableOperationManager);
          interrupted = true;
        }
        result = tryAcquireUsingThreadState(remote, thread, level);
      }

      if (interrupted) {
        Util.selfInterruptIfNeeded(interrupted);
      }

      if (result.isKnownResult()) {
        return result;
      } else if (this.greediness.canAward(level)) {
        addFirst(new LockHold(thread, level));
        return level.isWrite() ? LockAcquireResult.SUCCESS : LockAcquireResult.SHARED_SUCCESS;
      } else {
        return LockAcquireResult.UNKNOWN;
      }
    }
  }

  private LockAcquireResult tryAcquireUsingThreadState(RemoteLockManager remote, final ThreadID thread,
                                                       final LockLevel level) {
    // check if flush in progress then wait for flush
    if (isFlushInProgress()) { return LockAcquireResult.WAIT_FOR_FLUSH; }
    // What can we glean from local lock state
    final LockHold newHold = new LockHold(thread, level);
    for (final Iterator<LockStateNode> it = iterator(); it.hasNext();) {
      final LockStateNode s = it.next();
      final LockAcquireResult result = s.allowsHold(newHold);
      if (result.isKnownResult()) {
        if (result.isWaitingForFlush()) {
          return result;
        } else if (result.isSuccess()) {
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

  private boolean isFlushInProgress() {
    for (final LockStateNode s : this) {
      if ((s instanceof LockHold) && (((LockHold) s).isFlushInProgress())) { return true; }
    }
    return false;
  }

  /*
   * Unlock and return true if acquires might now succeed.
   */
  private boolean release(final RemoteLockManager remote, final ThreadID thread, final LockLevel level)
      throws AbortedOperationException {
    // concurrent unlocks are implicitly okay - we don't monitor concurrent locks
    if (level == LockLevel.CONCURRENT) {
      // concurrent unlocks do not change the state - no reason why queued acquires would succeed
      return false;
    }

    LockHold unlock = null;
    boolean aborted = false;
    synchronized (this) {
      for (final Iterator<LockStateNode> it = iterator(); it.hasNext();) {
        final LockStateNode s = it.next();
        if (s instanceof LockHold) {
          final LockHold hold = (LockHold) s;
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
      try {
        remote.waitForServerToReceiveTxnsForThisLock(this.lock);
      } catch (AbortedOperationException e) {
        aborted = true;
      }
    }

    synchronized (this) {
      final boolean noLocksHeld = noLocksHeld(unlock, null);
      final ServerLockLevel flushLevel = greediness.getFlushLevel();
      // only one unlock callback is added for flushing the lock
      if (flushOnUnlock(unlock) && !isFlushInProgress()) {
        // TODO: to be done in a flush thread and not on txn complete thread
        unlock.flushInProgress();
        UnlockCallback flushCallback = new UnlockCallback(remote, flushLevel, unlock);
        if (remote.asyncFlush(lock, flushCallback, noLocksHeld)) {
          flushCallback.transactionsForLockFlushed(lock);
        }
      } else {
        boolean rv = release(remote, unlock);
        if (aborted) { throw new AbortedOperationException(); }
        return rv;
      }
    }
    if (aborted) { throw new AbortedOperationException(); }

    return true;
  }

  private boolean noLocksHeld(LockHold unlockHold, ThreadID thread) {
    synchronized (this) {
      if (this.greediness == ClientGreediness.WRITE_RECALL_FOR_READ_IN_PROGRESS
          || this.greediness == ClientGreediness.RECALLED_WRITE_FOR_READ) { return false; }

      for (final LockStateNode s : this) {
        if (s == unlockHold || s.getOwner().equals(thread)) {
          continue;
        }

        if (s instanceof LockHold) { return false; }
      }
      return true;
    }
  }

  private synchronized boolean release(final RemoteLockManager remote, final LockHold unlock) {
    remove(unlock);
    if (this.greediness.isFree()) {
      remoteUnlock(remote, unlock);
    } else if (this.greediness.isRecalled() && canRecallNow()) {
      this.greediness = recallCommit(remote, false);
    }

    // this is wrong - but shouldn't break anything
    return true;
  }

  private void remoteUnlock(final RemoteLockManager remote, final LockHold unlock) {
    for (final LockStateNode s : this) {
      if (s == unlock) {
        continue;
      }

      if (s instanceof LockHold && s.getOwner().equals(unlock.getOwner())) {
        final LockHold hold = (LockHold) s;
        if (unlock.getLockLevel().isWrite()) {
          if (hold.getLockLevel().isWrite()) { return; }
        } else {
          return;
        }
      }
    }

    remote.unlock(this.lock, unlock.getOwner(), ServerLockLevel.fromClientLockLevel(unlock.getLockLevel()));
  }

  private synchronized boolean flushOnUnlock(final LockHold unlock) {
    if (!this.greediness.flushOnUnlock()) { return false; }

    for (final LockStateNode s : this) {
      if (s == unlock) {
        continue;
      }

      if (s instanceof LockHold && s.getOwner().equals(unlock.getOwner())) {
        if (((LockHold) s).getLockLevel().isWrite()) { return false; }
        if (unlock.getLockLevel().isRead()) { return false; }
      }
    }
    return true;
  }

  private synchronized boolean flushOnUnlockAll(final ThreadID thread) {
    if (this.greediness.flushOnUnlock()) { return true; }

    for (final LockStateNode s : this) {
      if (s instanceof LockHold && s.getOwner().equals(thread)) {
        if (((LockHold) s).getLockLevel().isSyncWrite()) { return true; }
      }
    }
    return false;
  }

  /*
   * Conventional acquire queued - uses a LockSupport based queue object.
   */
  private void acquireQueued(final AbortableOperationManager abortableOperationManager, final RemoteLockManager remote,
                             final ThreadID thread, final LockLevel level) throws GarbageLockException,
      AbortedOperationException {
    final PendingLockHold node = new PendingLockHold(thread, level);
    addLast(node);
    try {
      acquireQueued(abortableOperationManager, remote, thread, level, node);
    } catch (AbortedOperationException e) {
      remove(node);
      throw e;
    }
  }

  /*
   * Generic acquire - uses an already existing queued node - used during wait notify
   */
  private void acquireQueued(final AbortableOperationManager abortableOperationManager, final RemoteLockManager remote,
                             final ThreadID thread, final LockLevel level, final PendingLockHold node)
      throws GarbageLockException, AbortedOperationException {
    boolean interrupted = false;
    try {
      for (;;) {
        // try to acquire before sleeping
        final LockAcquireResult result = tryAcquire(abortableOperationManager, remote, thread, level, BLOCKING_LOCK,
                                                    node);
        if (result.isShared()) {
          unparkNextQueuedAcquire(node);
        } else {
          unparkSubsequentTryLocks(node);
        }
        if (result.isSuccess()) {
          remove(node);
          return;
        }

        // park the thread and wait for unpark
        node.park();
        if (Thread.interrupted()) {
          try {
            AbortedOperationUtil.throwExceptionIfAborted(abortableOperationManager);
          } catch (AbortedOperationException e) {
            abortAndRemove(remote, node);
            unparkFirstQueuedAcquire();
            throw e;
          }
          interrupted = true;

          if (remote.isShutdown()) { throw new TCNotRunningException(); }
        }
        if (node.isRejoinInProgress()) { throw new PlatformRejoinException(); }
      }
    } catch (final RuntimeException ex) {
      abortAndRemove(remote, node);
      unparkFirstQueuedAcquire();
      throw ex;
    } catch (final TCLockUpgradeNotSupportedError e) {
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
  private void acquireQueuedInterruptibly(final AbortableOperationManager abortableOperationManager,
                                          final RemoteLockManager remote, final ThreadID thread, final LockLevel level)
      throws InterruptedException, GarbageLockException, AbortedOperationException {
    final PendingLockHold node = new PendingLockHold(thread, level);
    addLast(node);
    try {
      for (;;) {
        LockAcquireResult result = null;
        try {
          result = tryAcquire(abortableOperationManager, remote, thread, level, BLOCKING_LOCK, node);
        } catch (AbortedOperationException e) {
          remove(node);
          throw e;
        }
        if (result.isShared()) {
          unparkNextQueuedAcquire(node);
        } else {
          unparkSubsequentTryLocks(node);
        }
        if (result.isSuccess()) {
          remove(node);
          return;
        }

        if (Thread.interrupted()) {
          break;
        }

        node.park();

        if (Thread.interrupted()) {
          try {
            AbortedOperationUtil.throwExceptionIfAborted(abortableOperationManager);
          } catch (AbortedOperationException e) {
            abortAndRemove(remote, node);
            unparkFirstQueuedAcquire();
            throw e;
          }

          break;
        }
        if (node.isRejoinInProgress()) { throw new PlatformRejoinException(); }
      }
    } catch (final RuntimeException ex) {
      abortAndRemove(remote, node);
      unparkFirstQueuedAcquire();
      throw ex;
    } catch (final TCLockUpgradeNotSupportedError e) {
      abortAndRemove(remote, node);
      unparkFirstQueuedAcquire();
      throw e;
    }
    // Arrive here only if interrupted
    abortAndRemove(remote, node);
    handleInterrupt(abortableOperationManager);
  }

  /*
   * Acquire queued - waiting for at most timeout milliseconds.
   */
  private boolean acquireQueuedTimeout(final AbortableOperationManager abortableOperationManager,
                                       final RemoteLockManager remote, final ThreadID thread, final LockLevel level,
                                       long timeout) throws InterruptedException, GarbageLockException,
      AbortedOperationException {
    long lastTime = System.currentTimeMillis();
    final PendingTryLockHold node = new PendingTryLockHold(thread, level, timeout);
    addLast(node);
    try {
      while (!node.isRefused()) {
        LockAcquireResult result;
        try {
          result = tryAcquire(abortableOperationManager, remote, thread, level, timeout, node);
        } catch (AbortedOperationException e) {
          remove(node);
          throw e;
        }
        if (result.isShared()) {
          unparkNextQueuedAcquire(node);
        } else {
          unparkSubsequentTryLocks(node);
        }
        if (result.isSuccess()) {
          remove(node);
          return true;
        } else if (result.isFailure() && timeout <= 0) {
          abortAndRemove(remote, node);
          return false;
        } else if (node.canDelegate() && timeout <= 0) {
          abortAndRemove(remote, node);
          return false;
        }

        if (!node.canDelegate()) {
          node.park();
          try {
            AbortedOperationUtil.throwExceptionIfAborted(abortableOperationManager);
          } catch (AbortedOperationException e) {
            abortAndRemove(remote, node);
            unparkFirstQueuedAcquire();
            throw e;
          }
        } else {
          node.park(timeout);
          try {
            AbortedOperationUtil.throwExceptionIfAborted(abortableOperationManager);
          } catch (AbortedOperationException e) {
            abortAndRemove(remote, node);
            unparkFirstQueuedAcquire();
            throw e;
          }
        }
        if (Thread.interrupted()) {
          abortAndRemove(remote, node);
          handleInterrupt(abortableOperationManager);
        }
        if (node.isRejoinInProgress()) { throw new PlatformRejoinException(); }
        final long now = System.currentTimeMillis();
        timeout -= now - lastTime;
        // possibility of changing node timeout here...
        lastTime = now;
      }
      remove(node);
      final LockAcquireResult result = tryAcquireLocally(remote, abortableOperationManager, thread, level);
      if (result.isShared()) {
        unparkFirstQueuedAcquire();
      }
      return result.isSuccess();
    } catch (final RuntimeException ex) {
      abortAndRemove(remote, node);
      unparkFirstQueuedAcquire();
      throw ex;
    } catch (final TCLockUpgradeNotSupportedError e) {
      abortAndRemove(remote, node);
      unparkFirstQueuedAcquire();
      throw e;
    }
  }

  private synchronized void abortAndRemove(final RemoteLockManager remote, PendingLockHold node) {
    node = (PendingLockHold) remove(node);
    if (node != null && node.isAwarded()) {
      remote.unlock(this.lock, node.getOwner(), ServerLockLevel.fromClientLockLevel(node.getLockLevel()));
    }
  }

  /*
   * Unpark the first queued acquire
   */
  private void unparkFirstQueuedAcquire() {
    final PendingLockHold firstAcquire = getFirstQueuedAcquire();
    if (firstAcquire != null) {
      firstAcquire.unpark();
    }
  }

  /*
   * Unpark the next queued acquire (after supplied node)
   */
  private void unparkNextQueuedAcquire(final LockStateNode node) {
    final PendingLockHold nextAcquire = getNextQueuedAcquire(node);
    if (nextAcquire != null) {
      nextAcquire.unpark();
    }
  }

  private void unparkSubsequentTryLocks(final LockStateNode node) {
    Collection<PendingTryLockHold> pending = new ArrayList<PendingTryLockHold>();
    synchronized (this) {
      PendingLockHold a = getNextQueuedAcquire(node);
      while (a != null) {
        if (a instanceof PendingTryLockHold) {
          pending.add((PendingTryLockHold) a);
        }
        a = getNextQueuedAcquire(a);
      }
    }

    for (PendingTryLockHold a : pending) {
      a.unpark();
    }
  }

  private synchronized PendingLockHold getFirstQueuedAcquire() {
    for (final LockStateNode current : this) {
      if (current instanceof PendingLockHold) { return (PendingLockHold) current; }
    }
    return null;
  }

  private synchronized PendingLockHold getNextQueuedAcquire(final LockStateNode node) {
    LockStateNode current = node.getNext();
    while (current != null) {
      if (current instanceof PendingLockHold) { return (PendingLockHold) current; }
      current = current.getNext();
    }
    return null;
  }

  private synchronized PendingLockHold getQueuedAcquire(final ThreadID thread, final ServerLockLevel level) {
    for (final LockStateNode s : this) {
      if ((s instanceof PendingLockHold) && s.getOwner().equals(thread)
          && level.equals(ServerLockLevel.fromClientLockLevel(((PendingLockHold) s).getLockLevel()))) { return (PendingLockHold) s; }
    }
    return null;
  }

  private synchronized ClientGreediness doRecall(final RemoteLockManager remote, final boolean batch) {
    if (canRecallNow()) {
      final ServerLockLevel flushLevel = this.greediness.getFlushLevel();
      final LockFlushCallback callback = new RecallCallback(remote, batch, flushLevel);
      boolean noLocksHeld = noLocksHeld(null, null);
      if (remote.asyncFlush(this.lock, callback, noLocksHeld)) {
        return recallCommit(remote, batch);
      } else {
        return this.greediness.recallInProgress();
      }
    } else {
      return this.greediness;
    }
  }

  private class UnlockCallback implements LockFlushCallback {
    private final RemoteLockManager remote;
    private final ServerLockLevel   expectedFlushLevel;
    private final LockHold          unlock;

    public UnlockCallback(RemoteLockManager remote, ServerLockLevel flushLevel, final LockHold unlock) {
      this.remote = remote;
      this.expectedFlushLevel = flushLevel;
      this.unlock = unlock;
    }

    @Override
    public void transactionsForLockFlushed(LockID id) {
      synchronized (ClientLockImpl.this) {
        if (expectedFlushLevel.equals(greediness.getFlushLevel())) {
          releaseOnFlush();
        } else {
          UnlockCallback callback = new UnlockCallback(remote, greediness.getFlushLevel(), unlock);
          boolean noLocksHeld = noLocksHeld(null, null);
          if (remote.asyncFlush(id, callback, noLocksHeld)) {
            releaseOnFlush();
          }
        }
      }
    }

    private void releaseOnFlush() {
      unlock.flushCompleted();
      ClientLockImpl.this.notifyAll();
      release(remote, unlock);
      unparkFirstQueuedAcquire();
    }
  }

  private class RecallCallback implements LockFlushCallback {

    private final RemoteLockManager remote;
    private final boolean           batch;
    private final ServerLockLevel   expectedFlushLevel;

    public RecallCallback(RemoteLockManager remote, boolean batch, ServerLockLevel flushLevel) {
      this.remote = remote;
      this.batch = batch;
      this.expectedFlushLevel = flushLevel;
    }

    @Override
    public void transactionsForLockFlushed(final LockID id) {
      synchronized (ClientLockImpl.this) {
        if (greediness.isRecallInProgress()) {
          ServerLockLevel flushLevel = greediness.getFlushLevel();
          if (expectedFlushLevel.equals(flushLevel)) {
            greediness = recallCommit(remote, batch);
          } else {
            LOGGER.info("Retrying flush on " + lock + " as flush level moved from " + expectedFlushLevel + " to "
                        + flushLevel + " during flush operation");
            LockFlushCallback callback = new RecallCallback(remote, batch, flushLevel);
            boolean noLocksHeld = noLocksHeld(null, null);
            if (remote.asyncFlush(id, callback, noLocksHeld)) {
              greediness = recallCommit(remote, batch);
            }
          }
        }
      }
    }
  }

  private synchronized ClientGreediness recallCommit(final RemoteLockManager remote, boolean batch) {
    if (this.greediness.isFree()) {
      return this.greediness;
    } else {
      final Collection<ClientServerExchangeLockContext> contexts = getRecallCommitStateSnapshot(remote.getClientID());

      final ClientGreediness postRecallCommitGreediness = this.greediness.recallCommitted();
      for (final LockStateNode node : this) {
        if (node instanceof PendingLockHold) {
          if (postRecallCommitGreediness.isGreedy()) {
            ((PendingLockHold) node).allowDelegation();
          } else {
            // these nodes have now contacted the server
            ((PendingLockHold) node).delegated("Attached To Recall Commit Message...");
          }
        }
      }

      remote.recallCommit(this.lock, contexts, batch);

      this.greediness = this.greediness.recallCommitted();

      if (this.greediness.isGreedy()) {
        unparkFirstQueuedAcquire();
      }

      return this.greediness;
    }
  }

  private synchronized boolean canRecallNow() {
    for (final LockStateNode s : this) {
      if (s instanceof LockHold && ((LockHold) s).getLockLevel().isWrite()) { return false; }
    }
    return true;
  }

  /**
   * This is always called from Lock GC. so recall commits from here will be batched
   */
  @Override
  public synchronized boolean tryMarkAsGarbage(final RemoteLockManager remote) {
    if (!this.pinned && isEmpty() && this.gcCycleCount > 0) {
      this.greediness = this.greediness.markAsGarbage();
      if (this.greediness.isGarbage()) {
        return true;
      } else {
        // batching can cause race explained in ENG-422
        recall(remote, ServerLockLevel.WRITE, -1, false);
        return false;
      }
    } else {
      this.gcCycleCount = (byte) Math.max(Byte.MAX_VALUE, this.gcCycleCount++);
      return false;
    }
  }

  private void markUsed() {
    this.gcCycleCount = 0;
  }

  @Override
  public synchronized void initializeHandshake(final ClientID client, final ClientHandshakeMessage message) {
    final Collection<ClientServerExchangeLockContext> contexts = getFilteredStateSnapshot(client, true);

    for (final LockStateNode node : this) {
      if (node instanceof PendingLockHold) {
        // these nodes have now contacted the server
        ((PendingLockHold) node).delegated("Attached To Handshake Message...");
      }
    }

    for (final ClientServerExchangeLockContext c : contexts) {
      message.addLockContext(c);
    }
  }

  private synchronized Collection<ClientServerExchangeLockContext> getFilteredStateSnapshot(final ClientID client,
                                                                                            final boolean greedy) {
    final Collection<ClientServerExchangeLockContext> legacyState = new ArrayList();

    final Map<ThreadID, ClientServerExchangeLockContext> holds = new HashMap<ThreadID, ClientServerExchangeLockContext>();
    final Map<ThreadID, ClientServerExchangeLockContext> pends = new HashMap<ThreadID, ClientServerExchangeLockContext>();

    for (final ClientServerExchangeLockContext context : getStateSnapshot(client)) {
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

  private synchronized Collection<ClientServerExchangeLockContext> getRecallCommitStateSnapshot(final ClientID client) {
    final ClientGreediness postRecallGreediness = this.greediness.recallCommitted();
    if (postRecallGreediness.isGreedy()) {
      final List<ClientServerExchangeLockContext> contexts = new ArrayList<ClientServerExchangeLockContext>();
      contexts.add(postRecallGreediness.toContext(this.lock, client));
      return contexts;
    } else {
      return getFilteredStateSnapshot(client, false);
    }
  }

  @Override
  public synchronized String toString() {
    final StringBuilder sb = new StringBuilder();

    sb.append("ClientLockImpl : ").append(this.lock).append('\n');
    sb.append("GC Cycle Count : ").append(this.gcCycleCount).append('\n');
    sb.append("Greediness : ").append(this.greediness).append('\n');
    sb.append("State:").append('\n');
    for (final LockStateNode s : this) {
      sb.append('\t').append(s).append('\n');
    }

    return sb.toString();
  }

  private void handleInterrupt(AbortableOperationManager abortableOperationManager) throws InterruptedException,
      AbortedOperationException {
    if (abortableOperationManager.isAborted()) {
      throw new AbortedOperationException();
    } else {
      throw new InterruptedException();
    }
  }
}
