/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.lockmanager.impl;

import org.apache.commons.collections.map.ListOrderedMap;

import com.tc.async.api.Sink;
import com.tc.exception.TCInternalError;
import com.tc.exception.TCLockUpgradeNotSupportedError;
import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.management.L2LockStatsManager;
import com.tc.net.groups.NodeID;
import com.tc.object.lockmanager.api.LockContext;
import com.tc.object.lockmanager.api.LockID;
import com.tc.object.lockmanager.api.LockLevel;
import com.tc.object.lockmanager.api.ServerThreadID;
import com.tc.object.lockmanager.api.ThreadID;
import com.tc.object.lockmanager.api.TCLockTimer;
import com.tc.object.lockmanager.api.TimerCallback;
import com.tc.object.lockmanager.impl.LockHolder;
import com.tc.object.net.DSOChannelManager;
import com.tc.object.tx.TimerSpec;
import com.tc.objectserver.context.LockResponseContext;
import com.tc.objectserver.lockmanager.api.LockEventListener;
import com.tc.objectserver.lockmanager.api.LockMBean;
import com.tc.objectserver.lockmanager.api.LockWaitContext;
import com.tc.objectserver.lockmanager.api.NotifiedWaiters;
import com.tc.objectserver.lockmanager.api.ServerLockRequest;
import com.tc.objectserver.lockmanager.api.TCIllegalMonitorStateException;
import com.tc.objectserver.lockmanager.api.Waiter;
import com.tc.properties.TCPropertiesConsts;
import com.tc.properties.TCPropertiesImpl;
import com.tc.util.Assert;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TimerTask;

public class Lock {
  private static final TCLogger            logger              = TCLogging.getLogger(Lock.class);
  private final static boolean             LOCK_LEASE_ENABLE   = TCPropertiesImpl.getProperties()
                                                                   .getBoolean(TCPropertiesConsts.L2_LOCKMANAGER_GREEDY_LEASE_ENABLED);
  private final static int                 LOCK_LEASE_TIME     = TCPropertiesImpl.getProperties()
                                                                   .getInt(TCPropertiesConsts.L2_LOCKMANAGER_GREEDY_LEASE_LEASETIME_INMILLS);
  public final static Lock                 NULL_LOCK           = new Lock(LockID.NULL_ID, 0,
                                                                          new LockEventListener[] {}, true,
                                                                          LockManagerImpl.ALTRUISTIC_LOCK_POLICY,
                                                                          ServerThreadContextFactory.DEFAULT_FACTORY,
                                                                          L2LockStatsManager.NULL_LOCK_STATS_MANAGER);

  private final LockEventListener[]        listeners;
  private final Map                        greedyHolders       = new HashMap();
  private final Map                        holders             = new HashMap();
  private final Map                        tryLockTimers       = new HashMap();
  private final ListOrderedMap             pendingLockRequests = new ListOrderedMap();

  private final ListOrderedMap             waiters             = new ListOrderedMap();
  private final Map                        waitTimers          = new HashMap();
  private final LockID                     lockID;
  private final long                       timeout;
  private final boolean                    isNull;
  private int                              level;
  private boolean                          recalled            = false;

  private int                              lockPolicy;
  private final ServerThreadContextFactory threadContextFactory;
  private final L2LockStatsManager         lockStatsManager;
  private String                           lockType;

  // real constructor used by lock manager
  Lock(LockID lockID, ServerThreadContext txn, int lockLevel, String lockType, Sink lockResponseSink, long timeout,
       LockEventListener[] listeners, int lockPolicy, ServerThreadContextFactory threadContextFactory,
       L2LockStatsManager lockStatsManager) {
    this(lockID, timeout, listeners, false, lockPolicy, threadContextFactory, lockStatsManager);
    this.lockType = lockType;
    requestLock(txn, lockLevel, lockResponseSink);
  }

  // real constructor used by lock manager when re-establishing waits and lock holds on
  // restart.
  Lock(LockID lockID, ServerThreadContext txn, long timeout, LockEventListener[] listeners, int lockPolicy,
       ServerThreadContextFactory threadContextFactory, L2LockStatsManager lockStatsManager) {
    this(lockID, timeout, listeners, false, lockPolicy, threadContextFactory, lockStatsManager);
  }

  // for tests
  Lock(LockID lockID, long timeout, LockEventListener[] listeners) {
    this(lockID, timeout, listeners, false, LockManagerImpl.ALTRUISTIC_LOCK_POLICY,
         ServerThreadContextFactory.DEFAULT_FACTORY, L2LockStatsManager.NULL_LOCK_STATS_MANAGER);
  }

  private Lock(LockID lockID, long timeout, LockEventListener[] listeners, boolean isNull, int lockPolicy,
               ServerThreadContextFactory threadContextFactory, L2LockStatsManager lockStatsManager) {
    this.lockID = lockID;
    this.listeners = listeners;
    this.timeout = timeout;
    this.isNull = isNull;
    this.lockPolicy = lockPolicy;
    this.threadContextFactory = threadContextFactory;
    this.lockStatsManager = lockStatsManager;
  }

  static LockResponseContext createLockRejectedResponseContext(LockID lockID, ServerThreadID threadID, int level) {
    return new LockResponseContext(lockID, threadID.getNodeID(), threadID.getClientThreadID(), level,
                                   LockResponseContext.LOCK_NOT_AWARDED);
  }

  static LockResponseContext createLockAwardResponseContext(LockID lockID, ServerThreadID threadID, int level) {
    LockResponseContext lrc = new LockResponseContext(lockID, threadID.getNodeID(), threadID.getClientThreadID(),
                                                      level, LockResponseContext.LOCK_AWARD);
    return lrc;
  }

