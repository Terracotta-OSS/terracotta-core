/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.locks;

import com.tc.logging.TCLogger;
import com.tc.management.ClientLockStatManager;
import com.tc.net.ClientID;
import com.tc.net.NodeID;
import com.tc.object.msg.ClientHandshakeMessage;
import com.tc.object.session.SessionID;
import com.tc.object.session.SessionManager;
import com.tc.text.PrettyPrintable;
import com.tc.text.PrettyPrinter;
import com.tc.util.Util;
import com.tc.util.runtime.ThreadIDManager;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Timer;
import java.util.TimerTask;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
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
  private final Condition                         runningCondition    = stateGuard.writeLock().newCondition();
  private State                                   state               = State.RUNNING;

  private final RemoteLockManager                 remoteManager;
  private final ThreadIDManager                   threadManager;
  private final SessionManager                    sessionManager;
  private final TCLogger                          logger;

  private final ConcurrentMap<ThreadID, Object>   inFlightLockQueries = new ConcurrentHashMap<ThreadID, Object>();

  @Deprecated
  private final ClientLockStatManager             statManager;

  public ClientLockManagerImpl(TCLogger logger, SessionManager sessionManager, RemoteLockManager remoteManager,
                               ThreadIDManager threadManager, ClientLockManagerConfig config,
                               ClientLockStatManager statManager) {
    this.logger = logger;
    this.remoteManager = remoteManager;
    this.threadManager = threadManager;
    this.sessionManager = sessionManager;

    this.statManager = statManager;
    locks = new ConcurrentHashMap<LockID, ClientLock>(config.getStripedCount());
    long gcPeriod = Math.max(config.getTimeoutInterval(), 100);
    gcTimer.schedule(new LockGcTimerTask(), gcPeriod, gcPeriod);
  }

  private ClientLock getOrCreateClientLockState(LockID lock) {
    ClientLock lockState = locks.get(lock);
    if (lockState == null) {
      lockState = new ClientLockImpl(lock);
      ClientLock racer = locks.putIfAbsent(lock, lockState);
      if (racer != null) { return racer; }
    }

    return lockState;
  }

  private ClientLock getClientLockState(LockID lock) {
    return locks.get(lock);
  }

  /***********************************/
  /* BEGIN TerracottaLocking METHODS */
  /***********************************/

  public void lock(LockID lock, LockLevel level) {
    waitUntilRunning();

    fireLockAttempted(lock);

    while (true) {
      ClientLock lockState = getOrCreateClientLockState(lock);
      try {
        lockState.lock(remoteManager, threadManager.getThreadID(), level);
        break;
      } catch (GarbageLockException e) {
        // ignorable - thrown when operating on a garbage collected lock
        // gc thread should clear this object soon - spin and re-get...
        logger.info("Hitting garbage lock state during lock on " + lock);
      }
    }

    fireLockSucceeded(lock);
  }

  public boolean tryLock(LockID lock, LockLevel level) {
    waitUntilRunning();

    fireLockAttempted(lock);

    while (true) {
      ClientLock lockState = getOrCreateClientLockState(lock);
      try {
        if (lockState.tryLock(remoteManager, threadManager.getThreadID(), level)) {
          fireLockSucceeded(lock);
          return true;
        } else {
          return false;
        }
      } catch (GarbageLockException e) {
        // ignorable - thrown when operating on a garbage collected lock
        // gc thread should clear this object soon - spin and re-get...
        logger.info("Hitting garbage lock state during tryLock on " + lock);
      }
    }
  }

  public boolean tryLock(LockID lock, LockLevel level, long timeout) throws InterruptedException {
    waitUntilRunning();

    fireLockAttempted(lock);

    while (true) {
      ClientLock lockState = getOrCreateClientLockState(lock);
      try {
        if (lockState.tryLock(remoteManager, threadManager.getThreadID(), level, timeout)) {
          fireLockSucceeded(lock);
          return true;
        } else {
          return false;
        }
      } catch (GarbageLockException e) {
        // ignorable - thrown when operating on a garbage collected lock
        // gc thread should clear this object soon - spin and re-get...
        logger.info("Hitting garbage lock state during tryLock with timeout on " + lock);
      }
    }
  }

  public void lockInterruptibly(LockID lock, LockLevel level) throws InterruptedException {
    waitUntilRunning();

    fireLockAttempted(lock);

    while (true) {
      ClientLock lockState = getOrCreateClientLockState(lock);
      try {
        lockState.lockInterruptibly(remoteManager, threadManager.getThreadID(), level);
        break;
      } catch (GarbageLockException e) {
        // ignorable - thrown when operating on a garbage collected lock
        // gc thread should clear this object soon - spin and re-get...
        logger.info("Hitting garbage lock state during lockInterruptibly on " + lock);
      }
    }

    fireLockSucceeded(lock);
  }

  public void unlock(LockID lock, LockLevel level) {
    waitUntilRunning();
    ClientLock lockState = getOrCreateClientLockState(lock);
    lockState.unlock(remoteManager, threadManager.getThreadID(), level);

    fireUnlock(lock);
  }

  public Notify notify(LockID lock, Object waitObject) {
    waitUntilRunning();
    ClientLock lockState = getOrCreateClientLockState(lock);
    ThreadID thread = threadManager.getThreadID();
    if (lockState.notify(remoteManager, thread, null)) {
      return new NotifyImpl(lock, thread, false);
    } else {
      return NotifyImpl.NULL;
    }
  }

  public Notify notifyAll(LockID lock, Object waitObject) {
    waitUntilRunning();
    ClientLock lockState = getOrCreateClientLockState(lock);
    ThreadID thread = threadManager.getThreadID();
    if (lockState.notifyAll(remoteManager, thread, null)) {
      return new NotifyImpl(lock, thread, true);
    } else {
      return NotifyImpl.NULL;
    }
  }

  public void wait(LockID lock, Object waitObject) throws InterruptedException {
    wait(lock, NULL_LISTENER, waitObject);
  }

  public void wait(LockID lock, Object waitObject, long timeout) throws InterruptedException {
    wait(lock, NULL_LISTENER, waitObject, timeout);
  }

  public boolean isLocked(LockID lock, LockLevel level) {
    waitUntilRunning();
    ClientLock lockState = getClientLockState(lock);
    if (lockState != null) {
      if (lockState.isLocked(level)) { return true; }
    }

    for (ClientServerExchangeLockContext cselc : queryLock(lock)) {
      if (remoteManager.getClientID().equals(cselc.getNodeID())) {
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

  public boolean isLockedByCurrentThread(LockID lock, LockLevel level) {
    waitUntilRunning();
    ClientLock lockState = getClientLockState(lock);
    if (lockState == null) {
      return false;
    } else {
      return lockState.isLockedBy(threadManager.getThreadID(), level);
    }
  }

  public boolean isLockedByCurrentThread(LockLevel level) {
    ThreadID thread = threadManager.getThreadID();
    for (ClientLock lockState : locks.values()) {
      if (lockState.isLockedBy(thread, level)) { return true; }
    }
    return false;
  }

  public int localHoldCount(LockID lock, LockLevel level) {
    waitUntilRunning();
    ClientLock lockState = getClientLockState(lock);
    if (lockState == null) {
      return 0;
    } else {
      return lockState.holdCount(level);
    }
  }

  public int globalHoldCount(LockID lock, LockLevel level) {
    waitUntilRunning();

    int holdCount = 0;
    ClientLock lockState = getClientLockState(lock);
    if (lockState != null) {
      holdCount += lockState.holdCount(level);
    }

    for (ClientServerExchangeLockContext cselc : queryLock(lock)) {
      if (remoteManager.getClientID().equals(cselc.getNodeID())) {
        continue;
      }

      switch (cselc.getState()) {
        case GREEDY_HOLDER_READ:
        case HOLDER_READ:
          if (level == LockLevel.READ) holdCount++;
          break;
        case GREEDY_HOLDER_WRITE:
          holdCount++;
          break;
        case HOLDER_WRITE:
          if ((level == LockLevel.WRITE) || (level == LockLevel.SYNCHRONOUS_WRITE)) holdCount++;
          break;
        default:
          break;
      }
    }

    return holdCount;
  }

  public int globalPendingCount(LockID lock) {
    waitUntilRunning();

    int pendingCount = 0;
    ClientLock lockState = getClientLockState(lock);
    if (lockState != null) {
      pendingCount += lockState.pendingCount();
    }

    for (ClientServerExchangeLockContext cselc : queryLock(lock)) {
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

  public int globalWaitingCount(LockID lock) {
    waitUntilRunning();

    int waiterCount = 0;
    for (ClientServerExchangeLockContext cselc : queryLock(lock)) {
      switch (cselc.getState()) {
        default:
          continue;
        case WAITER:
          waiterCount++;
      }
    }

    if (waiterCount > 0) { return waiterCount; }

    ClientLock lockState = getClientLockState(lock);
    if (lockState != null) {
      return lockState.waitingCount();
    } else {
      return 0;
    }
  }

  public void pinLock(LockID lock) {
    ClientLock lockState = getClientLockState(lock);
    if (lockState != null) {
      lockState.pinLock();
    }
  }

  public void unpinLock(LockID lock) {
    ClientLock lockState = getClientLockState(lock);
    if (lockState != null) {
      lockState.unpinLock();
    }
  }

  public LockID generateLockIdentifier(String str) {
    throw new AssertionError(getClass().getSimpleName() + " does not generate lock identifiers");
  }

  public LockID generateLockIdentifier(Object obj) {
    throw new AssertionError(getClass().getSimpleName() + " does not generate lock identifiers");
  }

  public LockID generateLockIdentifier(Object obj, String field) {
    throw new AssertionError(getClass().getSimpleName() + " does not generate lock identifiers");
  }

  /***********************************/
  /* END TerracottaLocking METHODS */
  /***********************************/

  /***********************************/
  /* BEGIN ClientLockManager METHODS */
  /***********************************/

  public void award(NodeID node, SessionID session, LockID lock, ThreadID thread, ServerLockLevel level) {
    stateGuard.readLock().lock();
    try {
      if (paused() || !sessionManager.isCurrentSession(node, session)) {
        logger.warn("Ignoring lock award from a dead server :" + session + ", " + sessionManager + " : " + lock + " "
                    + thread + " " + level + " state = " + state);
        return;
      }

      if (ThreadID.VM_ID.equals(thread)) {
        while (true) {
          ClientLock lockState = getOrCreateClientLockState(lock);
          try {
            lockState.award(remoteManager, thread, level);
            break;
          } catch (GarbageLockException e) {
            // ignorable - thrown when operating on a garbage collected lock
            // gc thread should clear this object soon - spin and re-get...
            logger.info("Hitting garbage lock state during award on " + lock);
          }
        }
      } else {
        ClientLock lockState = getClientLockState(lock);
        if (lockState == null) {
          remoteManager.unlock(lock, thread, level);
        } else {
          try {
            lockState.award(remoteManager, thread, level);
          } catch (GarbageLockException e) {
            remoteManager.unlock(lock, thread, level);
          }
        }
      }
    } finally {
      stateGuard.readLock().unlock();
    }
  }

  public void notified(LockID lock, ThreadID thread) {
    stateGuard.readLock().lock();
    try {
      if (paused()) {
        logger.warn("Ignoring notified call from dead server : " + lock + ", " + thread);
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
      stateGuard.readLock().unlock();
    }
  }

  public void recall(final LockID lock, final ServerLockLevel level, final int lease) {
    stateGuard.readLock().lock();
    try {
      if (paused()) {
        logger.warn("Ignoring recall request from dead server : " + lock + ", interestedLevel : " + level);
        return;
      }

      ClientLock lockState = getClientLockState(lock);
      if (lockState != null) {
        if (lockState.recall(remoteManager, level, lease)) {
          // schedule the greedy lease
          lockLeaseTimer.schedule(new TimerTask() {
            @Override
            public void run() {
              ClientLockManagerImpl.this.recall(lock, level, -1);
            }
          }, lease);
        }
      }
    } finally {
      stateGuard.readLock().unlock();
    }
  }

  public void refuse(NodeID node, SessionID session, LockID lock, ThreadID thread, ServerLockLevel level) {
    stateGuard.readLock().lock();
    try {
      if (paused() || !sessionManager.isCurrentSession(node, session)) {
        logger.warn("Ignoring lock refuse from a dead server :" + session + ", " + sessionManager + " : " + lock + " "
                    + thread + " " + level + " state = " + state);
        return;
      }
      fireRefused(lock);

      ClientLock lockState = getClientLockState(lock);
      if (lockState != null) {
        lockState.refuse(thread, level);
        return;
      }
    } finally {
      stateGuard.readLock().unlock();
    }
  }

  public void info(LockID lock, ThreadID requestor, Collection<ClientServerExchangeLockContext> contexts) {
    stateGuard.readLock().lock();
    try {
      Object old = inFlightLockQueries.put(requestor, contexts);
      synchronized (old) {
        old.notifyAll();
      }
    } finally {
      stateGuard.readLock().unlock();
    }
  }

  /***********************************/
  /* END ClientLockManager METHODS */
  /***********************************/

  /***********************************/
  /* BEGIN Stupid Wait Test METHODS */
  /***********************************/

  public void wait(LockID lock, WaitListener listener, Object waitObject) throws InterruptedException {
    waitUntilRunning();
    ClientLock lockState = getOrCreateClientLockState(lock);
    lockState.wait(remoteManager, listener, threadManager.getThreadID(), waitObject);
  }

  public void wait(LockID lock, WaitListener listener, Object waitObject, long timeout) throws InterruptedException {
    waitUntilRunning();
    ClientLock lockState = getOrCreateClientLockState(lock);
    lockState.wait(remoteManager, listener, threadManager.getThreadID(), waitObject, timeout);
  }

  /***********************************/
  /* END Stupid Wait Test METHODS */
  /***********************************/

  /***********************************/
  /* BEGIN ClientHandshake METHODS */
  /***********************************/

  public void initializeHandshake(NodeID thisNode, NodeID remoteNode, ClientHandshakeMessage handshakeMessage) {
    stateGuard.writeLock().lock();
    try {
      state = state.initialize();
      if (state == State.STARTING) {
        for (ClientLock cls : locks.values()) {
          cls.initializeHandshake((ClientID) thisNode, handshakeMessage);
        }
      }
    } finally {
      stateGuard.writeLock().unlock();
    }
  }

  public void pause(NodeID remoteNode, int disconnected) {
    stateGuard.writeLock().lock();
    try {
      state = state.pause();
    } finally {
      stateGuard.writeLock().unlock();
    }
  }

  public void shutdown() {
    stateGuard.writeLock().lock();
    try {
      state = state.shutdown();
    } finally {
      stateGuard.writeLock().unlock();
    }
  }

  public void unpause(NodeID remoteNode, int disconnected) {
    stateGuard.writeLock().lock();
    try {
      state = state.unpause();
      if (state == State.RUNNING) {
        runningCondition.signalAll();
      }
    } finally {
      stateGuard.writeLock().unlock();
    }
    resubmitInFlightLockQueries();
  }

  /***********************************/
  /* END ClientHandshake METHODS */
  /***********************************/

  private void waitUntilRunning() {
    stateGuard.readLock().lock();
    try {
      if (state == State.RUNNING) return;
    } finally {
      stateGuard.readLock().unlock();
    }

    boolean interrupted = false;
    stateGuard.writeLock().lock();
    try {
      while (state != State.RUNNING) {
        try {
          runningCondition.await();
        } catch (InterruptedException e) {
          interrupted = true;
        }
      }
    } finally {
      stateGuard.writeLock().unlock();
    }

    Util.selfInterruptIfNeeded(interrupted);
  }

  private boolean paused() {
    /*
     * I would like to wrap this read in a stateGuard read lock but due to the current RRWL instrumentation forcing RRWL
     * instances to use a fair policy and the associated "bug" in fair RRWL in JDK 1.5 I have to prevent reentrant
     * acquires of the read lock. (CDV-1434) <p> Its okay though since this is private and all callers have already read
     * locked.
     */
    return state == State.PAUSED;
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

  public PrettyPrinter prettyPrint(PrettyPrinter out) {
    out.print("ClientLockManagerImpl [" + locks.size() + " locks]").flush();
    for (ClientLock lock : locks.values()) {
      out.indent().print(lock).flush();
    }
    return out;
  }

  public Collection<ClientServerExchangeLockContext> getAllLockContexts() {
    Collection<ClientServerExchangeLockContext> contexts = new ArrayList<ClientServerExchangeLockContext>();
    for (ClientLock lock : locks.values()) {
      contexts.addAll(lock.getStateSnapshot(remoteManager.getClientID()));
    }
    return contexts;
  }

  private Collection<ClientServerExchangeLockContext> queryLock(LockID lock) {
    ThreadID current = threadManager.getThreadID();

    inFlightLockQueries.put(current, lock);
    remoteManager.query(lock, threadManager.getThreadID());

    while (true) {
      synchronized (lock) {
        Object data = inFlightLockQueries.get(current);
        if (data instanceof Collection) {
          return (Collection<ClientServerExchangeLockContext>) data;
        } else {
          try {
            lock.wait();
          } catch (InterruptedException e) {
            //
          }
        }
      }
    }
  }

  private void resubmitInFlightLockQueries() {
    for (Entry<ThreadID, Object> query : inFlightLockQueries.entrySet()) {
      if (query.getValue() instanceof LockID) {
        remoteManager.query((LockID) query.getValue(), query.getKey());
      }
    }
  }

  class LockGcTimerTask extends TimerTask {
    @Override
    public void run() {
      int gcCount = 0;
      for (Entry<LockID, ClientLock> entry : locks.entrySet()) {
        stateGuard.readLock().lock();
        try {
          if (state != State.RUNNING) { return; }

          LockID lock = entry.getKey();
          ClientLock lockState = entry.getValue();
          if (lockState == null) continue;

          if (lockState.tryMarkAsGarbage(remoteManager) && locks.remove(lock, lockState)) {
            gcCount++;
          }
        } finally {
          stateGuard.readLock().unlock();
        }
      }
      if (gcCount > 0) {
        logger.info("Lock GC collected " + gcCount + " garbage locks");
      }
    }
  }

  public int runLockGc() {
    new LockGcTimerTask().run();
    return locks.size();
  }

  @Deprecated
  private void fireLockAttempted(LockID lock) {
    if (statManager.isEnabled()) {
      ClientLock lockState = getClientLockState(lock);
      int pendingCount = lockState == null ? 0 : lockState.pendingCount();
      statManager.recordLockRequested(lock, threadManager.getThreadID(), "", pendingCount);
    }
  }

  @Deprecated
  private void fireLockSucceeded(LockID lock) {
    if (statManager.isEnabled()) {
      statManager.recordLockAwarded(lock, threadManager.getThreadID());
    }
  }

  @Deprecated
  private void fireUnlock(LockID lock) {
    if (statManager.isEnabled()) {
      statManager.recordLockReleased(lock, threadManager.getThreadID());
    }
  }

  @Deprecated
  private void fireRefused(LockID lock) {
    if (statManager.isEnabled()) {
      statManager.recordLockRejected(lock, threadManager.getThreadID());
    }
  }
}
