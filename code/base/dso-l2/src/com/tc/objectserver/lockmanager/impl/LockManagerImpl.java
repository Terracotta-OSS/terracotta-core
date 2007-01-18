/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.lockmanager.impl;

import com.tc.async.api.Sink;
import com.tc.exception.ImplementMe;
import com.tc.logging.CustomerLogging;
import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.net.protocol.tcm.ChannelID;
import com.tc.object.lockmanager.api.LockContext;
import com.tc.object.lockmanager.api.LockID;
import com.tc.object.lockmanager.api.LockLevel;
import com.tc.object.lockmanager.api.ThreadID;
import com.tc.object.lockmanager.api.WaitContext;
import com.tc.object.lockmanager.api.WaitTimer;
import com.tc.object.lockmanager.api.WaitTimerCallback;
import com.tc.object.lockmanager.impl.WaitTimerImpl;
import com.tc.object.net.DSOChannelManager;
import com.tc.object.tx.WaitInvocation;
import com.tc.objectserver.lockmanager.api.DeadlockChain;
import com.tc.objectserver.lockmanager.api.DeadlockResults;
import com.tc.objectserver.lockmanager.api.LockEventListener;
import com.tc.objectserver.lockmanager.api.LockMBean;
import com.tc.objectserver.lockmanager.api.LockManager;
import com.tc.objectserver.lockmanager.api.LockManagerMBean;
import com.tc.objectserver.lockmanager.api.LockWaitContext;
import com.tc.objectserver.lockmanager.api.NotifiedWaiters;
import com.tc.objectserver.lockmanager.api.TCIllegalMonitorStateException;
import com.tc.util.Assert;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Server representation of lock management. We will need to keep track of what locks are checkedout, who has the lock
 * and who wants the lock
 *
 * @author steve
 */
public class LockManagerImpl implements LockManager, LockManagerMBean, WaitTimerCallback {
  private static final TCLogger                   logger                     = TCLogging
                                                                                 .getLogger(LockManagerImpl.class);
  private static final TCLogger                   clogger                    = CustomerLogging.getDSOGenericLogger();

  public static final LockManagerErrorDescription NOT_STARTING_ERROR         = new LockManagerErrorDescription(
                                                                                                               "NOT STARTING");
  public static final LockManagerErrorDescription NOT_STARTED_ERROR          = new LockManagerErrorDescription(
                                                                                                               "NOT STARTED");
  public static final LockManagerErrorDescription IS_STARTING_ERROR          = new LockManagerErrorDescription(
                                                                                                               "IS STARTING");
  public static final LockManagerErrorDescription IS_STOPPED_ERROR           = new LockManagerErrorDescription(
                                                                                                               "IS STOPPED");

  public static final LockManagerErrorDescription LOCK_ALREADY_GRANTED_ERROR = new LockManagerErrorDescription(
                                                                                                               "LOCK ALREADY GRANTED");

  public static final int                         UNINITIALIZED_LOCK_POLICY  = 0x00;
  public static final int                         GREEDY_LOCK_POLICY         = 0x01;
  public static final int                         ALTRUISTIC_LOCK_POLICY     = 0x02;

  private static final State                      STARTING                   = new State("STARTING");
  private static final State                      STARTED                    = new State("STARTED");
  private static final State                      STOPPING                   = new State("STOPPING");
  private static final State                      STOPPED                    = new State("STOPPED");

  private State                                   status                     = STARTING;
  private final Map                               locks                      = new HashMap();
  private final LockEventListener                 lockTimer;
  private final DSOChannelManager                 channelManager;
  private final LockEventListener[]               lockListeners;
  private final WaitTimer                         waitTimer;

  // XXX: These lock timeout/policy needs to be configurable-- probably per lock...
  private final long                              lockTimeout                = 1000 * 60 * 2;
  private int                                     lockPolicy                 = UNINITIALIZED_LOCK_POLICY;
  // private int lockPolicy = ALTRUISTIC_LOCK_POLICY;

  private final List                              lockRequestQueue           = new ArrayList();
  private final ServerThreadContextFactory        threadContextFactory       = new ServerThreadContextFactory();