  static LockResponseContext createLockRecallResponseContext(LockID lockID, ServerThreadID threadID, int level) {
    if (LOCK_LEASE_ENABLE) {
      return new LockResponseContext(lockID, threadID.getNodeID(), threadID.getClientThreadID(), level,
                                     LockResponseContext.LOCK_RECALL, LOCK_LEASE_TIME);
    } else {
      return new LockResponseContext(lockID, threadID.getNodeID(), threadID.getClientThreadID(), level,
                                     LockResponseContext.LOCK_RECALL);
    }
  }

  static LockResponseContext createLockWaitTimeoutResponseContext(LockID lockID, ServerThreadID threadID, int level) {
    return new LockResponseContext(lockID, threadID.getNodeID(), threadID.getClientThreadID(), level,
                                   LockResponseContext.LOCK_WAIT_TIMEOUT);
  }

  static LockResponseContext createLockQueriedResponseContext(LockID lockID, ServerThreadID threadID, int level,
                                                              int lockRequestQueueLength, Collection greedyHolders,
                                                              Collection holders, Collection waiters) {
    return new LockResponseContext(lockID, threadID.getNodeID(), threadID.getClientThreadID(), level,
                                   lockRequestQueueLength, greedyHolders, holders, waiters,
                                   LockResponseContext.LOCK_INFO);
  }

  private static Request createRequest(ServerThreadContext txn, int lockLevel, Sink lockResponseSink,
                                       TimerSpec timeout, boolean isBlock) {
    Request request = null;
    if (isBlock) {
      request = new TryLockRequest(txn, lockLevel, lockResponseSink, timeout);
    } else {
      request = new Request(txn, lockLevel, lockResponseSink);
    }
    return request;
  }

  synchronized LockMBean getMBean(DSOChannelManager channelManager) {
    int count;
    LockHolder[] holds = new LockHolder[this.holders.size()];
    ServerLockRequest[] reqs = new ServerLockRequest[this.pendingLockRequests.size()];
    Waiter[] waits = new Waiter[this.waiters.size()];

    count = 0;
    for (Iterator i = this.holders.values().iterator(); i.hasNext();) {
      Holder h = (Holder) i.next();
      NodeID cid = h.getNodeID();
      holds[count] = new LockHolder(h.getLockID(), cid, channelManager.getChannelAddress(cid), h.getThreadID(), h
          .getLockLevel(), h.getTimestamp());
      holds[count++].lockAcquired(h.getTimestamp());
    }

    count = 0;
    for (Iterator i = this.pendingLockRequests.values().iterator(); i.hasNext();) {
      Request r = (Request) i.next();
      NodeID cid = r.getRequesterID();
      reqs[count++] = new ServerLockRequest(cid, channelManager.getChannelAddress(cid), r.getSourceID(), r
          .getLockLevel(), r.getTimestamp());
    }

    count = 0;
    for (Iterator i = this.waiters.values().iterator(); i.hasNext();) {
      LockWaitContext wc = (LockWaitContext) i.next();
      NodeID cid = wc.getNodeID();
      waits[count++] = new Waiter(cid, channelManager.getChannelAddress(cid), wc.getThreadID(), wc.getTimerSpec(),
                                  wc.getTimestamp());
    }

    return new LockMBeanImpl(lockID, holds, reqs, waits);
  }

  synchronized void queryLock(ServerThreadContext txn, Sink lockResponseSink) {

    // TODO:
    // The Remote Lock Manager needs to ask the client for lock information when greedy lock is awarded.
    // Currently, the Remote Lock Manager responds to queryLock by looking at the server only.
    lockResponseSink.add(createLockQueriedResponseContext(this.lockID, txn.getId(), this.level,
                                                          this.pendingLockRequests.size(), this.greedyHolders.values(),
                                                          this.holders.values(), this.waiters.values()));
  }

  boolean tryRequestLock(ServerThreadContext txn, int requestedLockLevel, TimerSpec lockRequestTimeout, TCLockTimer waitTimer,
                         TimerCallback callback, Sink lockResponseSink) {
    return requestLock(txn, requestedLockLevel, lockResponseSink, true, lockRequestTimeout, waitTimer, callback);
  }

  boolean requestLock(ServerThreadContext txn, int requestedLockLevel, Sink lockResponseSink) {
    return requestLock(txn, requestedLockLevel, lockResponseSink, false, null, null, null);
  }

