/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.locks;

import com.tc.async.api.Sink;
import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.management.L2LockStatsManager;
import com.tc.net.ClientID;
import com.tc.object.locks.ClientServerExchangeLockContext;
import com.tc.object.locks.LockID;
import com.tc.object.locks.ServerLockLevel;
import com.tc.object.locks.ThreadID;
import com.tc.object.net.DSOChannelManager;
import com.tc.objectserver.locks.LockStore.LockIterator;
import com.tc.objectserver.locks.ServerLock.NotifyAction;
import com.tc.objectserver.locks.factory.ServerLockFactoryImpl;
import com.tc.objectserver.locks.timer.LockTimer.LockTimerContext;
import com.tc.objectserver.locks.timer.TimerCallback;
import com.tc.text.PrettyPrintable;
import com.tc.text.PrettyPrinter;
import com.tc.util.Assert;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * LockManager is responsible for holding locks (in a LockStore) and delegating requests from the handler to the
 * concerned lock. Each lock is checked out, worked upon and then finally checked in.
 */
public class LockManagerImpl implements LockManager, PrettyPrintable, LockManagerMBean, L2LockStatisticsChangeListener,
    TimerCallback {
  private enum RequestType {
    LOCK, TRY_LOCK, WAIT, UNLOCK
  }

  private final LockStore                               lockStore;
  private final DSOChannelManager                       channelManager;
  private final LockHelper                              lockHelper;
  private final ReentrantReadWriteLock                  statusLock       = new ReentrantReadWriteLock();
  private boolean                                       isStarted        = false;
  private final LinkedBlockingQueue<RequestLockContext> lockRequestQueue = new LinkedBlockingQueue<RequestLockContext>();

  private static final TCLogger                         logger           = TCLogging.getLogger(LockManagerImpl.class);

  public LockManagerImpl(Sink lockSink, DSOChannelManager channelManager) {
    this(lockSink, channelManager, new ServerLockFactoryImpl());
  }

  public LockManagerImpl(Sink lockSink, DSOChannelManager channelManager, LockFactory factory) {
    this.lockStore = new LockStore(factory);
    this.channelManager = channelManager;
    this.lockHelper = new LockHelper(L2LockStatsManager.UNSYNCHRONIZED_LOCK_STATS_MANAGER, lockSink, lockStore, this);
  }

  public void lock(LockID lid, ClientID cid, ThreadID tid, ServerLockLevel level) {
    if (!queueIfNecessary(lid, cid, tid, level, RequestType.LOCK)) { return; }

    ServerLock lock = lockStore.checkOut(lid);
    try {
      if (!isClientAlive(cid)) { return; }
      lock.lock(cid, tid, level, lockHelper);
    } finally {
      lockStore.checkIn(lock);
    }
  }

  public void tryLock(LockID lid, ClientID cid, ThreadID tid, ServerLockLevel level, long timeout) {
    if (!queueIfNecessary(lid, cid, tid, level, RequestType.TRY_LOCK, timeout)) { return; }

    ServerLock lock = lockStore.checkOut(lid);
    try {
      if (!isClientAlive(cid)) { return; }
      lock.tryLock(cid, tid, level, timeout, lockHelper);
    } finally {
      lockStore.checkIn(lock);
    }
  }

  public void unlock(LockID lid, ClientID cid, ThreadID tid) {
    if (!queueIfNecessary(lid, cid, tid, ServerLockLevel.WRITE, RequestType.UNLOCK)) { return; }

    // Lock might be removed from the lock store in the call to the unlock
    ServerLock lock = lockStore.checkOut(lid);
    try {
      if (!isClientAlive(cid)) { return; }
      lock.unlock(cid, tid, lockHelper);
    } finally {
      lockStore.checkIn(lock);
    }
  }

  public void queryLock(LockID lid, ClientID cid, ThreadID tid) {
    if (!isValidStateFor(lid, cid, tid, "QueryLock")) { return; }

    ServerLock lock = lockStore.checkOut(lid);
    try {
      if (!isClientAlive(cid)) { return; }
      lock.queryLock(cid, tid, lockHelper);
    } finally {
      lockStore.checkIn(lock);
    }
  }

  public void interrupt(LockID lid, ClientID cid, ThreadID tid) {
    if (!isValidStateFor(lid, cid, tid, "Interrupt")) { return; }

    ServerLock lock = lockStore.checkOut(lid);
    try {
      if (!isClientAlive(cid)) { return; }
      lock.interrupt(cid, tid, lockHelper);
    } finally {
      lockStore.checkIn(lock);
    }
  }

  /*
   * Ignoring messages from client while in starting state. Such a case might come up when a recall timer goes out and
   * the lock is recalled by the client without it noticing that it might still be in paused state.
   */
  public void recallCommit(LockID lid, ClientID cid, Collection<ClientServerExchangeLockContext> serverLockContexts) {
    if (!isStarted()) {
      logger.info("Ignoring recall commit messages from Client " + cid + " for Lock " + lid);
      return;
    }

    ServerLock lock = lockStore.checkOut(lid);
    try {
      if (!isClientAlive(cid)) { return; }
      lock.recallCommit(cid, serverLockContexts, lockHelper);
    } finally {
      lockStore.checkIn(lock);
    }
  }

  public NotifiedWaiters notify(LockID lid, ClientID cid, ThreadID tid, NotifyAction action,
                                NotifiedWaiters addNotifiedWaitersTo) {
    if (!isValidStateFor(lid, cid, tid, "Notify")) { return addNotifiedWaitersTo; }

    ServerLock lock = lockStore.checkOut(lid);
    try {
      if (!isClientAlive(cid)) { return addNotifiedWaitersTo; }
      return lock.notify(cid, tid, action, addNotifiedWaitersTo, lockHelper);
    } finally {
      lockStore.checkIn(lock);
    }
  }

  public void wait(LockID lid, ClientID cid, ThreadID tid, long timeout) {
    // Why queue wait request?
    // consider steps:
    // 1) ClientLock trying to do remote wait
    // 2) Then that thread doesn't somehow doesn't get CPU cycles and before getting synchronized for wait
    // 3) Server crashes and transport is established again
    // 4) Client handshake gets called and it sends holder context to the server
    // 5) Then remote wait gets called and wait request gets queued in L2 Lock Manager
    // Hence the reason.
    if (!queueIfNecessary(lid, cid, tid, ServerLockLevel.WRITE, RequestType.WAIT, timeout)) { return; }

    ServerLock lock = lockStore.checkOut(lid);
    try {
      if (!isClientAlive(cid)) { return; }
      lock.wait(cid, tid, timeout, lockHelper);
    } finally {
      lockStore.checkIn(lock);
    }
  }

  public void reestablishState(ClientID cid, Collection<ClientServerExchangeLockContext> serverLockContexts) {
    assertStateIsStarting("Reestablish was called after the LockManager was started.");

    for (ClientServerExchangeLockContext cselc : serverLockContexts) {
      LockID lid = cselc.getLockID();

      switch (cselc.getState().getType()) {
        case GREEDY_HOLDER:
        case HOLDER:
        case WAITER:
          ServerLock lock = lockStore.checkOut(lid);
          try {
            lock.reestablishState(cselc, lockHelper);
          } finally {
            lockStore.checkIn(lock);
          }
          break;
        case PENDING:
          lock(lid, (ClientID) cselc.getNodeID(), cselc.getThreadID(), cselc.getState().getLockLevel());
          break;
        case TRY_PENDING:
          tryLock(lid, (ClientID) cselc.getNodeID(), cselc.getThreadID(), cselc.getState().getLockLevel(),
                  cselc.timeout());
          break;
      }
    }
  }

  public void clearAllLocksFor(ClientID cid) {
    LockIterator iter = lockStore.iterator();
    ServerLock lock = iter.getNextLock(null);
    while (lock != null) {
      if (lock.clearStateForNode(cid, lockHelper)) {
        iter.remove();
      }
      lock = iter.getNextLock(lock);
    }
    this.lockHelper.getLockStatsManager().clearAllStatsFor(cid);
  }

  public void enableLockStatsForNodeIfNeeded(ClientID cid) {
    this.lockHelper.getLockStatsManager().enableStatsForNodeIfNeeded(cid);
  }

  public LockMBean[] getAllLocks() {
    List<LockMBean> beansList = new ArrayList<LockMBean>();

    LockIterator iter = lockStore.iterator();
    ServerLock lock = iter.getNextLock(null);
    while (lock != null) {
      beansList.add(lock.getMBean(channelManager));
      lock = iter.getNextLock(lock);
    }

    return beansList.toArray(new LockMBean[beansList.size()]);
  }

  public void start() {
    statusLock.writeLock().lock();
    try {
      Assert.assertTrue(!isStarted);
      isStarted = true;

      // Done to make sure that all wait/try timers are started
      lockHelper.getLockTimer().start();

      processPendingRequests();
    } finally {
      statusLock.writeLock().unlock();
    }
  }

  private void processPendingRequests() {
    RequestLockContext ctxt = null;
    while ((ctxt = lockRequestQueue.poll()) != null) {
      switch (ctxt.getType()) {
        case LOCK:
          lock(ctxt.getLockID(), ctxt.getClientID(), ctxt.getThreadID(), ctxt.getRequestedLockLevel());
          break;
        case TRY_LOCK:
          tryLock(ctxt.getLockID(), ctxt.getClientID(), ctxt.getThreadID(), ctxt.getRequestedLockLevel(),
                  ctxt.getTimeout());
          break;
        case WAIT:
          wait(ctxt.getLockID(), ctxt.getClientID(), ctxt.getThreadID(), ctxt.getTimeout());
          break;
        case UNLOCK:
          unlock(ctxt.getLockID(), ctxt.getClientID(), ctxt.getThreadID());
          break;
      }
    }
  }

  public void timerTimeout(LockTimerContext lockTimerContext) {
    LockID lid = lockTimerContext.getLockID();
    ServerLock lock = lockStore.checkOut(lid);
    try {
      // call timeout for the lock
      lock.timerTimeout(lockTimerContext);
    } finally {
      lockStore.checkIn(lock);
    }
  }

  public void setLockStatisticsEnabled(boolean lockStatsEnabled, L2LockStatsManager manager) {
    if (lockStatsEnabled) {
      lockHelper.setLockStatsManager(manager);
    } else {
      lockHelper.setLockStatsManager(L2LockStatsManager.UNSYNCHRONIZED_LOCK_STATS_MANAGER);
    }
  }

  private void queueRequest(LockID lid, ClientID cid, ThreadID tid, ServerLockLevel level, RequestType type,
                            long timeout) {
    RequestLockContext context = new RequestLockContext(lid, cid, tid, level, type, timeout);
    try {
      lockRequestQueue.put(context);
    } catch (InterruptedException e) {
      throw new AssertionError(e);
    }
  }

  private boolean queueIfNecessary(LockID lid, ClientID cid, ThreadID tid, ServerLockLevel level, RequestType type) {
    return queueIfNecessary(lid, cid, tid, level, type, -1);
  }

  private boolean queueIfNecessary(LockID lid, ClientID cid, ThreadID tid, ServerLockLevel level, RequestType type,
                                   long timeout) {
    statusLock.readLock().lock();
    try {
      if (!isStarted) {
        queueRequest(lid, cid, tid, level, type, timeout);
        return false;
      }
      return true;
    } finally {
      statusLock.readLock().unlock();
    }
  }

  private boolean isClientAlive(ClientID cid) {
    if (!this.channelManager.isActiveID(cid)) {
      logger.warn("Lock Manager ignoring message received from dead client");
      return false;
    }
    return true;
  }

  private boolean isValidStateFor(LockID lid, ClientID cid, ThreadID tid, String callType) {
    statusLock.readLock().lock();
    try {
      if (!isStarted) { throw new AssertionError(callType + " message received when lock manager was starting"
                                                 + " Message Context: [LockID=" + lid + ", NodeID=" + cid
                                                 + ", ThreadID=" + tid + "]"); }
      return true;
    } finally {
      statusLock.readLock().unlock();
    }
  }

  private boolean isStarted() {
    statusLock.readLock().lock();
    try {
      return isStarted;
    } finally {
      statusLock.readLock().unlock();
    }
  }

  public PrettyPrinter prettyPrint(PrettyPrinter out) {
    out.print(this.getClass().getName()).flush();
    int size = 0;
    LockIterator iter = lockStore.iterator();
    ServerLock lock = iter.getNextLock(null);
    while (lock != null) {
      out.visit(lock);
      size++;
      lock = iter.getNextLock(lock);
    }
    out.indent().print("locks: " + size).println().flush();
    return out;
  }

  private void assertStateIsStarting(String errMessage) {
    statusLock.readLock().lock();
    try {
      Assert.assertTrue(errMessage, !isStarted);
    } finally {
      statusLock.readLock().unlock();
    }
  }

  private static class RequestLockContext {
    private final LockID          lockID;
    private final ClientID        nodeID;
    private final ThreadID        threadID;
    private final ServerLockLevel requestedLockLevel;
    private final RequestType     type;
    private final long            timeout;

    public RequestLockContext(LockID lockID, ClientID nodeID, ThreadID threadID, ServerLockLevel requestedLockLevel,
                              RequestType type, long timeout) {
      this.lockID = lockID;
      this.nodeID = nodeID;
      this.threadID = threadID;
      this.requestedLockLevel = requestedLockLevel;
      this.type = type;
      this.timeout = timeout;
    }

    public LockID getLockID() {
      return lockID;
    }

    public ClientID getClientID() {
      return nodeID;
    }

    public ThreadID getThreadID() {
      return threadID;
    }

    public ServerLockLevel getRequestedLockLevel() {
      return requestedLockLevel;
    }

    public RequestType getType() {
      return type;
    }

    public long getTimeout() {
      return timeout;
    }

    @Override
    public String toString() {
      return "RequestLockContext [ " + this.lockID + "," + this.nodeID + "," + this.threadID + ","
             + this.requestedLockLevel + ", " + this.type + ", " + this.timeout + " ]";
    }
  }

  /**
   * To be used only in tests
   */
  public int getLockCount() {
    int size = 0;
    ServerLock oldLock = null;
    LockIterator iter = lockStore.iterator();
    ServerLock lock = iter.getNextLock(oldLock);
    while (lock != null) {
      oldLock = lock;
      size++;
      lock = iter.getNextLock(oldLock);
    }
    return size;
  }

  /**
   * To be used only in tests
   */
  public boolean hasPending(LockID lid) {
    AbstractServerLock lock = (AbstractServerLock) lockStore.checkOut(lid);
    boolean result = false;
    try {
      result = lock.hasPendingRequests();
    } finally {
      lockStore.checkIn(lock);
    }
    return result;
  }

  /**
   * To be used only in tests
   */
  public LockHelper getHelper() {
    return lockHelper;
  }

}
