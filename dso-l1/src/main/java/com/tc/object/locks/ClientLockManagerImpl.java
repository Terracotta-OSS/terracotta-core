/* 
 * The contents of this file are subject to the Terracotta Public License Version
 * 2.0 (the "License"); You may not use this file except in compliance with the
 * License. You may obtain a copy of the License at 
 *
 *      http://terracotta.org/legal/terracotta-public-license.
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 * the specific language governing rights and limitations under the License.
 *
 * The Covered Software is Terracotta Platform.
 *
 * The Initial Developer of the Covered Software is 
 *      Terracotta, Inc., a Software AG company
 */
package com.tc.object.locks;

import com.tc.abortable.AbortableOperationManager;
import com.tc.abortable.AbortedOperationException;
import com.tc.exception.PlatformRejoinException;
import com.tc.exception.TCNotRunningException;
import com.tc.logging.TCLogger;
import com.tc.net.ClientID;
import com.tc.net.NodeID;
import com.tc.object.msg.ClientHandshakeMessage;
import com.tc.object.session.SessionID;
import com.tc.object.session.SessionManager;
import com.tc.operatorevent.LockEventListener;
import com.tc.text.PrettyPrintable;
import com.tc.text.PrettyPrinter;
import com.tc.util.AbortedOperationUtil;
import com.tc.util.FindbugsSuppressWarnings;
import com.tc.util.Util;
import com.tc.util.concurrent.TaskRunner;
import com.tc.util.concurrent.Timer;
import com.tc.util.runtime.ThreadIDManager;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class ClientLockManagerImpl implements ClientLockManager, ClientLockManagerTestMethods, PrettyPrintable {
  private static final WaitListener               NULL_LISTENER       = new WaitListener() {
                                                                        @Override
                                                                        public void handleWaitEvent() {
                                                                          //
                                                                        }
                                                                      };

  private final ConcurrentMap<LockID, ClientLock> locks;
  private final ReentrantReadWriteLock            stateGuard          = new ReentrantReadWriteLock();
  private final Condition                         runningCondition    = this.stateGuard.writeLock().newCondition();
  private State                                   state               = State.RUNNING;

  private final RemoteLockManager                 remoteLockManager;
  private final ThreadIDManager                   threadManager;
  private final SessionManager                    sessionManager;
  private final TCLogger                          logger;

  private final ConcurrentMap<ThreadID, Object>   inFlightLockQueries = new ConcurrentHashMap<ThreadID, Object>();
  private final List<LockEventListener>           lockEventListeners  = new CopyOnWriteArrayList<LockEventListener>();

  private final AbortableOperationManager         abortableOperationManager;

  private final Timer                             gcTimer;
  private final Timer                             lockLeaseTimer;
  private final AtomicLong                        lockAwardSequence   = new AtomicLong();

  public ClientLockManagerImpl(final TCLogger logger, final SessionManager sessionManager,
                               final RemoteLockManager remoteLockManager, final ThreadIDManager threadManager,
                               final ClientLockManagerConfig config,
                               final AbortableOperationManager abortableOperationManager, final TaskRunner taskRunner) {
    this.logger = logger;
    this.remoteLockManager = remoteLockManager;
    this.threadManager = threadManager;
    this.sessionManager = sessionManager;
    this.abortableOperationManager = abortableOperationManager;
    this.locks = new ConcurrentHashMap<LockID, ClientLock>(config.getStripedCount());
    final long gcPeriod = Math.max(config.getTimeoutInterval(), 100);
    this.gcTimer = taskRunner.newTimer("ClientLockManager LockGC");
    this.lockLeaseTimer = taskRunner.newTimer("ClientLockManager Lock Lease Timer");
    this.gcTimer.scheduleWithFixedDelay(new LockGcTimerTask(), gcPeriod, gcPeriod, TimeUnit.MILLISECONDS);
  }

  @Override
  public void cleanup() {
    this.stateGuard.writeLock().lock();
    try {
      checkAndSetstate();
      for (ClientLock clientLock : locks.values()) {
        clientLock.cleanup();
      }
      locks.clear();
      remoteLockManager.cleanup();
      inFlightLockQueries.clear();
    } finally {
      this.stateGuard.writeLock().unlock();
    }
  }

  private void checkAndSetstate() {
    state = state.rejoin_in_progress();
    runningCondition.signalAll();
  }

  private ClientLock getOrCreateClientLockState(final LockID lock) {
    stateGuard.readLock().lock();
    try {
      throwExceptionIfNecessary();
      ClientLock lockState = this.locks.get(lock);
      if (lockState == null) {
        lockState = new ClientLockImpl(lock);
        final ClientLock racer = this.locks.putIfAbsent(lock, lockState);
        if (racer != null) { return racer; }
      }
      return lockState;
    } finally {
      stateGuard.readLock().unlock();
    }
  }

  private ClientLock getClientLockState(final LockID lock) {
    return this.locks.get(lock);
  }

  /***********************************/
  /* BEGIN TerracottaLocking METHODS */
  /**
   * @throws AbortedOperationException
   *********************************/

  @Override
  public void lock(final LockID lock, final LockLevel level) throws AbortedOperationException {
    waitUntilRunning();

    while (true) {
      final ClientLock lockState = getOrCreateClientLockState(lock);
      try {
        lockState.lock(this.abortableOperationManager, this.remoteLockManager, this.threadManager.getThreadID(), level);
        break;
      } catch (final GarbageLockException e) {
        // ignorable - thrown when operating on a garbage collected lock
        // gc thread should clear this object soon - spin and re-get...
        this.logger.info("Hitting garbage lock state during lock on " + lock);
      }
    }
  }

  @Override
  public boolean tryLock(final LockID lock, final LockLevel level) throws AbortedOperationException {
    waitUntilRunning();

    while (true) {
      final ClientLock lockState = getOrCreateClientLockState(lock);
      try {
        if (lockState.tryLock(this.abortableOperationManager, this.remoteLockManager, this.threadManager.getThreadID(),
                              level)) {
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

  @Override
  public boolean tryLock(final LockID lock, final LockLevel level, final long timeout) throws InterruptedException,
      AbortedOperationException {
    if (timeout < 0) { throw new IllegalArgumentException("tryLock is passed with negative timeout, timeout=" + timeout); }

    waitUntilRunning();

    while (true) {
      final ClientLock lockState = getOrCreateClientLockState(lock);
      try {
        if (lockState.tryLock(this.abortableOperationManager, this.remoteLockManager, this.threadManager.getThreadID(),
                              level, timeout)) {
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

  @Override
  public void lockInterruptibly(final LockID lock, final LockLevel level) throws InterruptedException,
      AbortedOperationException {
    waitUntilRunning();

    while (true) {
      final ClientLock lockState = getOrCreateClientLockState(lock);
      try {
        lockState.lockInterruptibly(this.abortableOperationManager, this.remoteLockManager,
                                    this.threadManager.getThreadID(), level);
        break;
      } catch (final GarbageLockException e) {
        // ignorable - thrown when operating on a garbage collected lock
        // gc thread should clear this object soon - spin and re-get...
        this.logger.info("Hitting garbage lock state during lockInterruptibly on " + lock);
      }
    }
  }

  @Override
  public void unlock(final LockID lock, final LockLevel level) throws AbortedOperationException {
    final ClientLock lockState = getOrCreateClientLockState(lock);
    lockState.unlock(this.remoteLockManager, this.threadManager.getThreadID(), level);
  }

  @Override
  public Notify notify(final LockID lock, final Object waitObject) throws AbortedOperationException {
    waitUntilRunning();
    final ClientLock lockState = getOrCreateClientLockState(lock);
    final ThreadID thread = this.threadManager.getThreadID();
    if (lockState.notify(this.remoteLockManager, thread, null)) {
      return new NotifyImpl(lock, thread, false);
    } else {
      return NotifyImpl.NULL;
    }
  }

  @Override
  public Notify notifyAll(final LockID lock, final Object waitObject) throws AbortedOperationException {
    waitUntilRunning();
    final ClientLock lockState = getOrCreateClientLockState(lock);
    final ThreadID thread = this.threadManager.getThreadID();
    if (lockState.notifyAll(this.remoteLockManager, thread, null)) {
      return new NotifyImpl(lock, thread, true);
    } else {
      return NotifyImpl.NULL;
    }
  }

  @Override
  public void wait(final LockID lock, final Object waitObject) throws InterruptedException, AbortedOperationException {
    wait(lock, NULL_LISTENER, waitObject);
  }

  @Override
  public void wait(final LockID lock, final Object waitObject, final long timeout) throws InterruptedException,
      AbortedOperationException {
    wait(lock, NULL_LISTENER, waitObject, timeout);
  }

  @Override
  public boolean isLocked(final LockID lock, final LockLevel level) throws AbortedOperationException {
    waitUntilRunning();
    final ClientLock lockState = getClientLockState(lock);
    if (lockState != null) {
      if (lockState.isLocked(level)) { return true; }
    }

    for (final ClientServerExchangeLockContext cselc : queryLock(lock)) {
      if (this.remoteLockManager.getClientID().equals(cselc.getNodeID())) {
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

  @Override
  public boolean isLockedByCurrentThread(final LockID lock, final LockLevel level) throws AbortedOperationException {
    waitUntilRunning();
    final ClientLock lockState = getClientLockState(lock);
    if (lockState == null) {
      return false;
    } else {
      return lockState.isLockedBy(this.threadManager.getThreadID(), level);
    }
  }

  @Override
  public boolean isLockedByCurrentThread(final LockLevel level) {
    final ThreadID thread = this.threadManager.getThreadID();
    for (final ClientLock lockState : this.locks.values()) {
      if (lockState.isLockedBy(thread, level)) { return true; }
    }
    return false;
  }

  @Override
  public int localHoldCount(final LockID lock, final LockLevel level) throws AbortedOperationException {
    waitUntilRunning();
    final ClientLock lockState = getClientLockState(lock);
    if (lockState == null) {
      return 0;
    } else {
      return lockState.holdCount(level);
    }
  }

  @Override
  public int globalHoldCount(final LockID lock, final LockLevel level) throws AbortedOperationException {
    waitUntilRunning();

    int holdCount = 0;
    final ClientLock lockState = getClientLockState(lock);
    if (lockState != null) {
      holdCount += lockState.holdCount(level);
    }

    for (final ClientServerExchangeLockContext cselc : queryLock(lock)) {
      if (this.remoteLockManager.getClientID().equals(cselc.getNodeID())) {
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

  @Override
  public int globalPendingCount(final LockID lock) throws AbortedOperationException {
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

  @Override
  public int globalWaitingCount(final LockID lock) throws AbortedOperationException {
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

  @Override
  public void pinLock(final LockID lock, long awardID) {
    final ClientLock lockState = getClientLockState(lock);
    if (lockState != null) {
      lockState.pinLock(awardID);
    }
  }

  @Override
  public void unpinLock(final LockID lock, long awardID) {
    final ClientLock lockState = getClientLockState(lock);
    if (lockState != null) {
      lockState.unpinLock(awardID);
    }
  }

  @Override
  public LockID generateLockIdentifier(final String str) {
    throw new AssertionError(getClass().getSimpleName() + " does not generate lock identifiers");
  }

  @Override
  public LockID generateLockIdentifier(final long l) {
    throw new AssertionError(getClass().getSimpleName() + " does not generate lock identifiers");
  }

  @Override
  public LockID generateLockIdentifier(final Object obj) {
    throw new AssertionError(getClass().getSimpleName() + " does not generate lock identifiers");
  }

  @Override
  public LockID generateLockIdentifier(final Object obj, final String field) {
    throw new AssertionError(getClass().getSimpleName() + " does not generate lock identifiers");
  }

  /***********************************/
  /* END TerracottaLocking METHODS */
  /***********************************/

  /***********************************/
  /* BEGIN ClientLockManager METHODS */
  /***********************************/

  @Override
  public void award(final NodeID node, final SessionID session, final LockID lock, final ThreadID thread,
                    final ServerLockLevel level) {
    this.stateGuard.readLock().lock();
    try {
      if (isPausedOrRejoinInProgress() || !this.sessionManager.isCurrentSession(node, session)) {
        this.logger.warn("Ignoring lock award from a dead server :" + session + ", " + this.sessionManager + " : "
                         + lock + " " + thread + " " + level + " state = " + this.state);
        return;
      }

      if (ThreadID.VM_ID.equals(thread)) {
        while (true) {
          final ClientLock lockState = getOrCreateClientLockState(lock);
          try {
            lockState.award(this.remoteLockManager, thread, level, lockAwardSequence.incrementAndGet());
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
          this.remoteLockManager.unlock(lock, thread, level);
        } else {
          try {
            lockState.award(this.remoteLockManager, thread, level, lockAwardSequence.incrementAndGet());
          } catch (final GarbageLockException e) {
            this.remoteLockManager.unlock(lock, thread, level);
          }
        }
      }
    } finally {
      this.stateGuard.readLock().unlock();
    }
  }

  @Override
  public void notified(final LockID lock, final ThreadID thread) {
    this.stateGuard.readLock().lock();
    try {
      if (isPausedOrRejoinInProgress()) {
        this.logger.warn("Ignoring notified call from dead server : " + lock + ", " + thread);
        return;
      }

      if (this.logger.isDebugEnabled()) {
        this.logger.debug("Got a notification for " + lock + " with thread " + thread);
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

  @Override
  public void recall(final NodeID node, final SessionID session, final LockID lock, final ServerLockLevel level,
                     final int lease) {
    recall(node, session, lock, level, lease, false);
  }

  @Override
  public void recall(final NodeID node, final SessionID session, final LockID lock, final ServerLockLevel level,
                     final int lease, final boolean batch) {
    this.stateGuard.readLock().lock();
    try {
      if (isPausedOrRejoinInProgress() || isShutdown()
          || (node != null && !sessionManager.isCurrentSession(node, session))) {
        this.logger.warn("Ignoring recall request from a dead server :" + session + ", " + this.sessionManager + " : "
                         + lock + ", interestedLevel : " + level + " state: " + state);
        return;
      }

      final ClientLock lockState = getClientLockState(lock);
      if (lockState != null) {
        if (lockState.recall(this.remoteLockManager, level, lease, batch)) {
          // schedule the greedy lease
          lockLeaseTimer.schedule(new LeaseTask(node, session, lock, level, batch), lease, TimeUnit.MILLISECONDS);
        }
      }
    } finally {
      this.stateGuard.readLock().unlock();
    }
  }

  @Override
  public void refuse(final NodeID node, final SessionID session, final LockID lock, final ThreadID thread,
                     final ServerLockLevel level) {
    this.stateGuard.readLock().lock();
    try {
      if (isPausedOrRejoinInProgress() || !this.sessionManager.isCurrentSession(node, session)) {
        this.logger.warn("Ignoring lock refuse from a dead server :" + session + ", " + this.sessionManager + " : "
                         + lock + " " + thread + " " + level + " state = " + this.state);
        return;
      }

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
  @Override
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
  /**
   * @throws AbortedOperationException
   *********************************/

  @Override
  public void wait(final LockID lock, final WaitListener listener, final Object waitObject)
      throws InterruptedException, AbortedOperationException {
    waitUntilRunning();
    final ClientLock lockState = getOrCreateClientLockState(lock);
    if (logger.isDebugEnabled()) {
      logger.debug(this.threadManager.getThreadID() + " waiting on log " + lock);
    }
    lockState.wait(this.abortableOperationManager, this.remoteLockManager, listener, this.threadManager.getThreadID(),
                   waitObject);
  }

  @Override
  public void wait(final LockID lock, final WaitListener listener, final Object waitObject, final long timeout)
      throws InterruptedException, AbortedOperationException {
    waitUntilRunning();
    final ClientLock lockState = getOrCreateClientLockState(lock);
    if (logger.isDebugEnabled()) {
      logger.debug(this.threadManager.getThreadID() + " waiting on log " + lock);
    }
    lockState.wait(this.abortableOperationManager, this.remoteLockManager, listener, this.threadManager.getThreadID(),
                   waitObject, timeout);
  }

  /***********************************/
  /* END Stupid Wait Test METHODS */
  /***********************************/

  /***********************************/
  /* BEGIN ClientHandshake METHODS */
  /***********************************/

  @Override
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

  @Override
  public void pause(final NodeID remoteNode, final int disconnected) {
    this.stateGuard.writeLock().lock();
    try {
      this.state = this.state.pause();
    } finally {
      this.stateGuard.writeLock().unlock();
    }
  }

  @Override
  public void shutdown(boolean fromShutdownHook) {
    stateGuard.writeLock().lock();
    try {
      state = state.shutdown();
      gcTimer.cancel();
      lockLeaseTimer.cancel();
      remoteLockManager.shutdown();
      runningCondition.signalAll();
      LockStateNode.shutdown();
    } finally {
      stateGuard.writeLock().unlock();
    }
  }

  @Override
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
  /**
   * @throws AbortedOperationException
   *********************************/

  private void waitUntilRunning() throws AbortedOperationException {
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
          if (isRejoinInProgress()) { throw new PlatformRejoinException(); }
          this.runningCondition.await();
        } catch (final InterruptedException e) {
          AbortedOperationUtil.throwExceptionIfAborted(abortableOperationManager);
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
  private void throwExceptionIfNecessary() {
    if (isShutdown()) { throw new TCNotRunningException(); }
    if (isRejoinInProgress()) { throw new PlatformRejoinException(); }
  }

  /**
   * Should be called under read lock
   */
  private boolean isShutdown() {
    return this.state == State.SHUTDOWN;
  }

  @SuppressWarnings("unused")
  private boolean paused() {
    /*
     * I would like to wrap this read in a stateGuard read lock but due to the current RRWL instrumentation forcing RRWL
     * instances to use a fair policy and the associated "bug" in fair RRWL in JDK 1.5 I have to prevent reentrant
     * acquires of the read lock. (CDV-1434) <p> Its okay though since this is private and all callers have already read
     * locked.
     */
    return this.state == State.PAUSED;
  }

  private boolean isRejoinInProgress() {
    return this.state == State.REJOIN_IN_PROGRESS;
  }

  private boolean isPausedOrRejoinInProgress() {
    State current = this.state;
    return current == State.PAUSED || current == State.REJOIN_IN_PROGRESS;
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
      State rejoin_in_progress() {
        throw new AssertionError("rejoin_in_progress is an invalid state transition for " + this);
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
      State rejoin_in_progress() {
        throw new AssertionError("rejoin_in_progress is an invalid state transition for " + this);
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
      State rejoin_in_progress() {
        return REJOIN_IN_PROGRESS;
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
      State rejoin_in_progress() {
        return SHUTDOWN;
      }

      @Override
      State shutdown() {
        return SHUTDOWN;
      }
    },

    REJOIN_IN_PROGRESS {
      @Override
      State pause() {
        throw new AssertionError("pause is an invalid state transition for " + this);
      }

      @Override
      State unpause() {
        throw new AssertionError("unpause is an invalid state transition for " + this);
      }

      @Override
      State initialize() {
        return STARTING;
      }

      @Override
      State rejoin_in_progress() {
        throw new AssertionError("rejoin_in_progress is an invalid state transition for " + this);
      }

      @Override
      State shutdown() {
        return SHUTDOWN;
      }
    };

    abstract State pause();

    abstract State unpause();

    abstract State initialize();

    abstract State rejoin_in_progress();

    abstract State shutdown();

  }

  @Override
  public PrettyPrinter prettyPrint(final PrettyPrinter out) {
    out.print("ClientLockManagerImpl [" + this.locks.size() + " locks]").flush();
    for (final ClientLock lock : this.locks.values()) {
      out.indent().print(lock).flush();
    }
    return out;
  }

  @Override
  public Collection<ClientServerExchangeLockContext> getAllLockContexts() {
    final Collection<ClientServerExchangeLockContext> contexts = new ArrayList<ClientServerExchangeLockContext>();
    for (final ClientLock lock : this.locks.values()) {
      contexts.addAll(lock.getStateSnapshot(this.remoteLockManager.getClientID()));
    }
    return contexts;
  }

  private Collection<ClientServerExchangeLockContext> queryLock(final LockID lock) {
    final ThreadID current = this.threadManager.getThreadID();

    this.inFlightLockQueries.put(current, lock);
    this.remoteLockManager.query(lock, this.threadManager.getThreadID());

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
        this.remoteLockManager.query((LockID) query.getValue(), query.getKey());
      }
    }
  }

  class LeaseTask implements Runnable {
    private final NodeID          node;
    private final SessionID       session;
    private final LockID          lock;
    private final ServerLockLevel level;
    private final boolean         batch;

    public LeaseTask(NodeID node, SessionID session, LockID lock, ServerLockLevel level, boolean batch) {
      this.node = node;
      this.session = session;
      this.lock = lock;
      this.level = level;
      this.batch = batch;
    }

    @Override
    public void run() {
      try {
        ClientLockManagerImpl.this.recall(node, session, lock, level, -1, batch);
      } catch (TCNotRunningException e) {
        logger.info("Ignoring " + e.getMessage() + " in " + this.getClass().getName() + " and cancelling timer task");
      } catch (PlatformRejoinException e) {
        logger.info("Ignoring " + e.getMessage() + " in " + this.getClass().getName());
      }
    }
  }

  class LockGcTimerTask implements Runnable {
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

            if (lockState.tryMarkAsGarbage(ClientLockManagerImpl.this.remoteLockManager)
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
      } catch (PlatformRejoinException e) {
        logger.info("Ignoring " + e.getMessage() + " in " + this.getClass().getName());
      } catch (TCNotRunningException e) {
        logger.info("Ignoring " + e.getMessage() + " in " + this.getClass().getName() + " and cancelling timer task");
      }
    }
  }

  @Override
  public int runLockGc() {
    new LockGcTimerTask().run();
    return this.locks.size();
  }

  @Override
  public boolean isLockAwardValid(final LockID lock, final long awardID) {
    final ClientLock lockState = getClientLockState(lock);
    if (lockState == null) {
      return false;
    } else {
      return lockState.isAwardValid(awardID);
    }
  }

  @Override
  public long getAwardIDFor(final LockID lock) {
    final ClientLock lockState = getClientLockState(lock);
    if (lockState == null) {
      throw new IllegalStateException("LockState shouldn't be null");
    } else {
      return lockState.getAwardID();
    }

  }

}