  // XXX:: UPGRADE Requests can come in with requestLockLevel == UPGRADE on a notified wait during server crash
  synchronized boolean requestLock(ServerThreadContext txn, int requestedLockLevel, Sink lockResponseSink,
                                   boolean noBlock, TimerSpec lockRequestTimeout, TCLockTimer waitTimer,
                                   TimerCallback callback) {

    if (holdsReadLock(txn) && LockLevel.isWrite(requestedLockLevel)) {
      // lock upgrade is not supported; it should have been rejected by the client.
      throw new TCLockUpgradeNotSupportedError(
                                               "Lock upgrade is not supported. The request should have been rejected by the client. Your client may be using an older version of tc.jar");
    }

    if (waiters.containsKey(txn)) throw new AssertionError("Attempt to request a lock in a Thread "
                                                           + "that is already part of the wait set. lock = " + this);

    recordLockRequestStat(txn.getId().getNodeID(), txn.getId().getClientThreadID());
    // debug("requestLock - BEGIN -", txn, ",", LockLevel.toString(requestedLockLevel));
    // it is an error (probably originating from the client side) to
    // request a lock you already hold
    Holder holder = getHolder(txn);
    if (noBlock && !lockRequestTimeout.needsToWait() && holder == null && (requestedLockLevel != LockLevel.READ || !this.isRead())
        && (getHoldersCount() > 0 || hasGreedyHolders())) {
      cannotAwardAndRespond(txn, requestedLockLevel, lockResponseSink);
      return false;
    }

    if (holder != null) {
      if (LockLevel.NIL_LOCK_LEVEL != (holder.getLockLevel() & requestedLockLevel)) {
        // formatting
        throw new AssertionError("Client requesting already held lock! holder=" + holder + ", lock=" + this);
      }
    }

    if (isPolicyGreedy()) {
      if (canAwardGreedilyOnTheClient(txn, requestedLockLevel)) {
        // These requests are the ones in the wire when the greedy lock was given out to the client.
        // We can safely ignore it as the clients will be able to award it locally.
        logger.debug(lockID + " : Lock.requestLock() : Ignoring the Lock request(" + txn + ","
                     + LockLevel.toString(requestedLockLevel)
                     + ") message from the a client that has the lock greedily.");
        return false;
      } else if (recalled) {
        // add to pending until recall process is complete, those who hold the lock greedily will send the
        // pending state during recall commit.
        if (!holdsGreedyLock(txn)) {
          queueRequest(txn, requestedLockLevel, lockResponseSink, noBlock, lockRequestTimeout, waitTimer, callback);
        }
        return false;
      }
    }

    // Lock granting logic:
    // 0. If no one is holding this lock, go ahead and award it
    // 1. If only a read lock is held and no write locks are pending, and another read
    // (and only read) lock is requested, award it. If Write locks are pending, we dont want to
    // starve the WRITES by keeping on awarding READ Locks.
    // 2. Else the request must be queued (ie. added to pending list)

    if ((getHoldersCount() == 0) || ((!hasPending()) && ((requestedLockLevel == LockLevel.READ) && this.isRead()))) {
      // (0, 1) uncontended or additional read lock
      if (isPolicyGreedy() && ((requestedLockLevel == LockLevel.READ) || (getWaiterCount() == 0))) {
        awardGreedyAndRespond(txn, requestedLockLevel, lockResponseSink);
      } else {
        awardAndRespond(txn, txn.getId().getClientThreadID(), requestedLockLevel, lockResponseSink);
      }
    } else {
      // (2) queue request
      if (isPolicyGreedy() && hasGreedyHolders()) {
        recall(requestedLockLevel);
      }
      if (!holdsGreedyLock(txn)) {
        queueRequest(txn, requestedLockLevel, lockResponseSink, noBlock, lockRequestTimeout, waitTimer, callback);
      }
      return false;
    }

    return true;
  }

  private void queueRequest(ServerThreadContext txn, int requestedLockLevel, Sink lockResponseSink, boolean noBlock,
                            TimerSpec lockRequesttimeout, TCLockTimer waitTimer, TimerCallback callback) {
    if (noBlock) {
      // By the time it reaches here, timeout.needsToWait() must be true
      Assert.assertTrue(lockRequesttimeout.needsToWait());
      addPendingTryLockRequest(txn, requestedLockLevel, lockRequesttimeout, lockResponseSink, waitTimer, callback);
    } else {
      addPendingLockRequest(txn, requestedLockLevel, lockResponseSink);
    }
  }

  synchronized void addRecalledHolder(ServerThreadContext txn, int lockLevel) {
    // debug("addRecalledHolder - BEGIN -", txn, ",", LockLevel.toString(lockLevel));
    if (!LockLevel.isWrite(level) && LockLevel.isWrite(lockLevel)) {
      // Client issued a WRITE lock without holding a GREEDY WRITE. Bug in the client.
      throw new AssertionError("Client issued a WRITE lock without holding a GREEDY WRITE !");
    }
    recordLockRequestStat(txn.getId().getNodeID(), txn.getId().getClientThreadID());
    awardLock(txn, txn.getId().getClientThreadID(), lockLevel);
  }

  synchronized void addRecalledPendingRequest(ServerThreadContext txn, int lockLevel, Sink lockResponseSink) {
    // debug("addRecalledPendingRequest - BEGIN -", txn, ",", LockLevel.toString(lockLevel));
    recordLockRequestStat(txn.getId().getNodeID(), txn.getId().getClientThreadID());
    addPendingLockRequest(txn, lockLevel, lockResponseSink);
  }

  synchronized void addRecalledTryLockPendingRequest(ServerThreadContext txn, int lockLevel, TimerSpec lockRequestTimeout,
                                                     Sink lockResponseSink, TCLockTimer waitTimer,
                                                     TimerCallback callback) {
    recordLockRequestStat(txn.getId().getNodeID(), txn.getId().getClientThreadID());

    if (!lockRequestTimeout.needsToWait()) {
      cannotAwardAndRespond(txn, lockLevel, lockResponseSink);
      return;
    }

    addPendingTryLockRequest(txn, lockLevel, lockRequestTimeout, lockResponseSink, waitTimer, callback);
  }

  private void addPendingTryLockRequest(ServerThreadContext txn, int lockLevel, TimerSpec lockRequestTimeout,
                                        Sink lockResponseSink, TCLockTimer waitTimer, TimerCallback callback) {
    Request request = addPending(txn, lockLevel, lockResponseSink, lockRequestTimeout, true);

    TryLockContextImpl tryLockWaitRequestContext = new TryLockContextImpl(txn, this, lockRequestTimeout, lockLevel,
                                                                          lockResponseSink);

    scheduleWaitForTryLock(callback, waitTimer, request, tryLockWaitRequestContext);
  }

  private void addPendingLockRequest(ServerThreadContext threadContext, int lockLevel, Sink awardLockSink) {
    addPending(threadContext, lockLevel, awardLockSink, null, false);
  }