  public LockManagerImpl(DSOChannelManager channelManager) {
    this.channelManager = channelManager;

    // Replacing real lock timer with a null lock timer until the OOP stuff is
    // put in for real --Orion 2/24/2005
    // this.lockTimer = new LockTimer(this.channelManager);
    this.lockTimer = new NullLockTimer();
    this.lockListeners = new LockEventListener[] { this.lockTimer };

    // This could maybe be combined with the lock timeout stuff, but for now
    // just use a dedicated Timer instance
    this.waitTimer = new WaitTimerImpl();
  }

  public synchronized void dump() {
    StringBuffer buf = new StringBuffer("LockManager");
    buf.append("locks=" + locks).append("\n");
    buf.append("/LockManager").append("\n");
    System.err.println(buf.toString());
  }

  public synchronized int getLockCount() {
    return this.locks.size();
  }

  public synchronized int getThreadContextCount() {
    return this.threadContextFactory.getCount();
  }

  public synchronized void verify(ChannelID channelID, LockID[] lockIDs) {
    if (!isStarted()) { return; }
    for (int i = 0; i < lockIDs.length; i++) {
      Lock lock = (Lock) locks.get(lockIDs[i]);
      if (lock == null) {
        String errorMsg = " Lock is not held for " + lockIDs[i] + ". Not by " + channelID
                          + ". Not by anyone. uhm... Nada";
        logger.warn(errorMsg);
        throw new AssertionError(errorMsg);
      }
      if (!lock.holdsSomeLock(channelID)) { throw new AssertionError(" Lock " + lockIDs[i]
                                                                     + " is not held by anyone in " + channelID); }
    }
  }

  public synchronized void reestablishLock(LockID lockID, ChannelID channelID, ThreadID sourceID, int requestedLevel,
                                           Sink lockResponseSink) {
    assertStarting();
    ServerThreadContext threadContext = threadContextFactory.getOrCreate(channelID, sourceID);
    Lock lock = (Lock) this.locks.get(lockID);

    if (lock == null) {
      lock = new Lock(lockID, threadContext, this.lockTimeout, this.lockListeners, this.lockPolicy,
                      threadContextFactory);
      locks.put(lockID, lock);
    }
    lock.reestablishLock(threadContext, requestedLevel, lockResponseSink);
    /*
     * if (!basicRequestLock(lockID, channelID, threadID, requestedLevel, lockResponseSink)) { // formatter throw new
     * LockManagerError(LOCK_ALREADY_GRANTED_ERROR, "Attempt to reestablish a lock failed. " + "Another client may have
     * already reestablished this lock: " + "lockID=" + lockID + ", channelID=" + channelID + ", threadID=" + threadID + ",
     * requestedLevel=" + requestedLevel); }
     */
  }

  public synchronized boolean tryRequestLock(LockID lockID, ChannelID channelID, ThreadID sourceID, int requestedLevel,
                                             Sink lockResponseSink) {
    return requestLock(lockID, channelID, sourceID, requestedLevel, lockResponseSink, true);
  }

  private synchronized boolean requestLock(LockID lockID, ChannelID channelID, ThreadID threadID, int requestedLevel,
                                           Sink lockResponseSink, boolean noBlock) {
    if (!channelManager.isActiveID(channelID)) return false;
    if (isStarting()) {
      queueRequestLock(lockID, channelID, threadID, requestedLevel, lockResponseSink, noBlock);
      return false;
    }
    if (!isStarted()) return false;
    return basicRequestLock(lockID, channelID, threadID, requestedLevel, lockResponseSink, noBlock);
  }

  public synchronized boolean requestLock(LockID lockID, ChannelID channelID, ThreadID sourceID, int requestedLevel,
                                          Sink lockResponseSink) {
    return requestLock(lockID, channelID, sourceID, requestedLevel, lockResponseSink, false);
  }

  private boolean basicRequestLock(LockID lockID, ChannelID channelID, ThreadID threadID, int requestedLevel,
                                   Sink lockResponseSink, boolean noBlock) {
    ServerThreadContext threadContext = threadContextFactory.getOrCreate(channelID, threadID);
    Lock lock = (Lock) this.locks.get(lockID);

    if (lock != null) {
      if (noBlock) {
        return lock.tryRequestLock(threadContext, requestedLevel, lockResponseSink);
      } else {
        return lock.requestLock(threadContext, requestedLevel, lockResponseSink);
      }
    } else {
      lock = new Lock(lockID, threadContext, requestedLevel, lockResponseSink, this.lockTimeout, this.lockListeners,
                      this.lockPolicy, threadContextFactory);
      locks.put(lockID, lock);
      return true;
    }
  }

