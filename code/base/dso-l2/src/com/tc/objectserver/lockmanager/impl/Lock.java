/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.lockmanager.impl;

import org.apache.commons.collections.map.ListOrderedMap;

import com.tc.async.api.Sink;
import com.tc.exception.TCInternalError;
import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.net.protocol.tcm.ChannelID;
import com.tc.object.lockmanager.api.LockContext;
import com.tc.object.lockmanager.api.LockID;
import com.tc.object.lockmanager.api.LockLevel;
import com.tc.object.lockmanager.api.ServerThreadID;
import com.tc.object.lockmanager.api.ThreadID;
import com.tc.object.lockmanager.api.WaitTimer;
import com.tc.object.lockmanager.api.WaitTimerCallback;
import com.tc.object.net.DSOChannelManager;
import com.tc.object.tx.WaitInvocation;
import com.tc.objectserver.context.LockResponseContext;
import com.tc.objectserver.lockmanager.api.LockEventListener;
import com.tc.objectserver.lockmanager.api.LockHolder;
import com.tc.objectserver.lockmanager.api.LockMBean;
import com.tc.objectserver.lockmanager.api.LockWaitContext;
import com.tc.objectserver.lockmanager.api.NotifiedWaiters;
import com.tc.objectserver.lockmanager.api.ServerLockRequest;
import com.tc.objectserver.lockmanager.api.TCIllegalMonitorStateException;
import com.tc.objectserver.lockmanager.api.Waiter;
import com.tc.util.Assert;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TimerTask;

public class Lock {
  private static final TCLogger            logger              = TCLogging.getLogger(Lock.class);
  public final static Lock                 NULL_LOCK           = new Lock(LockID.NULL_ID, 0,
                                                                          new LockEventListener[] {}, true,
                                                                          LockManagerImpl.ALTRUISTIC_LOCK_POLICY,
                                                                          ServerThreadContextFactory.DEFAULT_FACTORY);

  private static final int                 UPGRADE             = LockLevel.READ | LockLevel.WRITE;

  private final LockEventListener[]        listeners;
  private final Map                        greedyHolders       = new HashMap();
  private final Map                        holders             = new HashMap();
  private final List                       pendingLockRequests = new LinkedList();
  private final List                       pendingLockUpgrades = new LinkedList();
  private final ListOrderedMap             waiters             = new ListOrderedMap();
  private final Map                        waitTimers          = new HashMap();
  private final LockID                     lockID;
  private final long                       timeout;
  private final boolean                    isNull;
  private int                              level;
  private boolean                          recalled            = false;

  private int                              lockPolicy;
  private final ServerThreadContextFactory threadContextFactory;

  // real constructor used by lock manager
  Lock(LockID lockID, ServerThreadContext txn, int lockLevel, Sink lockResponseSink, long timeout,
       LockEventListener[] listeners, int lockPolicy, ServerThreadContextFactory threadContextFactory) {
    this(lockID, timeout, listeners, false, lockPolicy, threadContextFactory);
    requestLock(txn, lockLevel, lockResponseSink);
  }

  // real constructor used by lock manager when re-establishing waits and lock holds on
  // restart.
  Lock(LockID lockID, ServerThreadContext txn, long timeout, LockEventListener[] listeners, int lockPolicy,
       ServerThreadContextFactory threadContextFactory) {
    this(lockID, timeout, listeners, false, lockPolicy, threadContextFactory);
  }

  // for tests
  Lock(LockID lockID, long timeout, LockEventListener[] listeners) {
    this(lockID, timeout, listeners, false, LockManagerImpl.ALTRUISTIC_LOCK_POLICY,
         ServerThreadContextFactory.DEFAULT_FACTORY);
  }

  private Lock(LockID lockID, long timeout, LockEventListener[] listeners, boolean isNull, int lockPolicy,
               ServerThreadContextFactory threadContextFactory) {
    this.lockID = lockID;
    this.listeners = listeners;
    this.timeout = timeout;
    this.isNull = isNull;
    this.lockPolicy = lockPolicy;
    this.threadContextFactory = threadContextFactory;
  }