  private Request addPending(ServerThreadContext threadContext, int lockLevel, Sink awardLockSink,
                             TimerSpec lockRequestTimeout, boolean noBlock) {
    Assert.assertFalse(isNull());
    // debug("addPending() - BEGIN -", threadContext, ", ", LockLevel.toString(lockLevel));

    Request request = createRequest(threadContext, lockLevel, awardLockSink, lockRequestTimeout, noBlock);

    if (pendingLockRequests.containsValue(request)) {
      logger.debug("Ignoring existing Request " + request + " in Lock " + lockID);
      return request;
    }

    this.pendingLockRequests.put(threadContext, request);
    for (Iterator currentHolders = holders.values().iterator(); currentHolders.hasNext();) {
      Holder holder = (Holder) currentHolders.next();
      notifyAddPending(holder);
    }
    return request;
  }

  private boolean isGreedyRequest(ServerThreadContext txn) {
    return (txn.getId().getClientThreadID().equals(ThreadID.VM_ID));
  }

  private boolean isPolicyGreedy() {
    return lockPolicy == LockManagerImpl.GREEDY_LOCK_POLICY;
  }

  int getLockPolicy() {
    return lockPolicy;
  }

  int getLockLevel() {
    return level;
  }

  void setLockPolicy(int newPolicy) {
    if (!isNull() && newPolicy != lockPolicy) {
      this.lockPolicy = newPolicy;
      if (!isPolicyGreedy()) {
        recall(LockLevel.WRITE);
      }
    }
  }

  private void awardGreedyAndRespond(ServerThreadContext txn, int requestedLockLevel, Sink lockResponseSink) {
    // debug("awardGreedyAndRespond() - BEGIN - ", txn, ",", LockLevel.toString(requestedLockLevel));
    final ServerThreadContext clientTx = getClientVMContext(txn);
    final int greedyLevel = LockLevel.makeGreedy(requestedLockLevel);

    NodeID ch = txn.getId().getNodeID();
    checkAndClearStateOnGreedyAward(txn.getId().getClientThreadID(), ch, requestedLockLevel);
    Holder holder = awardAndRespond(clientTx, txn.getId().getClientThreadID(), greedyLevel, lockResponseSink);
    holder.setSink(lockResponseSink);
    greedyHolders.put(ch, holder);
  }

  private void cannotAwardAndRespond(ServerThreadContext txn, int requestedLockLevel, Sink lockResponseSink) {
    lockResponseSink.add(createLockRejectedResponseContext(this.lockID, txn.getId(), requestedLockLevel));
    recordLockRejectStat(txn.getId().getNodeID(), txn.getId().getClientThreadID());
  }

  private Holder awardAndRespond(ServerThreadContext txn, ThreadID requestThreadID, int requestedLockLevel,
                                 Sink lockResponseSink) {
    // debug("awardRespond() - BEGIN - ", txn, ",", LockLevel.toString(requestedLockLevel));
    Holder holder = awardLock(txn, requestThreadID, requestedLockLevel);
    lockResponseSink.add(createLockAwardResponseContext(this.lockID, txn.getId(), requestedLockLevel));
    return holder;
  }

  private void recallIfPending(int recallLevel) {
    if (pendingLockRequests.size() > 0) {
      recall(recallLevel);
    }
  }

  private void recall(int recallLevel) {
    if (recalled) { return; }
    recordLockHoppedStat();
    for (Iterator i = greedyHolders.values().iterator(); i.hasNext();) {
      Holder holder = (Holder) i.next();
      holder.getSink().add(
                           createLockRecallResponseContext(holder.getLockID(), holder.getThreadContext()
                               .getId(), recallLevel));
      recalled = true;
    }
  }

  synchronized void notify(ServerThreadContext txn, boolean all, NotifiedWaiters addNotifiedWaitersTo)
      throws TCIllegalMonitorStateException {
    // debug("notify() - BEGIN - ", txn, ", all = " + all);
    if (waiters.containsKey(txn)) { throw Assert.failure("Can't notify self: " + txn); }
    checkLegalWaitNotifyState(txn);

    if (waiters.size() > 0) {
      final int numToNotify = all ? waiters.size() : 1;
      for (int i = 0; i < numToNotify; i++) {
        LockWaitContext wait = (LockWaitContext) waiters.remove(0);
        removeAndCancelWaitTimer(wait);
        recordLockRequestStat(wait.getNodeID(), wait.getThreadID());
        createPendingFromWaiter(wait);
        addNotifiedWaitersTo.addNotification(new LockContext(lockID, wait.getNodeID(), wait.getThreadID(), wait
            .lockLevel(), lockType));
      }
    }
  }

  synchronized void interrupt(ServerThreadContext txn) {
    if (waiters.size() == 0 || !waiters.containsKey(txn)) {
      logger.warn("Cannot interrupt: " + txn + " is not waiting.");
      return;
    }
    LockWaitContext wait = (LockWaitContext) waiters.remove(txn);
    recordLockRequestStat(wait.getNodeID(), wait.getThreadID());
    removeAndCancelWaitTimer(wait);
    createPendingFromWaiter(wait);
  }

  private void removeAndCancelWaitTimer(LockWaitContext wait) {
    TimerTask task = (TimerTask) waitTimers.remove(wait);
    if (task != null) task.cancel();
  }

  private Request createPendingFromWaiter(LockWaitContext wait) {
    // XXX: This cast to WaitContextImpl is lame. I'm not sure how to refactor it right now.
    Request request = createRequest(((LockWaitContextImpl) wait).getThreadContext(), wait.lockLevel(), wait
        .getLockResponseSink(), null, false);
    createPending(wait, request);
    return request;
  }

  private void createPending(LockWaitContext wait, Request request) {
    ServerThreadContext txn = ((LockWaitContextImpl) wait).getThreadContext();
    pendingLockRequests.put(txn, request);

    if (isPolicyGreedy() && hasGreedyHolders()) {
      recall(request.getLockLevel());
    }
  }