  private void queueRequestLock(LockID lockID, ChannelID channelID, ThreadID threadID, int requestedLevel,
                                Sink lockResponseSink, boolean noBlock) {
    lockRequestQueue
        .add(new RequestLockContext(lockID, channelID, threadID, requestedLevel, lockResponseSink, noBlock));
  }

  public synchronized void queryLock(LockID lockID, ChannelID channelID, ThreadID threadID, Sink lockResponseSink) {
    assertNotStarting();
    if (!isStarted()) return;

    Lock lock = getLockFor(lockID);
    ServerThreadContext threadContext = threadContextFactory.getOrCreate(channelID, threadID);
    lock.queryLock(threadContext, lockResponseSink);
  }

  public synchronized void unlock(LockID id, ChannelID channelID, ThreadID threadID) {
    assertNotStarting();
    if (!isStarted()) return;

    Lock l = getLockFor(id);
    if (l.isNull()) {
      logger.warn("An attempt was made to unlock:" + id + " for channelID:" + channelID
                  + " This lock was not held. This could be do to that node being down so it may not be an error.");
      return;
    }
    basicUnlock(l, threadContextFactory.getOrCreate(channelID, threadID));
  }

  public synchronized void wait(LockID lid, ChannelID cid, ThreadID tid, WaitInvocation call, Sink lockResponseSink) {
    assertNotStopped();
    Lock lock = (Lock) this.locks.get(lid);
    if (lock != null) {
      ServerThreadContext threadContext = threadContextFactory.getOrCreate(cid, tid);
      try {
        lock.wait(threadContext, waitTimer, call, this, lockResponseSink);
        notifyAll();
      } catch (TCIllegalMonitorStateException e) {
        e.printStackTrace();
        // XXX: send error response back to client
        throw new ImplementMe();
      }
    } else {
      // XXX: lock doesn't exist...this is bad ;-)
      throw new ImplementMe();
    }
  }

  public synchronized void reestablishWait(LockID lid, ChannelID cid, ThreadID tid, int lockLevel, WaitInvocation call,
                                           Sink lockResponseSink) {
    assertStarting();
    Lock lock = (Lock) this.locks.get(lid);
    ServerThreadContext threadContext = threadContextFactory.getOrCreate(cid, tid);
    if (lock == null) {
      lock = new Lock(lid, threadContext, this.lockTimeout, this.lockListeners, this.lockPolicy, threadContextFactory);
      locks.put(lid, lock);
    }
    lock.reestablishWait(threadContext, call, lockLevel, lockResponseSink);
  }

  public synchronized void recallCommit(LockID lid, ChannelID cid, Collection lockContexts, Collection waitContexts,
                                        Collection pendingLockContexts, Sink lockResponseSink) {
    assertNotStarting();
    Lock lock = (Lock) this.locks.get(lid);
    Assert.assertNotNull(lock);

    synchronized (lock) {
      for (Iterator i = lockContexts.iterator(); i.hasNext();) {
        LockContext ctxt = (LockContext) i.next();
        ServerThreadContext threadContext = threadContextFactory.getOrCreate(cid, ctxt.getThreadID());
        lock.addRecalledHolder(threadContext, ctxt.getLockLevel());
      }

      for (Iterator i = waitContexts.iterator(); i.hasNext();) {
        WaitContext ctxt = (WaitContext) i.next();
        ServerThreadContext threadContext = threadContextFactory.getOrCreate(cid, ctxt.getThreadID());
        lock.addRecalledWaiter(threadContext, ctxt.getWaitInvocation(), ctxt.getLockLevel(), lockResponseSink,
                               waitTimer, this);
      }

      for (Iterator i = pendingLockContexts.iterator(); i.hasNext();) {
        LockContext ctxt = (LockContext) i.next();
        ServerThreadContext threadContext = threadContextFactory.getOrCreate(cid, ctxt.getThreadID());
        lock.addRecalledPendingRequest(threadContext, ctxt.getLockLevel(), lockResponseSink);
      }

      ServerThreadContext threadContext = threadContextFactory.getOrCreate(cid, ThreadID.VM_ID);
      if (lock.recallCommit(threadContext)) {
        locks.remove(lid);
        threadContextFactory.removeIfClear(threadContext);
      }
    }
  }

