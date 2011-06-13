/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.locks;

import com.tc.exception.TCNotRunningException;
import com.tc.logging.TCLogger;
import com.tc.management.ClientLockStatManager;
import com.tc.net.ClientID;
import com.tc.net.NodeID;
import com.tc.object.msg.ClientHandshakeMessage;
import com.tc.object.session.SessionID;
import com.tc.object.session.SessionManager;
import com.tc.operatorevent.LockEventListener;
import com.tc.text.PrettyPrintable;
import com.tc.text.PrettyPrinter;
import com.tc.util.FindbugsSuppressWarnings;
import com.tc.util.Util;
import com.tc.util.runtime.ThreadIDManager;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map.Entry;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class ClientLockManagerImpl implements ClientLockManager, ClientLockManagerTestMethods, PrettyPrintable {
  private static final WaitListener               NULL_LISTENER       = new WaitListener() {
                                                                        public void handleWaitEvent() {
                                                                          //
                                                                        }
                                                                      };

  private final Timer                             gcTimer             = new Timer("ClientLockManager LockGC", true);
  private final Timer                             lockLeaseTimer      = new Timer("ClientLockManager Lock Lease Timer",
                                                                                  true);
  private final ConcurrentMap<LockID, ClientLock> locks;
  private final ReentrantReadWriteLock            stateGuard          = new ReentrantReadWriteLock();
  private final Condition                         runningCondition    = this.stateGuard.writeLock().newCondition();
  private State                                   state               = State.RUNNING;

  private final RemoteLockManager                 remoteManager;
  private final ThreadIDManager                   threadManager;
  private final SessionManager                    sessionManager;
  private final TCLogger                          logger;

  private final ConcurrentMap<ThreadID, Object>   inFlightLockQueries = new ConcurrentHashMap<ThreadID, Object>();
  private final List<LockEventListener>           lockEventListeners  = new CopyOnWriteArrayList<LockEventListener>();

  @Deprecated
  private final ClientLockStatManager             statManager;

  public ClientLockManagerImpl(final TCLogger logger, final SessionManager sessionManager,
                               final RemoteLockManager remoteManager, final ThreadIDManager threadManager,
                               final ClientLockManagerConfig config, final ClientLockStatManager statManager) {
    this.logger = logger;
    this.remoteManager = remoteManager;
    this.threadManager = threadManager;
    this.sessionManager = sessionManager;

    this.statManager = statManager;
    this.locks = new ConcurrentHashMap<LockID, ClientLock>(config.getStripedCount());
    final long gcPeriod = Math.max(config.getTimeoutInterval(), 100);
    this.gcTimer.schedule(new LockGcTimerTask(), gcPeriod, gcPeriod);
  }

  private ClientLock getOrCreateClientLockState(final LockID lock) {
    ClientLock lockState = this.locks.get(lock);
    if (lockState == null) {
      lockState = new ClientLockImpl(lock);
      final ClientLock racer = this.locks.putIfAbsent(lock, lockState);
      if (racer != null) { return racer; }
    }

    return lockState;
  }

  private ClientLock getClientLockState(final LockID lock) {
    return this.locks.get(lock);
  }

  /***********************************/
  /* BEGIN TerracottaLocking METHODS */
  /***********************************/

  public void lock(final LockID lock, final LockLevel level) {
    waitUntilRunning();

    fireLockAttempted(lock);

    while (true) {
      final ClientLock lockState = getOrCreateClientLockState(lock);
      try {
        lockState.lock(this.remoteManager, this.threadManager.getThreadID(), level);
        break;
      } catch (final GarbageLockException e) {
        // ignorable - thrown when operating on a garbage collected lock
        // gc thread should clear this object soon - spin and re-get...
        this.logger.info("Hitting garbage lock state during lock on " + lock);
      }
    }

    fireLockSucceeded(lock);
  }

  public boolean tryLock(final LockID lock, final LockLevel level) {
    waitUntilRunning();

    fireLockAttempted(lock);

    while (true) {
      final ClientLock lockState = getOrCreateClientLockState(lock);
      try {
        if (lockState.tryLock(this.remoteManager, this.threadManager.getThreadID(), level)) {
          fireLockSucceeded(lock);
          return true;
        } else {
          return false;
        }
      } catch (final GarbageLockException e) {
        // ignorable - thrown when operating on a garbage collected lock
        // gc thread should clear this object soon - spin and re-get...
        this.logger.info("Hitting garbage lock state during tryLock on " + lock);
      }
    }
  }

  public boolean tryLock(final LockID lock, final LockLevel level, final long timeout) throws InterruptedException {
    waitUntilRunning();

    fireLockAttempted(lock);

    while (true) {
      final ClientLock lockState = getOrCreateClientLockState(lock);
      try {
        if (lockState.tryLock(this.remoteManager, this.threadManager.getThreadID(), level, timeout)) {
          fireLockSucceeded(lock);
          return true;
        } else {
          return false;
        }
      } catch (final GarbageLockException e) {
        // ignorable - thrown when operating on a garbage collected lock
        // gc thread should clear this object soon - spin and re-get...
        this.logger.info("Hitting garbage lock state during tryLock with timeout on " + lock);
      }
    }
  }

  public void lockInterruptibly(final LockID lock, final LockLevel level) throws InterruptedException {
    waitUntilRunning();

    fireLockAttempted(lock);

    while (true) {
      final ClientLock lockState = getOrCreateClientLockState(lock);
      try {
        lockState.lockInterruptibly(this.remoteManager, this.threadManager.getThreadID(), level);
        break;
      } catch (final GarbageLockException e) {
        // ignorable - thrown when operating on a garbage collected lock
        // gc thread should clear this object soon - spin and re-get...
        this.logger.info("Hitting garbage lock state during lockInterruptibly on " + lock);
      }
    }

    fireLockSucceeded(lock);
  }

  public void unlock(final LockID lock, final LockLevel level) {
    waitUntilRunning();
    final ClientLock lockState = getOrCreateClientLockState(lock);
    lockState.unlock(this.remoteManager, this.threadManager.getThreadID(), level);

    fireUnlock(lock);
  }

  public Notify notify(final LockID lock, final Object waitObject) {
    waitUntilRunning();
    final ClientLock lockState = getOrCreateClientLockState(lock);
    final ThreadID thread = this.threadManager.getThreadID();
    if (lockState.notify(this.remoteManager, thread, null)) {
      return new NotifyImpl(lock, thread, false);
    } else {
      return NotifyImpl.NULL;
    }
  }

  public Notify notifyAll(final LockID lock, final Object waitObject) {
    waitUntilRunning();
    final ClientLock lockState = getOrCreateClientLockState(lock);
    final ThreadID thread = this.threadManager.getThreadID();
    if (lockState.notifyAll(this.remoteManager, thread, null)) {
      return new NotifyImpl(lock, thread, true);
    } else {
      return NotifyImpl.NULL;
    }
  }

  public void wait(final LockID lock, final Object waitObject) throws InterruptedException {
    wait(lock, NULL_LISTENER, waitObject);
  }

  public void wait(final LockID lock, final Object waitObject, final long timeout) throws InterruptedException {
    wait(lock, NULL_LISTENER, waitObject, timeout);
  }

  public boolean isLocked(final LockID lock, final LockLevel level) {
    waitUntilRunning();
    final ClientLock lockState = getClientLockState(lock);
    if (lockState != null) {
      if (lockState.isLocked(level)) { return true; }
    }

    for (final ClientServerExchangeLockContext cselc : queryLock(lock)) {
      if (this.remoteManager.getClientID().equals(cselc.getNodeID())) {
        continue;
      }

      switch (cselc.getState()) {
        default:
          continue;

        case GREEDY_HOLDER_READ:
        case HOLDER_READ:
          if (level == LockLevel.READ) { return true; }
          break;
        case GREEDY_HOLDER_WRITE:
        case HOLDER_WRITE:
          if ((level == LockLevel.WRITE) || (level == LockLevel.SYNCHRONOUS_WRITE)) { return true; }
          break;
      }
    }
    return false;
  }

  public boolean isLockedByCurrentThread(final LockID lock, final LockLevel level) {
    waitUntilRunning();
    final ClientLock lockState = getClientLockState(lock);
    if (lockState == null) {
      return false;
    } else {
      return lockState.isLockedBy(this.threadManager.getThreadID(), level);
    }
  }

  public boolean isLockedByCurrentThread(final LockLevel level) {
    final ThreadID thread = this.threadManager.getThreadID();
    for (final ClientLock lockState : this.locks.values()) {
      if (lockState.isLockedBy(thread, level)) { return true; }
    }
    return false;
  }

  public int localHoldCount(final LockID lock, final LockLevel level) {
    waitUntilRunning();
    final ClientLock lockState = getClientLockState(lock);
    if (lockState == null) {
      return 0;
    } else {
      return lockState.holdCount(level);
    }
  }

  public int globalHoldCount(final LockID lock, final LockLevel level) {
    waitUntilRunning();

    int holdCount = 0;
    final ClientLock lockState = getClientLockState(lock);
    if (lockState != null) {
      holdCount += lockState.holdCount(level);
    }

    for (final ClientServerExchangeLockContext cselc : queryLock(lock)) {
      if (this.remoteManager.getClientID().equals(cselc.getNodeID())) {
        continue;
      }

      switch (cselc.getState()) {
        case GREEDY_HOLDER_READ:
        case HOLDER_READ:
          if (level == LockLevel.READ) {
            holdCount++;
          }
          break;
        case GREEDY_HOLDER_WRITE:
          holdCount++;
          break;
        case HOLDER_WRITE:
          if ((level == LockLevel.WRITE) || (level == LockLevel.SYNCHRONOUS_WRITE)) {
            holdCount++;
          }
          break;
        default:
          break;
      }
    }

    return holdCount;
  }

  public int globalPendingCount(final LockID lock) {
    waitUntilRunning();

    int pendingCount = 0;
    final ClientLock lockState = getClientLockState(lock);
    if (lockState != null) {
      pendingCount += lockState.pendingCount();
    }

    for (final ClientServerExchangeLockContext cselc : queryLock(lock)) {
      switch (cselc.getState()) {
        default:
          continue;
        case PENDING_READ:
        case PENDING_WRITE:
          pendingCount++;
      }
    }

    return pendingCount;
  }

  public int globalWaitingCount(final LockID lock) {
    waitUntilRunning();

    int waiterCount = 0;
    for (final ClientServerExchangeLockContext cselc : queryLock(lock)) {
      switch (cselc.getState()) {
        default:
          continue;
        case WAITER:
          waiterCount++;
      }
    }

    if (waiterCount > 0) { return waiterCount; }

    final ClientLock lockState = getClientLockState(lock);
    if (lockState != null) {
      return lockState.waitingCount();
    } else {
      return 0;
    }
  }

  public void pinLock(final LockID lock) {
    final ClientLock lockState = getClientLockState(lock);
    if (lockState != null) {
      lockState.pinLock();
    }
  }

  public void unpinLock(final LockID lock) {
    final ClientLock lockState = getClientLockState(lock);
    if (lockState != null) {
      lockState.unpinLock();
    }
  }

  public LockID generateLockIdentifier(final String str) {
    throw new AssertionError(getClass().getSimpleName() + " does not generate lock identifiers");
  }

  public LockID generateLockIdentifier(final long l) {
    throw new AssertionError(getClass().getSimpleName() + " does not generate lock identifiers");
  }

  public LockID generateLockIdentifier(final Object obj) {
    throw new AssertionError(getClass().getSimpleName() + " does not generate lock identifiers");
  }

  public LockID generateLockIdentifier(final Object obj, final String field) {
    throw new AssertionError(getClass().getSimpleName() + " does not generate lock identifiers");
  }

  /***********************************/
  /* END TerracottaLocking METHODS */
  /***********************************/

  /***********************************/
  /* BEGIN ClientLockManager METHODS */
  /***********************************/

  public void award(final NodeID node, final SessionID session, final LockID lock, final ThreadID thread,
                    final ServerLockLevel level) {
    this.stateGuard.readLock().lock();
    try {
      if (paused() || !this.sessionManager.isCurrentSession(node, session)) {
        this.logger.warn("Ignoring lock award from a dead server :" + session + ", " + this.sessionManager + " : "
                         + lock + " " + thread + " " + level + " state = " + this.state);
        return;
      }

      if (ThreadID.VM_ID.equals(thread)) {
        while (true) {
          final ClientLock lockState = getOrCreateClientLockState(lock);
          try {
            lockState.award(this.remoteManager, thread, level);
            break;
          } catch (final GarbageLockException e) {
            // ignorable - thrown when operating on a garbage collected lock
            // gc thread should clear this object soon - spin and re-get...
            this.logger.info("Hitting garbage lock state during award on " + lock);
          }
        }
      } else {
        final ClientLock lockState = getClientLockState(lock);
        if (lockState == null) {
          this.remoteManager.unlock(lock, thread, level);
        } else {
          try {
            lockState.award(this.remoteManager, thread, level);
          } catch (final GarbageLockException e) {
            this.remoteManager.unlock(lock, thread, level);
          }
        }
      }
    } finally {
      this.stateGuard.readLock().unlock();
    }
  }

  public void notified(final LockID lock, final ThreadID thread) {
    this.stateGuard.readLock().lock();
    try {
      if (paused()) {
        this.logger.warn("Ignoring notified call from dead server : " + lock + ", " + thread);
        return;
      }

      final ClientLock lockState = getClientLockState(lock);
      if (lockState == null) {
        throw new AssertionError("Server attempting to notify on non-existent lock " + lock);
      } else {
        lockState.notified(thread);
        return;
      }
    } finally {
      this.stateGuard.readLock().unlock();
    }
  }

  public void recall(final NodeID node, final SessionID session, final LockID lock, final ServerLockLevel level,
                     final int lease) {
    recall(node, session, lock, level, lease, false);
  }

  public void recall(final NodeID node, final SessionID session, final LockID lock, final ServerLockLevel level,
                     final int lease, final boolean batch) {
    this.stateGuard.readLock().lock();
    try {
      if (paused() || (node != null && !sessionManager.isCurrentSession(node, session))) {
        this.logger.warn("Ignoring recall request from a dead server :" + session + ", " + this.sessionManager + " : "
                         + lock + ", interestedLevel : " + level + " state: " + state);
        return;
      }

      final ClientLock lockState = getClientLockState(lock);
      if (lockState != null) {
        if (lockState.recall(this.remoteManager, level, lease, batch)) {
          // schedule the greedy lease
          this.lockLeaseTimer.schedule(new TimerTask() {
            @Override
            public void run() {
              try {
                ClientLockManagerImpl.this.recall(node, session, lock, level, -1, batch);
              } catch (TCNotRunningException e) {
                logger.info("Ignoring " + e.getMessage() + " in " + this.getClass().getName()
                            + " and cancelling timer task");
                this.cancel();
              }
            }
          }, lease);
        }
      }
    } finally {
      this.stateGuard.readLock().unlock();
    }
  }

  public void refuse(final NodeID node, final SessionID session, final LockID lock, final ThreadID thread,
                     final ServerLockLevel level) {
    this.stateGuard.readLock().lock();
    try {
      if (paused() || !this.sessionManager.isCurrentSession(node, session)) {
        this.logger.warn("Ignoring lock refuse from a dead server :" + session + ", " + this.sessionManager + " : "
                         + lock + " " + thread + " " + level + " state = " + this.state);
        return;
      }
      fireRefused(lock);

      final ClientLock lockState = getClientLockState(lock);
      if (lockState != null) {
        lockState.refuse(thread, level);
        return;
      }
    } finally {
      this.stateGuard.readLock().unlock();
    }
  }

  /*
   * State change that accompanies this notify is the mutation to "inFlightLockQueries"
   */
  @FindbugsSuppressWarnings("NN_NAKED_NOTIFY")
  public void info(final LockID lock, final ThreadID requestor,
                   final Collection<ClientServerExchangeLockContext> contexts) {
    this.stateGuard.readLock().lock();
    try {
      final Object old = this.inFlightLockQueries.put(requestor, contexts);
      synchronized (old) {
        old.notifyAll();
      }
    } finally {
      this.stateGuard.readLock().unlock();
    }
  }

  /***********************************/
  /* END ClientLockManager METHODS */
  /***********************************/

  /***********************************/
  /* BEGIN Stupid Wait Test METHODS */
  /***********************************/

  public void wait(final LockID lock, final WaitListener listener, final Object waitObject) throws InterruptedException {
    waitUntilRunning();
    final ClientLock lockState = getOrCreateClientLockState(lock);
    lockState.wait(this.remoteManager, listener, this.threadManager.getThreadID(), waitObject);
  }

  public void wait(final LockID lock, final WaitListener listener, final Object waitObject, final long timeout)
      throws InterruptedException {
    waitUntilRunning();
    final ClientLock lockState = getOrCreateClientLockState(lock);
    lockState.wait(this.remoteManager, listener, this.threadManager.getThreadID(), waitObject, timeout);
  }

  /***********************************/
  /* END Stupid Wait Test METHODS */
  /***********************************/

  /***********************************/
  /* BEGIN ClientHandshake METHODS */
  /***********************************/

  public void initializeHandshake(final NodeID thisNode, final NodeID remoteNode,
                                  final ClientHandshakeMessage handshakeMessage) {
    this.stateGuard.writeLock().lock();
    try {
      this.state = this.state.initialize();
      if (this.state == State.STARTING) {
        for (final ClientLock cls : this.locks.values()) {
          cls.initializeHandshake((ClientID) thisNode, handshakeMessage);
        }
      }
    } finally {
      this.stateGuard.writeLock().unlock();
    }
  }

  public void pause(final NodeID remoteNode, final int disconnected) {
    this.stateGuard.writeLock().lock();
    try {
      this.state = this.state.pause();
    } finally {
      this.stateGuard.writeLock().unlock();
    }
  }

  public void shutdown() {
    this.stateGuard.writeLock().lock();
    try {
      this.state = this.state.shutdown();
      this.gcTimer.cancel();
      this.lockLeaseTimer.cancel();
      this.remoteManager.shutdown();
      this.runningCondition.signalAll();
      LockStateNode.shutdown();
    } finally {
      this.stateGuard.writeLock().unlock();
    }
  }

  public void unpause(final NodeID remoteNode, final int disconnected) {
    this.stateGuard.writeLock().lock();
    try {
      this.state = this.state.unpause();
      if (this.state == State.RUNNING) {
        this.runningCondition.signalAll();
      }
    } finally {
      this.stateGuard.writeLock().unlock();
    }
    resubmitInFlightLockQueries();
  }

  /***********************************/
  /* END ClientHandshake METHODS */
  /***********************************/

  private void waitUntilRunning() {
    this.stateGuard.readLock().lock();
    try {
      if (this.state == State.RUNNING) { return; }
    } finally {
      this.stateGuard.readLock().unlock();
    }

    boolean interrupted = false;
    this.stateGuard.writeLock().lock();
    try {
      while (this.state != State.RUNNING) {
        try {
          if (isShutdown()) { throw new TCNotRunningException(); }
          this.runningCondition.await();
        } catch (final InterruptedException e) {
          interrupted = true;
        }
      }
    } finally {
      this.stateGuard.writeLock().unlock();
      Util.selfInterruptIfNeeded(interrupted);
    }

  }

  /**
   * Should be called under read lock
   */
  private boolean isShutdown() {
    return this.state == State.SHUTDOWN;
  }

  private boolean paused() {
    /*
     * I would like to wrap this read in a stateGuard read lock but due to the current RRWL instrumentation forcing RRWL
     * instances to use a fair policy and the associated "bug" in fair RRWL in JDK 1.5 I have to prevent reentrant
     * acquires of the read lock. (CDV-1434) <p> Its okay though since this is private and all callers have already read
     * locked.
     */
    return this.state == State.PAUSED;
  }

  static enum State {
    RUNNING {
      @Override
      State unpause() {
        throw new AssertionError("unpause is an invalid state transition for " + this);
      }

      @Override
      State pause() {
        return PAUSED;
      }

      @Override
      State initialize() {
        throw new AssertionError("initialize is an invalid state transition for " + this);
      }

      @Override
      State shutdown() {
        return SHUTDOWN;
      }
    },

    STARTING {
      @Override
      State unpause() {
        return RUNNING;
      }

      @Override
      State pause() {
        return PAUSED;
      }

      @Override
      State initialize() {
        throw new AssertionError("initialize is an invalid state transition for " + this);
      }

      @Override
      State shutdown() {
        return SHUTDOWN;
      }
    },

    PAUSED {
      @Override
      State unpause() {
        throw new AssertionError("unpause is an invalid state transition for " + this);
      }

      @Override
      State pause() {
        throw new AssertionError("pause is an invalid state transition for " + this);
      }

      @Override
      State initialize() {
        return STARTING;
      }

      @Override
      State shutdown() {
        return SHUTDOWN;
      }
    },

    SHUTDOWN {
      @Override
      State pause() {
        return SHUTDOWN;
      }

      @Override
      State unpause() {
        return SHUTDOWN;
      }

      @Override
      State initialize() {
        return SHUTDOWN;
      }

      @Override
      State shutdown() {
        return SHUTDOWN;
      }
    };

    abstract State pause();

    abstract State unpause();

    abstract State initialize();

    abstract State shutdown();
  }

  public PrettyPrinter prettyPrint(final PrettyPrinter out) {
    out.print("ClientLockManagerImpl [" + this.locks.size() + " locks]").flush();
    for (final ClientLock lock : this.locks.values()) {
      out.indent().print(lock).flush();
    }
    return out;
  }

  public Collection<ClientServerExchangeLockContext> getAllLockContexts() {
    final Collection<ClientServerExchangeLockContext> contexts = new ArrayList<ClientServerExchangeLockContext>();
    for (final ClientLock lock : this.locks.values()) {
      contexts.addAll(lock.getStateSnapshot(this.remoteManager.getClientID()));
    }
    return contexts;
  }

  private Collection<ClientServerExchangeLockContext> queryLock(final LockID lock) {
    final ThreadID current = this.threadManager.getThreadID();

    this.inFlightLockQueries.put(current, lock);
    this.remoteManager.query(lock, this.threadManager.getThreadID());

    boolean interrupted = false;
    try {
      while (true) {
        synchronized (lock) {
          final Object data = this.inFlightLockQueries.get(current);
          if (data instanceof Collection) {
            return (Collection<ClientServerExchangeLockContext>) data;
          } else {
            try {
              lock.wait();
            } catch (final InterruptedException e) {
              interrupted = true;
            }
          }
        }
      }
    } finally {
      Util.selfInterruptIfNeeded(interrupted);
    }
  }

  private void resubmitInFlightLockQueries() {
    for (final Entry<ThreadID, Object> query : this.inFlightLockQueries.entrySet()) {
      if (query.getValue() instanceof LockID) {
        this.remoteManager.query((LockID) query.getValue(), query.getKey());
      }
    }
  }

  class LockGcTimerTask extends TimerTask {
    private static final int GCED_LOCK_THRESHOLD = 1000;

    @Override
    public void run() {
      try {
        int gcCount = 0;
        for (final Entry<LockID, ClientLock> entry : ClientLockManagerImpl.this.locks.entrySet()) {
          ClientLockManagerImpl.this.stateGuard.readLock().lock();
          try {
            if (ClientLockManagerImpl.this.state != State.RUNNING) { return; }

            final LockID lock = entry.getKey();
            final ClientLock lockState = entry.getValue();
            if (lockState == null) {
              continue;
            }

            if (lockState.tryMarkAsGarbage(ClientLockManagerImpl.this.remoteManager)
                && ClientLockManagerImpl.this.locks.remove(lock, lockState)) {
              gcCount++;
            }
          } finally {
            ClientLockManagerImpl.this.stateGuard.readLock().unlock();
          }
        }
        if (gcCount > 0) {
          ClientLockManagerImpl.this.logger.info("Lock GC collected " + gcCount + " garbage locks");
        }

        if (gcCount > GCED_LOCK_THRESHOLD) {
          for (LockEventListener lockGCEventListener : lockEventListeners) {
            lockGCEventListener.fireLockGCEvent(gcCount);
          }
        }
      } catch (TCNotRunningException e) {
        logger.info("Ignoring " + e.getMessage() + " in " + this.getClass().getName() + " and cancelling timer task");
        this.cancel();
      }
    }
  }

  public int runLockGc() {
    new LockGcTimerTask().run();
    return this.locks.size();
  }

  @Deprecated
  private void fireLockAttempted(final LockID lock) {
    if (this.statManager.isEnabled()) {
      final ClientLock lockState = getClientLockState(lock);
      final int pendingCount = lockState == null ? 0 : lockState.pendingCount();
      this.statManager.recordLockRequested(lock, this.threadManager.getThreadID(), "", pendingCount);
    }
  }

  @Deprecated
  private void fireLockSucceeded(final LockID lock) {
    if (this.statManager.isEnabled()) {
      this.statManager.recordLockAwarded(lock, this.threadManager.getThreadID());
    }
  }

  @Deprecated
  private void fireUnlock(final LockID lock) {
    if (this.statManager.isEnabled()) {
      this.statManager.recordLockReleased(lock, this.threadManager.getThreadID());
    }
  }

  @Deprecated
  private void fireRefused(final LockID lock) {
    if (this.statManager.isEnabled()) {
      this.statManager.recordLockRejected(lock, this.threadManager.getThreadID());
    }
  }

}