  synchronized void tryRequestLockTimeout(LockWaitContext context) {
    TryLockContextImpl tryLockContext = (TryLockContextImpl) context;
    ServerThreadContext txn = tryLockContext.getThreadContext();
    Object removed = tryLockTimers.remove(txn);
    if (removed != null) {
      pendingLockRequests.remove(txn);
      Sink lockResponseSink = context.getLockResponseSink();
      int lockLevel = context.lockLevel();
      cannotAwardAndRespond(txn, lockLevel, lockResponseSink);
    }
  }

  synchronized void waitTimeout(LockWaitContext context) {

    // debug("waitTimeout() - BEGIN -", context);
    // XXX: This cast is gross, too.
    ServerThreadContext txn = ((LockWaitContextImpl) context).getThreadContext();
    Object removed = waiters.remove(txn);

    if (removed != null) {
      waitTimers.remove(context);
      Sink lockResponseSink = context.getLockResponseSink();
      int lockLevel = context.lockLevel();

      // Add a wait Timeout message
      lockResponseSink.add(createLockWaitTimeoutResponseContext(this.lockID, txn.getId(), lockLevel));

      recordLockRequestStat(context.getNodeID(), context.getThreadID());
      if (holders.size() == 0) {
        if (isPolicyGreedy() && (getWaiterCount() == 0)) {
          awardGreedyAndRespond(txn, lockLevel, lockResponseSink);
        } else {
          awardAndRespond(txn, txn.getId().getClientThreadID(), lockLevel, lockResponseSink);
        }
      } else {
        createPendingFromWaiter(context);
      }
    }
  }

  synchronized void wait(ServerThreadContext txn, TCLockTimer waitTimer, TimerSpec call, TimerCallback callback,
                         Sink lockResponseSink) throws TCIllegalMonitorStateException {
    // debug("wait() - BEGIN -", txn, ", ", call);
    if (waiters.containsKey(txn)) throw Assert.failure("Already in wait set: " + txn);
    checkLegalWaitNotifyState(txn);

    Holder current = getHolder(txn);
    Assert.assertNotNull(current);

    LockWaitContext waitContext = new LockWaitContextImpl(txn, this, call, current.getLockLevel(), lockResponseSink);
    waiters.put(txn, waitContext);

    scheduleWait(callback, waitTimer, waitContext);
    removeCurrentHold(txn);

    nextPending();
  }

  // This method reestablished Wait State and schedules wait timeouts too. There are cases where we may need to ignore a
  // wait, if we already know about it. Note that it could be either in waiting or pending state.
  synchronized void addRecalledWaiter(ServerThreadContext txn, TimerSpec call, int lockLevel,
                                      Sink lockResponseSink, TCLockTimer waitTimer, TimerCallback callback) {
    // debug("addRecalledWaiter() - BEGIN -", txn, ", ", call);

    LockWaitContext waitContext = new LockWaitContextImpl(txn, this, call, lockLevel, lockResponseSink);
    if (waiters.containsKey(txn)) {
      logger.debug("addRecalledWaiter(): Ignoring " + waitContext + " as it is already in waiters list.");
      return;
    }
    Request request = createRequest(txn, lockLevel, lockResponseSink, null, false);
    if (pendingLockRequests.containsValue(request)) {
      logger.debug("addRecalledWaiter(): Ignoring " + waitContext + " as it is already in pending list.");
      return;
    }
    waiters.put(txn, waitContext);
    scheduleWait(callback, waitTimer, waitContext);
  }

  // This method reestablished Wait State and does not schedules wait timeouts too. This is
  // called when LockManager is starting and wait timers are started when the lock Manager is started.
  synchronized void reestablishWait(ServerThreadContext txn, TimerSpec call, int lockLevel, Sink lockResponseSink) {
    LockWaitContext waitContext = new LockWaitContextImpl(txn, this, call, lockLevel, lockResponseSink);
    Object old = waiters.put(txn, waitContext);
    if (old != null) throw Assert.failure("Already in wait set: " + txn);
  }

  synchronized void reestablishLock(ServerThreadContext threadContext, int requestedLevel, Sink lockResponseSink) {
    if ((LockLevel.isWrite(requestedLevel) && holders.size() != 0)
        || (LockLevel.isRead(requestedLevel) && LockLevel.isWrite(this.level))) { throw new AssertionError(
                                                                                                           "Lock "
                                                                                                               + this
                                                                                                               + " already held by other Holder. Can't grant to "
                                                                                                               + threadContext
                                                                                                               + LockLevel
                                                                                                                   .toString(requestedLevel));

    }
    if (waiters.get(threadContext) != null) { throw new AssertionError("Thread " + threadContext
                                                                       + "is already in Wait state for Lock " + this
                                                                       + ". Can't grant Lock Hold !"); }
    recordLockRequestStat(threadContext.getId().getNodeID(), threadContext.getId().getClientThreadID());
    if (isGreedyRequest(threadContext)) {
      int greedyLevel = LockLevel.makeGreedy(requestedLevel);
      NodeID nid = threadContext.getId().getNodeID();
      Holder holder = awardLock(threadContext, threadContext.getId().getClientThreadID(), greedyLevel);
      holder.setSink(lockResponseSink);
      greedyHolders.put(nid, holder);
    } else {
      awardLock(threadContext, threadContext.getId().getClientThreadID(), requestedLevel);
    }
  }

  private void scheduleWait(TimerCallback callback, TCLockTimer waitTimer, LockWaitContext waitContext) {
    final TimerTask timer = waitTimer.scheduleTimer(callback, waitContext.getTimerSpec(), waitContext);
    if (timer != null) {
      waitTimers.put(waitContext, timer);
    }
  }