  public synchronized void waitTimeout(Object callbackObject) {
    if (isStarted() && callbackObject instanceof LockWaitContext) {
      LockWaitContext context = (LockWaitContext) callbackObject;
      context.waitTimeout();
    } else {
      logger.warn("Ignoring wait timeout for : " + callbackObject);
    }
  }

  public synchronized void notify(LockID lid, ChannelID cid, ThreadID tid, boolean all,
                                  NotifiedWaiters addNotifiedWaitersTo) {
    // assertStarted();
    if (!isStarted()) {
      if (isStarting()) {
        throw new AssertionError("Notify was called before the LockManager was started.");
      } else {
        logger.warn("Notify was called after shutdown sequence commenced.");
      }
    }
    Lock lock = (Lock) this.locks.get(lid);
    if (lock != null) {
      ServerThreadContext threadContext = threadContextFactory.getOrCreate(cid, tid);
      try {
        lock.notify(threadContext, all, addNotifiedWaitersTo);
        if (false) System.err.println("LockManager.notify(" + lid + ", " + cid + ", " + tid + ", all=" + all
                                      + ", notifiedWaiters=" + addNotifiedWaitersTo);
      } catch (TCIllegalMonitorStateException e) {
        e.printStackTrace();
        throw new AssertionError(e);
      }
    } else {
      throw new AssertionError("Lock :" + lid + " is not present !");
    }
  }

  private void basicUnlock(Lock lock, ServerThreadContext threadContext) {
    boolean wasUpgrade = lock.removeCurrentHold(threadContext);
    if (isStarted()) {
      if (wasUpgrade) {
        lock.awardAllReads();
      } else {
        boolean clear = lock.nextPending();
        if (clear) {
          locks.remove(lock.getLockID());
        }
      }
    }
    threadContextFactory.removeIfClear(threadContext);
    notifyAll();
  }

  public synchronized boolean hasPending(LockID id) {
    return getLockFor(id).hasPending();
  }

  public synchronized void clearAllLocksFor(ChannelID channelID) {
    HashSet allLocks = new HashSet(locks.keySet());

    // NOTE: These loops might be too expensive as lock and threadContext sets grow large
    // We could keep indexes based on channel to speed things up

    for (Iterator i = allLocks.iterator(); i.hasNext();) {
      LockID lid = (LockID) i.next();
      Lock lock = getLockFor(lid);

      if (!lock.isNull()) {
        lock.clearStateForChannel(channelID);

        // This gets the next pending lock (if any) awarded
        basicUnlock(lock, ServerThreadContext.NULL_CONTEXT);
      }
    }
    threadContextFactory.clear(channelID);
  }

  private Lock getLockFor(LockID id) {
    Lock lock = (Lock) locks.get(id);
    if (lock == null) return Lock.NULL_LOCK;
    return lock;
  }

  public synchronized void scanForDeadlocks(DeadlockResults output) {
    new DeadlockDetector(output).detect(threadContextFactory.getView().iterator());
  }

  public DeadlockChain[] scanForDeadlocks() {
    final List chains = new ArrayList();
    DeadlockResults results = new DeadlockResults() {
      public void foundDeadlock(DeadlockChain chain) {
        chains.add(chain);
      }
    };

    scanForDeadlocks(results);

    return (DeadlockChain[]) chains.toArray(new DeadlockChain[chains.size()]);
  }

  public LockMBean[] getAllLocks() {
    final List copy;
    synchronized (this) {
      copy = new ArrayList(locks.size());
      copy.addAll(locks.values());
    }

    int count = 0;
    LockMBean[] rv = new LockMBean[copy.size()];
    for (Iterator i = copy.iterator(); i.hasNext();) {
      Lock lock = (Lock) i.next();
      rv[count++] = lock.getMBean(channelManager);
    }

    return rv;
  }