  static LockResponseContext createLockRejectedResponseContext(LockID lockID, ServerThreadID threadID, int level) {
    return new LockResponseContext(lockID, threadID.getChannelID(), threadID.getClientThreadID(), level,
                                   LockResponseContext.LOCK_NOT_AWARDED);
  }

  static LockResponseContext createLockAwardResponseContext(LockID lockID, ServerThreadID threadID, int level) {
    return new LockResponseContext(lockID, threadID.getChannelID(), threadID.getClientThreadID(), level,
                                   LockResponseContext.LOCK_AWARD);
  }

  static LockResponseContext createLockRecallResponseContext(LockID lockID, ServerThreadID threadID, int level) {
    return new LockResponseContext(lockID, threadID.getChannelID(), threadID.getClientThreadID(), level,
                                   LockResponseContext.LOCK_RECALL);
  }

  static LockResponseContext createLockWaitTimeoutResponseContext(LockID lockID, ServerThreadID threadID, int level) {
    return new LockResponseContext(lockID, threadID.getChannelID(), threadID.getClientThreadID(), level,
                                   LockResponseContext.LOCK_WAIT_TIMEOUT);
  }

  static LockResponseContext createLockQueriedResponseContext(LockID lockID, ServerThreadID threadID, int level,
                                                              int lockRequestQueueLength, int lockUpgradeQueueLength,
                                                              Collection greedyHolders, Collection holders,
                                                              Collection waiters) {
    return new LockResponseContext(lockID, threadID.getChannelID(), threadID.getClientThreadID(), level,
                                   lockRequestQueueLength, lockUpgradeQueueLength, greedyHolders, holders, waiters,
                                   LockResponseContext.LOCK_INFO);
  }

  synchronized LockMBean getMBean(DSOChannelManager channelManager) {
    int count;
    LockHolder[] holds = new LockHolder[this.holders.size()];
    ServerLockRequest[] reqs = new ServerLockRequest[this.pendingLockRequests.size()];
    ServerLockRequest[] upgrades = new ServerLockRequest[this.pendingLockUpgrades.size()];
    Waiter[] waits = new Waiter[this.waiters.size()];

    count = 0;
    for (Iterator i = this.holders.values().iterator(); i.hasNext();) {
      Holder h = (Holder) i.next();
      ChannelID cid = h.getChannelID();
      holds[count++] = new LockHolder(cid, channelManager.getChannelAddress(cid), h.getThreadID(), h.getLockLevel(), h
          .getTimestamp());
    }

    count = 0;
    for (Iterator i = this.pendingLockRequests.iterator(); i.hasNext();) {
      Request r = (Request) i.next();
      ChannelID cid = r.getRequesterID();
      reqs[count++] = new ServerLockRequest(cid, channelManager.getChannelAddress(cid), r.getSourceID(), r
          .getLockLevel(), r.getTimestamp());
    }

    count = 0;
    for (Iterator i = this.pendingLockUpgrades.iterator(); i.hasNext();) {
      Request r = (Request) i.next();
      ChannelID cid = r.getRequesterID();
      upgrades[count++] = new ServerLockRequest(cid, channelManager.getChannelAddress(cid), r.getSourceID(), r
          .getLockLevel(), r.getTimestamp());
    }

    count = 0;
    for (Iterator i = this.waiters.values().iterator(); i.hasNext();) {
      LockWaitContext wc = (LockWaitContext) i.next();
      ChannelID cid = wc.getChannelID();
      waits[count++] = new Waiter(cid, channelManager.getChannelAddress(cid), wc.getThreadID(), wc.getWaitInvocation(),
                                  wc.getTimestamp());
    }

    return new LockMBeanImpl(lockID, holds, reqs, upgrades, waits);
  }