  private TimerTask scheduleWaitForTryLock(TimerCallback callback, TCLockTimer waitTimer, Request pendingRequest,
                                           TryLockContextImpl tryLockWaitRequestContext) {
    final TimerTask timer = waitTimer.scheduleTimer(callback, tryLockWaitRequestContext.getTimerSpec(),
                                                    tryLockWaitRequestContext);
    if (timer != null) {
      tryLockTimers.put(tryLockWaitRequestContext.getThreadContext(), timer);
    }
    return timer;
  }

  private void checkLegalWaitNotifyState(ServerThreadContext threadContext) throws TCIllegalMonitorStateException {
    Assert.assertFalse(isNull());

    final int holdersSize = holders.size();
    if (holdersSize != 1) { throw new TCIllegalMonitorStateException("Invalid holder set size: " + holdersSize); }

    final int currentLevel = this.level;
    if (!LockLevel.isWrite(currentLevel)) { throw new TCIllegalMonitorStateException("Incorrect lock level: "
                                                                                     + LockLevel.toString(currentLevel)); }

    Holder holder = getHolder(threadContext);
    if (holder == null) {
      holder = getHolder(getClientVMContext(threadContext));
    }

    if (holder == null) {
      // make formatter sane
      throw new TCIllegalMonitorStateException(threadContext + " is not the current lock holder for: " + threadContext);
    }
  }

  private ServerThreadContext getClientVMContext(ServerThreadContext threadContext) {
    return threadContextFactory.getOrCreate(threadContext.getId().getNodeID(), ThreadID.VM_ID);
  }

  public synchronized int getHoldersCount() {
    return holders.size();
  }

  public synchronized int getPendingCount() {
    return pendingLockRequests.size();
  }

  Collection getHoldersCollection() {
    return Collections.unmodifiableCollection(this.holders.values());
  }

  public synchronized String toString() {
    try {
      StringBuffer rv = new StringBuffer();

      rv.append(lockID).append(", ").append("Level: ").append(LockLevel.toString(this.level)).append("\r\n");

      rv.append("Holders (").append(holders.size()).append(")\r\n");
      for (Iterator iter = holders.values().iterator(); iter.hasNext();) {
        rv.append('\t').append(iter.next().toString()).append("\r\n");
      }

      rv.append("Wait Set (").append(waiters.size()).append(")\r\n");
      for (Iterator iter = waiters.values().iterator(); iter.hasNext();) {
        rv.append('\t').append(iter.next().toString()).append("\r\n");
      }

      rv.append("Pending lock requests (").append(pendingLockRequests.size()).append(")\r\n");
      for (Iterator iter = pendingLockRequests.values().iterator(); iter.hasNext();) {
        rv.append('\t').append(iter.next().toString()).append("\r\n");
      }

      return rv.toString();
    } catch (Throwable t) {
      t.printStackTrace();
      return "Exception in toString(): " + t.getMessage();
    }
  }

  private Holder awardLock(ServerThreadContext threadContext, ThreadID requestThreadID, int lockLevel) {
    Assert.assertFalse(isNull());

    Holder holder = getHolder(threadContext);

    Assert.assertNull(holder);
    holder = new Holder(this.lockID, threadContext, this.timeout);
    holder.addLockLevel(lockLevel);
    Object prev = this.holders.put(threadContext, holder);
    Assert.assertNull(prev);
    this.level = holder.getLockLevel();
    notifyAwardLock(holder);
    recordLockAwardStat(holder.getNodeID(), requestThreadID, isGreedyRequest(threadContext), holder.getTimestamp());
    return holder;
  }

  private void notifyAwardLock(Holder holder) {
    final int waitingCount = this.pendingLockRequests.size();

    for (int i = 0; i < listeners.length; i++) {
      listeners[i].notifyAward(waitingCount, holder);
    }
  }

  public synchronized boolean isRead() {
    return LockLevel.READ == this.level;
  }

  public synchronized boolean isWrite() {
    return LockLevel.WRITE == this.level;
  }

  private boolean holdsReadLock(ServerThreadContext threadContext) {
    Holder holder = getHolder(threadContext);
    if (holder != null) { return holder.getLockLevel() == LockLevel.READ; }
    return false;
  }

  private Holder getHolder(ServerThreadContext threadContext) {
    return (Holder) this.holders.get(threadContext);
  }

  public Holder getLockHolder(ServerThreadContext threadContext) {
    Holder lockHolder = (Holder) this.holders.get(threadContext);
    if (lockHolder == null) {
      lockHolder = (Holder) this.holders.get(getClientVMContext(threadContext));
    }
    return lockHolder;
  }

  private void notifyAddPending(Holder holder) {
    final int waitingCount = this.pendingLockRequests.size();

    for (int i = 0; i < this.listeners.length; i++) {
      this.listeners[i].notifyAddPending(waitingCount, holder);
    }
  }

  synchronized int getWaiterCount() {
    return this.waiters.size();
  }

  synchronized boolean hasPending() {
    return pendingLockRequests.size() > 0;
  }

  synchronized boolean hasWaiting() {
    return this.waiters.size() > 0;
  }

  boolean hasGreedyHolders() {
    return this.greedyHolders.size() > 0;
  }

  synchronized boolean hasWaiting(ServerThreadContext threadContext) {
    return (this.waiters.get(threadContext) != null);
  }

  public LockID getLockID() {
    return lockID;
  }

  public boolean isNull() {
    return this.isNull;
  }

  public int hashCode() {
    return this.lockID.hashCode();
  }

  public boolean equals(Object obj) {
    if (obj instanceof Lock) {
      Lock other = (Lock) obj;
      return this.lockID.equals(other.lockID);
    }
    return false;
  }