  public void start() {
    synchronized (this) {
      assertStarting();
      changeState(STARTED);
      if (lockPolicy == UNINITIALIZED_LOCK_POLICY) {
        this.lockPolicy = GREEDY_LOCK_POLICY;
      }

      logger.debug("START Locks re-established -- " + locks.size());
      for (Iterator i = locks.values().iterator(); i.hasNext();) {
        Lock lock = (Lock) i.next();
        lock.setLockPolicy(lockPolicy);
        lock.notifyStarted(this, waitTimer);
      }

      for (Iterator i = lockRequestQueue.iterator(); i.hasNext();) {
        RequestLockContext ctxt = (RequestLockContext) i.next();
        requestLock(ctxt.lockID, ctxt.channelID, ctxt.threadID, ctxt.requestedLockLevel, ctxt.lockResponseSink,
                    ctxt.noBlock);
      }
      lockRequestQueue.clear();
    }
  }

  public synchronized void stop() throws InterruptedException {
    while (isStarting())
      wait();
    assertStarted();
    cinfo("Stopping...");
    changeState(STOPPING);

    locks.clear();
    threadContextFactory.clear();

    if (waitTimer != null) {
      waitTimer.shutdown();
    }
    setLockPolicy(ALTRUISTIC_LOCK_POLICY);

    changeState(STOPPED);
    cinfo("Stopped.");

  }

  public int getLockPolicy() {
    return lockPolicy;
  }

  public void setLockPolicy(int lockPolicy) {
    Assert.assertTrue(lockPolicy == GREEDY_LOCK_POLICY || lockPolicy == ALTRUISTIC_LOCK_POLICY);
    this.lockPolicy = lockPolicy;
    for (Iterator i = locks.values().iterator(); i.hasNext();) {
      Lock lock = (Lock) i.next();
      lock.setLockPolicy(this.lockPolicy);
    }
  }

  private void cinfo(Object message) {
    clogger.debug("Lock Manager: " + message);
  }

  private void changeState(State s) {
    this.status = s;
    notifyAll();
  }

  private boolean isStopped() {
    return status == STOPPED;
  }

  private boolean isStarted() {
    return status == STARTED;
  }

  private boolean isStarting() {
    return status == STARTING;
  }

  private void assertStarting() {
    if (!isStarting()) throw new LockManagerError(NOT_STARTING_ERROR, "LockManager is not starting ("
                                                                      + this.status.getName() + ")");
  }

  private void assertStarted() {
    if (!isStarted()) throw new LockManagerError(NOT_STARTED_ERROR, "LockManager is not started ("
                                                                    + this.status.getName() + ")");
  }

  private void assertNotStarting() {
    if (isStarting()) throw new LockManagerError(IS_STARTING_ERROR, "LockManager is starting");
  }

  private void assertNotStopped() {
    if (isStopped()) throw new LockManagerError(IS_STOPPED_ERROR, "LockManager is stopped");
  }

  public static class LockManagerError extends Error {

    private final LockManagerErrorDescription desc;

    private LockManagerError(LockManagerErrorDescription desc, String msg) {
      super(msg);
      this.desc = desc;
    }

    public LockManagerErrorDescription getDescription() {
      return this.desc;
    }
  }

  public static class LockManagerErrorDescription {
    private final String name;

    private LockManagerErrorDescription(String name) {
      this.name = name;
    }

    public String toString() {
      return getClass().getName() + "[" + this.name + "]";
    }
  }

  private static class RequestLockContext {
    final LockID    lockID;
    final ChannelID channelID;
    final ThreadID  threadID;
    final int       requestedLockLevel;
    final boolean   noBlock;
    final Sink      lockResponseSink;

    private RequestLockContext(LockID lockID, ChannelID channelID, ThreadID threadID, int requestedLockLevel,
                               Sink lockResponseSink, boolean noBlock) {
      this.lockID = lockID;
      this.channelID = channelID;
      this.threadID = threadID;
      this.requestedLockLevel = requestedLockLevel;
      this.lockResponseSink = lockResponseSink;
      this.noBlock = noBlock;
    }

    public String toString() {
      return "RequestLockContext [ " + lockID + "," + channelID + "," + threadID + ","
             + LockLevel.toString(requestedLockLevel) + ", " + noBlock + " ]";
    }
  }

  private static class State {
    private final String name;

    private State(String name) {
      this.name = name;
    }

    public String getName() {
      return this.name;
    }

    public String toString() {
      return getClass().getName() + "[" + this.name + "]";
    }
  }

}