  synchronized void queryLock(ServerThreadContext txn, Sink lockResponseSink) {
    if (!hasGreedyHolders()) {
      lockResponseSink.add(createLockQueriedResponseContext(this.lockID, txn.getId(), this.level,
                                                            this.pendingLockRequests.size(), this.pendingLockUpgrades
                                                                .size(), this.greedyHolders.values(), this.holders
                                                                .values(), this.waiters.values()));
    } else {
      // TODO:
      // The Remote Lock Manager needs to ask the client for lock information when greedy lock is awarded.
      // Currently, the Remote Lock Manager responds to queryLock by looking at the server only.
      lockResponseSink.add(createLockQueriedResponseContext(this.lockID, txn.getId(), this.level,
                                                            this.pendingLockRequests.size(), this.pendingLockUpgrades
                                                                .size(), this.greedyHolders.values(), this.holders
                                                                .values(), this.waiters.values()));
    }
  }

  synchronized boolean tryRequestLock(ServerThreadContext txn, int requestedLockLevel, Sink lockResponseSink) {
    return requestLock(txn, requestedLockLevel, lockResponseSink, true);
  }

  synchronized boolean requestLock(ServerThreadContext txn, int requestedLockLevel, Sink lockResponseSink) {
    return requestLock(txn, requestedLockLevel, lockResponseSink, false);
  }