  private boolean readHolder() {
    // We only need to check the first holder as we cannot have 2 holder, one holding a READ and another holding a
    // WRITE.
    Holder holder = (Holder) holders.values().iterator().next();
    return holder != null && LockLevel.isRead(holder.getLockLevel());
  }

  synchronized boolean nextPending() {
    Assert.eval(!isNull());
    // debug("nextPending() - BEGIN -");

    boolean clear;
    try {
      // Lock upgrade is not supported.
      if (!pendingLockRequests.isEmpty()) {
        Request request = (Request) pendingLockRequests.get(pendingLockRequests.get(0));
        int reqLockLevel = request.getLockLevel();

        boolean canGrantRequest = (reqLockLevel == LockLevel.READ) ? (holders.isEmpty() || readHolder()) : holders
            .isEmpty();
        if (canGrantRequest) {

          switch (reqLockLevel) {
            case LockLevel.WRITE: {
              pendingLockRequests.remove(0);
              cancelTryLockTimer(request);
              // Give locks greedily only if there is no one waiting or pending for this lock
              if (isPolicyGreedy()) {
                if (getWaiterCount() == 0) {
                  boolean isAllPendingRequestsFromRequestNode = isAllPendingLockRequestsFromNode(request
                      .getRequesterID());
                  if (LOCK_LEASE_ENABLE || isAllPendingRequestsFromRequestNode) {
                    grantGreedyRequest(request);
                    if (LOCK_LEASE_ENABLE && !isAllPendingRequestsFromRequestNode) {
                      recallIfPending(LockLevel.WRITE);
                    }
                  } else {
                    grantRequest(request);
                  }
                } else {
                  // When there are other clients that are waiting on the lock, we do not grant the lock greedily
                  // because the client who
                  // own the greedy lock may do a notify and the local wait will get wake up. This may starve the wait
                  // in the other clients.
                  grantRequest(request);
                }
              } else {
                grantRequest(request);
              }
              break;
            }
            case LockLevel.READ: {
              // debug("nextPending() - granting READ request -", request);
              awardAllReads();
              break;
            }
            default: {
              throw new TCInternalError("Unknown lock level in request: " + reqLockLevel);
            }
          }
        }
      }
    } finally {
      clear = holders.size() == 0 && this.waiters.size() == 0 && this.pendingLockRequests.size() == 0;
    }

    return clear;
  }

  private Request cancelTryLockTimer(Request request) {
    if (!(request instanceof TryLockRequest)) { return null; }

    ServerThreadContext requestThreadContext = request.getThreadContext();
    TimerTask recallTimer = (TimerTask) tryLockTimers.remove(requestThreadContext);
    if (recallTimer != null) {
      recallTimer.cancel();
      return request;
    }
    return null;
  }

  private void grantGreedyRequest(Request request) {
    // debug("grantGreedyRequest() - BEGIN -", request);
    ServerThreadContext threadContext = request.getThreadContext();
    awardGreedyAndRespond(threadContext, request.getLockLevel(), request.getLockResponseSink());
  }

  private void grantRequest(Request request) {
    // debug("grantRequest() - BEGIN -", request);
    ServerThreadContext threadContext = request.getThreadContext();
    awardLock(threadContext, threadContext.getId().getClientThreadID(), request.getLockLevel());
    request.execute(lockID);
  }

  /**
   * Remove the specified lock hold.
   *
   * @return true if the current hold was an upgrade
   */
  synchronized boolean removeCurrentHold(ServerThreadContext threadContext) {
    // debug("removeCurrentHold() - BEGIN -", threadContext);
    Holder holder = getHolder(threadContext);
    if (holder != null) {
      this.holders.remove(threadContext);
      if (isGreedyRequest(threadContext)) {
        removeGreedyHolder(threadContext.getId().getNodeID());
      }
      this.level = (holders.size() == 0 ? LockLevel.NIL_LOCK_LEVEL : LockLevel.READ);
      notifyRevoke(holder);
      recordLockReleaseStat(holder.getNodeID(), holder.getThreadID());
    }
    return false;
  }

  synchronized boolean recallCommit(ServerThreadContext threadContext) {
    // debug("recallCommit() - BEGIN -", threadContext);
    Assert.assertTrue(isGreedyRequest(threadContext));
    boolean issueRecall = !recalled;
    removeCurrentHold(threadContext);
    if (issueRecall) {
      recall(LockLevel.WRITE);
    }
    if (recalled == false) { return nextPending(); }
    return false;
  }

  private synchronized void removeGreedyHolder(NodeID nodeID) {
    // debug("removeGreedyHolder() - BEGIN -", channelID);
    greedyHolders.remove(nodeID);
    if (!hasGreedyHolders()) {
      recalled = false;
    }
  }

  synchronized void awardAllReads() {
    // debug("awardAllReads() - BEGIN -");
    List pendingReadLockRequests = new ArrayList(pendingLockRequests.size());
    boolean hasPendingWrites = false;

    for (Iterator i = pendingLockRequests.values().iterator(); i.hasNext();) {
      Request request = (Request) i.next();
      if (request.getLockLevel() == LockLevel.READ) {
        pendingReadLockRequests.add(request);
        i.remove();
      } else if (!hasPendingWrites && request.getLockLevel() == LockLevel.WRITE) {
        hasPendingWrites = true;
      }
    }

    for (Iterator i = pendingReadLockRequests.iterator(); i.hasNext();) {
      Request request = (Request) i.next();
      cancelTryLockTimer(request);
      if (isPolicyGreedy()) {
        ServerThreadContext tid = request.getThreadContext();
        if (!holdsGreedyLock(tid)) {
          if (LOCK_LEASE_ENABLE || !hasPendingWrites) {
            grantGreedyRequest(request);
          } else {
            grantRequest(request);
          }
        }
      } else {
        grantRequest(request);
      }
    }
    if (LOCK_LEASE_ENABLE && hasPendingWrites) {
      recall(LockLevel.WRITE);
    }
  }