  // XXX:: UPGRADE Requests can come in with requestLockLevel == UPGRADE on a notified wait during server crash
  private synchronized boolean requestLock(ServerThreadContext txn, int requestedLockLevel, Sink lockResponseSink,
                                           boolean noBlock) {

    // debug("requestLock - BEGIN -", txn, ",", LockLevel.toString(requestedLockLevel));
    // it is an error (probably originating from the client side) to
    // request a lock you already hold
    Holder holder = getHolder(txn);
    if (noBlock && holder == null && (getHoldersCount() > 0 || hasGreedyHolders())) {
      cannotAwardAndRespond(txn, requestedLockLevel, lockResponseSink);
      return false;
    }

    if (holder != null) {
      if (LockLevel.NIL_LOCK_LEVEL != (holder.getLockLevel() & requestedLockLevel)) {
        // formatting
        throw new AssertionError("Client requesting already held lock! holder=" + holder + ", lock=" + this);
      }
    }
    if (waiters.containsKey(txn)) throw new AssertionError("Attempt to request a lock in a Thread "
                                                           + "that is already part of the wait set. lock = " + this);

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
          addPending(txn, requestedLockLevel, lockResponseSink);
        }
        return false;
      }
    }

    // Lock granting logic:
    // 0. If no one is holding this lock, go ahead and award it
    // 1. If only a read lock is held and no write locks are pending, and another read
    // (and only read) lock is requested, award it. If Write locks are pending, we dont want to
    // starve the WRITES by keeping on awarding READ Locks.
    // 2. If there is only one holder, that hold is a read lock, the holder is
    // the requestor, and the requestor wants a write lock...then this is an
    // awardable lock upgrade
    // 3. Else the request must be queued (ie. added to pending list)

    if ((getHoldersCount() == 0) || ((!hasPending()) && ((requestedLockLevel == LockLevel.READ) && this.isRead()))) {
      // (0, 1) uncontended or additional read lock
      if (isPolicyGreedy() && (requestedLockLevel != UPGRADE)
          && ((requestedLockLevel == LockLevel.READ) || (getWaiterCount() == 0))) {
        awardGreedyAndRespond(txn, requestedLockLevel, lockResponseSink);
      } else {
        awardAndRespond(txn, requestedLockLevel, lockResponseSink);
      }
    } else if ((getHoldersCount() == 1) && holdsReadLock(txn) && LockLevel.isWrite(requestedLockLevel)) {
      // (2) allowed lock upgrade
      if (isPolicyGreedy() && isGreedyRequest(txn)) {
        // XXX::Currently Greedy upgrades are not supported. Client never does a greedy request.
        requestedLockLevel = LockLevel.makeGreedy(requestedLockLevel);
      }
      awardAndRespond(txn, requestedLockLevel, lockResponseSink);
    } else {
      // (3) queue request
      if (isPolicyGreedy() && hasGreedyHolders()) {
        recall(requestedLockLevel);
      }
      if (!holdsGreedyLock(txn)) {
        addPending(txn, requestedLockLevel, lockResponseSink);
      }
      return false;
    }

    return true;
  }

  synchronized void addRecalledHolder(ServerThreadContext txn, int lockLevel) {
    // debug("addRecalledHolder - BEGIN -", txn, ",", LockLevel.toString(lockLevel));
    if (!LockLevel.isWrite(level) && LockLevel.isWrite(lockLevel)) {
      // Client issued a WRITE lock without holding a GREEDY WRITE. Bug in the client.
      throw new AssertionError("Client issued a WRITE lock without holding a GREEDY WRITE !");
    }
    awardLock(txn, lockLevel);
    if (LockLevel.isRead(lockLevel) && pendingLockRequests.size() > 0) {
      // Check to see if we have any lock request for this Thread that needs to go to lock upgrade
      for (Iterator iter = pendingLockRequests.iterator(); iter.hasNext();) {
        Request request = (Request) iter.next();
        if (request.getThreadContext().equals(txn) && LockLevel.isWrite(request.getLockLevel())) {
          iter.remove();
          pendingLockUpgrades.add(request);
          break;
        }
      }
    }
  }

  synchronized void addRecalledPendingRequest(ServerThreadContext txn, int lockLevel, Sink lockResponseSink) {
    // debug("addRecalledPendingRequest - BEGIN -", txn, ",", LockLevel.toString(lockLevel));
    addPending(txn, lockLevel, lockResponseSink);
  }

  private synchronized void recall(int recallLevel) {
    if (recalled) { return; }
    for (Iterator i = greedyHolders.values().iterator(); i.hasNext();) {
      Holder holder = (Holder) i.next();
      // debug("recall() - issued for -", holder);
      holder.getSink().add(
                           createLockRecallResponseContext(holder.getLockID(), holder.getThreadContext().getId(),
                                                           recallLevel));
      recalled = true;
    }
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

    ChannelID ch = txn.getId().getChannelID();
    checkAndClearStateOnGreedyAward(ch, requestedLockLevel);
    awardAndRespond(clientTx, greedyLevel, lockResponseSink);
    Holder holder = getHolder(clientTx);
    holder.setSink(lockResponseSink);
    greedyHolders.put(ch, holder);
    clearWaitingOn(txn);
  }

  private void cannotAwardAndRespond(ServerThreadContext txn, int requestedLockLevel, Sink lockResponseSink) {
    lockResponseSink.add(createLockRejectedResponseContext(this.lockID, txn.getId(), requestedLockLevel));
  }

  private void awardAndRespond(ServerThreadContext txn, int requestedLockLevel, Sink lockResponseSink) {
    // debug("awardRespond() - BEGIN - ", txn, ",", LockLevel.toString(requestedLockLevel));
    awardLock(txn, requestedLockLevel);
    lockResponseSink.add(createLockAwardResponseContext(this.lockID, txn.getId(), requestedLockLevel));
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
        createPendingFromWaiter(wait);
        addNotifiedWaitersTo.addNotification(new LockContext(lockID, wait.getChannelID(), wait.getThreadID(), wait.lockLevel()));
      }
    }
  }

  synchronized void interrupt(ServerThreadContext txn) {
    if (waiters.size() == 0 || !waiters.containsKey(txn)) {
      logger.warn("Cannot interrupt: " + txn + " is not waiting.");
      return;
    }
    LockWaitContext wait = (LockWaitContext)waiters.remove(txn);
    removeAndCancelWaitTimer(wait);
    createPendingFromWaiter(wait);
  }

  private void removeAndCancelWaitTimer(LockWaitContext wait) {
    TimerTask task = (TimerTask) waitTimers.remove(wait);
    if (task != null) task.cancel();
  }

  private void createPendingFromWaiter(LockWaitContext wait) {
    // XXX: This cast to WaitContextImpl is lame. I'm not sure how to refactor it right now.
    Request request = new Request(((LockWaitContextImpl) wait).getThreadContext(), wait.lockLevel(), wait
        .getLockResponseSink());
    pendingLockRequests.add(request);
    if (isPolicyGreedy() && hasGreedyHolders()) {
      recall(LockLevel.WRITE);
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

      if (holders.size() == 0) {
        if (isPolicyGreedy() && (getWaiterCount() == 0)) {
          awardGreedyAndRespond(txn, lockLevel, lockResponseSink);
        } else {
          awardAndRespond(txn, lockLevel, lockResponseSink);
        }
      } else {
        createPendingFromWaiter(context);
      }
    }
  }

  synchronized void wait(ServerThreadContext txn, WaitTimer waitTimer, WaitInvocation call, WaitTimerCallback callback,
                         Sink lockResponseSink) throws TCIllegalMonitorStateException {
    // debug("wait() - BEGIN -", txn, ", ", call);
    if (waiters.containsKey(txn)) throw Assert.failure("Already in wait set: " + txn);
    checkLegalWaitNotifyState(txn);

    Holder current = getHolder(txn);
    Assert.assertNotNull(current);
    boolean isUpgrade = current.isUpgrade();

    LockWaitContext waitContext = new LockWaitContextImpl(txn, this, call, current.getLockLevel(), lockResponseSink);
    waiters.put(txn, waitContext);

    scheduleWait(callback, waitTimer, waitContext);
    txn.setWaitingOn(this);
    removeCurrentHold(txn);

    if (isUpgrade) {
      // wait()'ing on an upgraded lock needs to release both the READ and WRITE
      // locks held
      removeCurrentHold(txn);
    }

    nextPending();
  }

  // This method reestablished Wait State and schedules wait timeouts too. There are cases where we may need to ignore a
  // wait, if we already know about it. Note that it could be either in waiting or pending state.
  synchronized void addRecalledWaiter(ServerThreadContext txn, WaitInvocation call, int lockLevel,
                                      Sink lockResponseSink, WaitTimer waitTimer, WaitTimerCallback callback) {
    // debug("addRecalledWaiter() - BEGIN -", txn, ", ", call);

    LockWaitContext waitContext = new LockWaitContextImpl(txn, this, call, lockLevel, lockResponseSink);
    if (waiters.containsKey(txn)) {
      logger.debug("addRecalledWaiter(): Ignoring " + waitContext + " as it is already in waiters list.");
      return;
    }
    Request request = new Request(txn, lockLevel, lockResponseSink);
    if (pendingLockRequests.contains(request)) {
      logger.debug("addRecalledWaiter(): Ignoring " + waitContext + " as it is already in pending list.");
      return;
    }
    waiters.put(txn, waitContext);
    scheduleWait(callback, waitTimer, waitContext);
  }

  // This method reestablished Wait State and does not schedules wait timeouts too. This is
  // called when LockManager is starting and wait timers are started when the lock Manager is started.
  synchronized void reestablishWait(ServerThreadContext txn, WaitInvocation call, int lockLevel, Sink lockResponseSink) {
    LockWaitContext waitContext = new LockWaitContextImpl(txn, this, call, lockLevel, lockResponseSink);
    Object old = waiters.put(txn, waitContext);
    if (old != null) throw Assert.failure("Already in wait set: " + txn);
  }

  synchronized void reestablishLock(ServerThreadContext threadContext, int requestedLevel, Sink lockResponseSink) {
    if ((LockLevel.isWrite(requestedLevel) && holders.size() != 0)
        || (LockLevel.isRead(requestedLevel) && LockLevel.isWrite(this.level))) {
      //
      throw new AssertionError("Lock " + this + " already held by other Holder. Can't grant to " + threadContext
                               + LockLevel.toString(requestedLevel));

    }
    if (waiters.get(threadContext) != null) {
      //
      throw new AssertionError("Thread " + threadContext + "is already in Wait state for Lock " + this
                               + ". Can't grant Lock Hold !");
    }
    if (isGreedyRequest(threadContext)) {
      int greedyLevel = LockLevel.makeGreedy(requestedLevel);
      ChannelID ch = threadContext.getId().getChannelID();
      awardLock(threadContext, greedyLevel);
      Holder holder = getHolder(threadContext);
      holder.setSink(lockResponseSink);
      greedyHolders.put(ch, holder);
    } else {
      awardLock(threadContext, requestedLevel);
    }
  }

  private void scheduleWait(WaitTimerCallback callback, WaitTimer waitTimer, LockWaitContext waitContext) {
    final TimerTask timer = waitTimer.scheduleTimer(callback, waitContext.getWaitInvocation(), waitContext);
    if (timer != null) {
      waitTimers.put(waitContext, timer);
    }
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
    return threadContextFactory.getOrCreate(threadContext.getId().getChannelID(), ThreadID.VM_ID);
  }

  public synchronized int getHoldersCount() {
    return holders.size();
  }

  public synchronized int getPendingCount() {
    return pendingLockRequests.size();
  }

  public synchronized int getPendingUpgradeCount() {
    return pendingLockUpgrades.size();
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
      for (Iterator iter = pendingLockRequests.iterator(); iter.hasNext();) {
        rv.append('\t').append(iter.next().toString()).append("\r\n");
      }

      rv.append("Pending lock upgrades (").append(pendingLockUpgrades.size()).append(")\r\n");
      for (Iterator iter = pendingLockUpgrades.iterator(); iter.hasNext();) {
        rv.append('\t').append(iter.next().toString()).append("\r\n");
      }

      return rv.toString();
    } catch (Throwable t) {
      t.printStackTrace();
      return "Exception in toString(): " + t.getMessage();
    }
  }

  private void awardLock(ServerThreadContext threadContext, int lockLevel) {
    Assert.assertFalse(isNull());

    Holder holder = getHolder(threadContext);

    if (holder != null) {
      holder.addLockLevel(lockLevel);
      this.level = holder.getLockLevel();
    } else {
      threadContext.addLock(this);
      holder = new Holder(this.lockID, threadContext, this.timeout);
      holder.addLockLevel(lockLevel);
      Object prev = this.holders.put(threadContext, holder);
      Assert.assertNull(prev);
      this.level = holder.getLockLevel();
      notifyAwardLock(holder);
    }

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

  // XXX:: Note that lockLevel == UPGRADE (in notified waits) also get into pendingLockRequests which is correct
  synchronized void addPending(ServerThreadContext threadContext, int lockLevel, Sink awardLockSink) {
    Assert.assertFalse(isNull());
    // debug("addPending() - BEGIN -", threadContext, ", ", LockLevel.toString(lockLevel));

    Request request = new Request(threadContext, lockLevel, awardLockSink);

    if ((lockLevel == LockLevel.WRITE) && holdsReadLock(threadContext)) {
      // this is a lock upgrade request
      this.pendingLockUpgrades.add(request);
    } else {
      if (pendingLockRequests.contains(request)) {
        logger.debug("Ignoring existing Request " + request + " in Lock " + lockID);
        return;
      }

      this.pendingLockRequests.add(request);
      for (Iterator currentHolders = holders.values().iterator(); currentHolders.hasNext();) {
        Holder holder = (Holder) currentHolders.next();
        notifyAddPending(holder);
      }
    }

    threadContext.setWaitingOn(this);
  }

  private boolean holdsReadLock(ServerThreadContext threadContext) {
    Holder holder = getHolder(threadContext);
    if (holder != null) { return holder.getLockLevel() == LockLevel.READ; }
    return false;
  }

  private Holder getHolder(ServerThreadContext threadContext) {
    return (Holder) this.holders.get(threadContext);
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
    return pendingLockRequests.size() > 0 || pendingLockUpgrades.size() > 0;
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

  synchronized boolean nextPending() {
    Assert.eval(!isNull());
    // debug("nextPending() - BEGIN -");

    boolean clear;
    try {
      // FIXME:: If ever there are two lock upgrade request, then it is a deadlock situation.
      // Can be easily fixed if we know what is the right solution to the problem.
      if ((holders.size() == 1) && (!pendingLockUpgrades.isEmpty())) {
        // Can we award an upgrade?
        Request request = (Request) pendingLockUpgrades.get(0);
        if (holdsReadLock(request.getThreadContext())) {
          // Upgrades are not given greedily
          // debug("nextPending() - Giving Upgrade -", request);
          pendingLockUpgrades.remove(0);
          grantRequest(request);
        }
      } else if (holders.isEmpty()) {
        if (!pendingLockRequests.isEmpty()) {
          Request request = (Request) pendingLockRequests.get(0);
          int reqLockLevel = request.getLockLevel();
          switch (reqLockLevel) {
            case LockLevel.WRITE: {
              // Give locks greedily only if there is no one waiting or pending for this lock
              if (isPolicyGreedy() && isAllPendingLockRequestsFromChannel(request.getRequesterID())
                  && (getWaiterCount() == 0)) {
                // debug("nextPending() - Giving GREEDY WRITE request -", request);
                pendingLockRequests.remove(0);
                grantGreedyRequest(request);
                break;
              }
              // else fall thru
            }
            case UPGRADE: {
              // debug("nextPending() - granting not greedily request -", request);
              // Upgrades are not given greedily
              pendingLockRequests.remove(0);
              grantRequest(request);
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
      clear = holders.size() == 0 && this.waiters.size() == 0 && this.pendingLockRequests.size() == 0
              && this.pendingLockUpgrades.size() == 0;
    }

    return clear;
  }

  private void grantGreedyRequest(Request request) {
    // debug("grantGreedyRequest() - BEGIN -", request);
    ServerThreadContext threadContext = request.getThreadContext();
    awardGreedyAndRespond(threadContext, request.getLockLevel(), request.getLockResponseSink());
    clearWaitingOn(threadContext);
  }

  private void grantRequest(Request request) {
    // debug("grantRequest() - BEGIN -", request);
    ServerThreadContext threadContext = request.getThreadContext();
    awardLock(threadContext, request.getLockLevel());
    clearWaitingOn(threadContext);
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
      if (holder.isUpgrade()) {
        holder.removeLockLevel(LockLevel.WRITE);
        this.level = holder.getLockLevel();
        return true;
      } else {
        this.holders.remove(threadContext);
        threadContext.removeLock(this);
        threadContextFactory.removeIfClear(threadContext);
        if (isGreedyRequest(threadContext)) {
          removeGreedyHolder(threadContext.getId().getChannelID());
        }
        this.level = (holders.size() == 0 ? LockLevel.NIL_LOCK_LEVEL : LockLevel.READ);
        notifyRevoke(holder);
      }
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

  private synchronized void removeGreedyHolder(ChannelID channelID) {
    // debug("removeGreedyHolder() - BEGIN -", channelID);
    greedyHolders.remove(channelID);
    if (!hasGreedyHolders()) {
      recalled = false;
    }
  }

  private void clearWaitingOn(ServerThreadContext threadContext) {
    threadContext.clearWaitingOn();
    threadContextFactory.removeIfClear(threadContext);
  }

  synchronized void awardAllReads() {
    // debug("awardAllReads() - BEGIN -");
    List pendingReadLockRequests = new ArrayList(pendingLockRequests.size());
    boolean hasPendingWrites = false;

    for (Iterator i = pendingLockRequests.iterator(); i.hasNext();) {
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
      if (isPolicyGreedy() && !hasPendingWrites) {
        ServerThreadContext tid = request.getThreadContext();
        if (!holdsGreedyLock(tid)) {
          grantGreedyRequest(request);
        } else {
          // These can be awarded locally in the client ...
          clearWaitingOn(tid);
        }
      } else {
        grantRequest(request);
      }
    }
  }

  synchronized boolean holdsSomeLock(ChannelID ch) {
    for (Iterator iter = holders.values().iterator(); iter.hasNext();) {
      Holder holder = (Holder) iter.next();
      if (holder.getChannelID().equals(ch)) { return true; }
    }
    return false;
  }

  synchronized boolean holdsGreedyLock(ServerThreadContext threadContext) {
    return (greedyHolders.get(threadContext.getId().getChannelID()) != null);
  }

  synchronized boolean canAwardGreedilyOnTheClient(ServerThreadContext threadContext, int lockLevel) {
    Holder holder = (Holder) greedyHolders.get(threadContext.getId().getChannelID());
    if (holder != null) { return (LockLevel.isWrite(holder.getLockLevel()) || holder.getLockLevel() == lockLevel); }
    return false;
  }

  private void notifyRevoke(Holder holder) {
    for (int i = 0; i < this.listeners.length; i++) {
      this.listeners[i].notifyRevoke(holder);
    }
  }

  void notifyStarted(WaitTimerCallback callback, WaitTimer timer) {
    for (Iterator i = waiters.values().iterator(); i.hasNext();) {
      LockWaitContext ctxt = (LockWaitContext) i.next();
      scheduleWait(callback, timer, ctxt);
    }
  }

  synchronized boolean isAllPendingLockRequestsFromChannel(ChannelID channelId) {
    for (Iterator i = pendingLockRequests.iterator(); i.hasNext();) {
      Request r = (Request) i.next();
      if (!r.getRequesterID().equals(channelId)) { return false; }
    }
    return true;
  }

  /**
   * This clears out stuff from the pending and wait lists that belonged to a dead session. It occurs to me that this is
   * a race condition because a request could come in on the connection, then the cleanup could happen, and then the
   * request could be processed. We need to drop requests that are processed after the cleanup
   * 
   * @param channelId
   */
  synchronized void clearStateForChannel(ChannelID channelId) {
    // debug("clearStateForChannel() - BEGIN -", channelId);
    for (Iterator i = holders.values().iterator(); i.hasNext();) {
      Holder holder = (Holder) i.next();
      if (holder.getChannelID().equals(channelId)) {
        i.remove();
      }
    }

    for (Iterator i = pendingLockUpgrades.iterator(); i.hasNext();) {
      Request r = (Request) i.next();
      if (r.getRequesterID().equals(channelId)) {
        i.remove();
      }
    }

    for (Iterator i = pendingLockRequests.iterator(); i.hasNext();) {
      Request r = (Request) i.next();
      if (r.getRequesterID().equals(channelId)) {
        i.remove();
      }
    }

    for (Iterator i = waiters.values().iterator(); i.hasNext();) {
      LockWaitContext wc = (LockWaitContext) i.next();
      if (wc.getChannelID().equals(channelId)) {
        i.remove();
      }
    }

    for (Iterator i = waitTimers.keySet().iterator(); i.hasNext();) {
      LockWaitContext wc = (LockWaitContext) i.next();
      if (wc.getChannelID().equals(channelId)) {
        try {
          TimerTask task = (TimerTask) waitTimers.get(wc);
          task.cancel();
        } finally {
          i.remove();
        }
      }
    }
    removeGreedyHolder(channelId);
  }

  synchronized void checkAndClearStateOnGreedyAward(ChannelID ch, int requestedLevel) {
    // We dont want to award a greedy lock for lock upgrades or if there are waiters.
    // debug("checkAndClearStateOnGreedyAward For ", ch, ", ", LockLevel.toString(requestedLevel));
    // debug("checkAndClear... BEFORE Lock = ", this);
    Assert.assertTrue(pendingLockUpgrades.size() == 0);
    Assert.assertTrue((requestedLevel == LockLevel.READ) || (waiters.size() == 0));

    for (Iterator i = holders.values().iterator(); i.hasNext();) {
      Holder holder = (Holder) i.next();
      if (holder.getChannelID().equals(ch)) {
        i.remove();
      }
    }
    for (Iterator i = pendingLockRequests.iterator(); i.hasNext();) {
      Request r = (Request) i.next();
      if (r.getRequesterID().equals(ch)) {
        if ((requestedLevel == LockLevel.WRITE) || (r.getLockLevel() == requestedLevel)) {
          // debug("checkAndClear... removing request = ", r);
          i.remove();
          ServerThreadContext tid = r.getThreadContext();
          // debug("checkAndClear... clearing threadContext = ", tid);
          clearWaitingOn(tid);
        } else {
          throw new AssertionError("Issuing READ lock greedily when WRITE pending !");
        }
      }
    }
    // debug("checkAndClear... AFTER Lock = ", this);
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