  synchronized boolean holdsSomeLock(NodeID nodeID) {
    for (Iterator iter = holders.values().iterator(); iter.hasNext();) {
      Holder holder = (Holder) iter.next();
      if (holder.getNodeID().equals(nodeID)) { return true; }
    }
    return false;
  }

  synchronized boolean holdsGreedyLock(ServerThreadContext threadContext) {
    return (greedyHolders.get(threadContext.getId().getNodeID()) != null);
  }

  synchronized boolean canAwardGreedilyOnTheClient(ServerThreadContext threadContext, int lockLevel) {
    Holder holder = (Holder) greedyHolders.get(threadContext.getId().getNodeID());
    if (holder != null) { return (LockLevel.isWrite(holder.getLockLevel()) || holder.getLockLevel() == lockLevel); }
    return false;
  }

  private void notifyRevoke(Holder holder) {
    for (int i = 0; i < this.listeners.length; i++) {
      this.listeners[i].notifyRevoke(holder);
    }
  }

  void notifyStarted(TimerCallback callback, TCLockTimer timer) {
    for (Iterator i = waiters.values().iterator(); i.hasNext();) {
      LockWaitContext ctxt = (LockWaitContext) i.next();
      scheduleWait(callback, timer, ctxt);
    }
  }

  synchronized boolean isAllPendingLockRequestsFromNode(NodeID nodeID) {
    for (Iterator i = pendingLockRequests.values().iterator(); i.hasNext();) {
      Request r = (Request) i.next();
      if (!r.getRequesterID().equals(nodeID)) { return false; }
    }
    return true;
  }

  /**
   * This clears out stuff from the pending and wait lists that belonged to a dead session. It occurs to me that this is
   * a race condition because a request could come in on the connection, then the cleanup could happen, and then the
   * request could be processed. We need to drop requests that are processed after the cleanup
   *
   * @param nid
   */
  synchronized void clearStateForNode(NodeID nid) {
    // debug("clearStateForChannel() - BEGIN -", channelId);
    for (Iterator i = holders.values().iterator(); i.hasNext();) {
      Holder holder = (Holder) i.next();
      if (holder.getNodeID().equals(nid)) {
        i.remove();
      }
    }
    for (Iterator i = pendingLockRequests.values().iterator(); i.hasNext();) {
      Request r = (Request) i.next();
      if (r.getRequesterID().equals(nid)) {
        i.remove();
      }
    }

    for (Iterator i = waiters.values().iterator(); i.hasNext();) {
      LockWaitContext wc = (LockWaitContext) i.next();
      if (wc.getNodeID().equals(nid)) {
        i.remove();
      }
    }

    for (Iterator i = waitTimers.keySet().iterator(); i.hasNext();) {
      LockWaitContext wc = (LockWaitContext) i.next();
      if (wc.getNodeID().equals(nid)) {
        try {
          TimerTask task = (TimerTask) waitTimers.get(wc);
          task.cancel();
        } finally {
          i.remove();
        }
      }
    }
    removeGreedyHolder(nid);
  }

  synchronized void checkAndClearStateOnGreedyAward(ThreadID clientThreadID, NodeID nodeID, int requestedLevel) {
    // We dont want to award a greedy lock if there are waiters. Lock upgrade is not a problem as it is no longer
    // supported.
    Assert.assertTrue((requestedLevel == LockLevel.READ) || (waiters.size() == 0));

    for (Iterator i = holders.values().iterator(); i.hasNext();) {
      Holder holder = (Holder) i.next();
      if (holder.getNodeID().equals(nodeID)) {
        i.remove();
      }
    }
    for (Iterator i = pendingLockRequests.values().iterator(); i.hasNext();) {
      Request r = (Request) i.next();
      if (r.getRequesterID().equals(nodeID)) {
        // debug("checkAndClear... removing request = ", r);
        i.remove();
        cancelTryLockTimer(r);
      }
    }
  }

  private void recordLockRequestStat(NodeID nodeID, ThreadID threadID) {
    lockStatsManager.recordLockRequested(lockID, nodeID, threadID, lockType, pendingLockRequests.size());
  }

  private void recordLockAwardStat(NodeID nodeID, ThreadID threadID, boolean isGreedyRequest, long awardTimestamp) {
    lockStatsManager.recordLockAwarded(lockID, nodeID, threadID, isGreedyRequest, awardTimestamp);
  }

  private void recordLockReleaseStat(NodeID nodeID, ThreadID threadID) {
    lockStatsManager.recordLockReleased(lockID, nodeID, threadID);
  }

  private void recordLockHoppedStat() {
    lockStatsManager.recordLockHopRequested(lockID);
  }

  private void recordLockRejectStat(NodeID nodeID, ThreadID threadID) {
    lockStatsManager.recordLockRejected(lockID, nodeID, threadID);
  }

  // I wish we were using 1.5 !!!
  // private void debug(Object o1, Object o2) {
  // logger.warn(lockID + String.valueOf(o1) + String.valueOf(o2));
  // }
  //
  // private void debug(Object o1, Object o2, Object o3) {
  // logger.warn(lockID + String.valueOf(o1) + String.valueOf(o2) + String.valueOf(o3));
  // }
  //
  // private void debug(Object o1, Object o2, Object o3, Object o4) {
  // logger.warn(lockID + String.valueOf(o1) + String.valueOf(o2) + String.valueOf(o3) + String.valueOf(o4));
  // }
  //
  // private void debug(Object o) {
  // logger.warn(lockID + String.valueOf(o));
  // }

}
