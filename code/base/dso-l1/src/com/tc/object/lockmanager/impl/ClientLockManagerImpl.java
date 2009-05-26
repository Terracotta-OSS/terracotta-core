/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object.lockmanager.impl;

import org.apache.commons.collections.map.ListOrderedMap;

import com.tc.logging.TCLogger;
import com.tc.management.ClientLockStatManager;
import com.tc.net.GroupID;
import com.tc.net.NodeID;
import com.tc.net.OrderedGroupIDs;
import com.tc.object.lockmanager.api.ClientLockManager;
import com.tc.object.lockmanager.api.ClientLockManagerConfig;
import com.tc.object.lockmanager.api.LockContext;
import com.tc.object.lockmanager.api.LockFlushCallback;
import com.tc.object.lockmanager.api.LockID;
import com.tc.object.lockmanager.api.LockLevel;
import com.tc.object.lockmanager.api.LockRequest;
import com.tc.object.lockmanager.api.Notify;
import com.tc.object.lockmanager.api.QueryLockRequest;
import com.tc.object.lockmanager.api.RemoteLockManager;
import com.tc.object.lockmanager.api.TCLockTimer;
import com.tc.object.lockmanager.api.ThreadID;
import com.tc.object.lockmanager.api.TryLockContext;
import com.tc.object.lockmanager.api.TryLockRequest;
import com.tc.object.lockmanager.api.WaitContext;
import com.tc.object.lockmanager.api.WaitListener;
import com.tc.object.lockmanager.api.WaitLockRequest;
import com.tc.object.msg.ClientHandshakeMessage;
import com.tc.object.session.SessionID;
import com.tc.object.session.SessionManager;
import com.tc.object.tx.TimerSpec;
import com.tc.text.ConsoleParagraphFormatter;
import com.tc.text.ParagraphFormatter;
import com.tc.text.PrettyPrinter;
import com.tc.text.PrettyPrinterImpl;
import com.tc.text.StringFormatter;
import com.tc.util.Assert;
import com.tc.util.State;
import com.tc.util.Util;
import com.tc.util.runtime.LockInfoByThreadID;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.TimerTask;

/**
 * The Top level lock manager and entry point into the lock manager subsystem in the L1.
 */
public class ClientLockManagerImpl implements ClientLockManager, LockFlushCallback {

  private static final int               INIT_LOCK_MAP_SIZE           = 10000;

  private static final State             RUNNING                      = new State("RUNNING");
  private static final State             STARTING                     = new State("STARTING");
  private static final State             PAUSED                       = new State("PAUSED");

  private static final String            MISSING_LOCK_TEXT            = makeMissingLockText();

  private final Map<NodeID, State>       grpToState                   = new HashMap<NodeID, State>();
  private final Map                      pendingQueryLockRequestsByID = new ListOrderedMap();
  private final Map                      lockInfoByID                 = new HashMap();
  private final RemoteLockManager        remoteLockManager;
  private final Map<LockID, ClientLock>  locksByID                    = new HashMap(INIT_LOCK_MAP_SIZE);

  // This is specifically insertion ordered so that locks that are likely to be garbage are in the front, added first
  private final LinkedHashSet            gcCandidates                 = new LinkedHashSet();
  private final TCLogger                 logger;
  private final SessionManager           sessionManager;
  private final ClientLockStatManager    lockStatManager;
  private final ClientLockManagerConfig  clientLockManagerConfig;
  private final TCLockTimer              waitTimer;
  private final LockDistributionStrategy lockDistributionStrategy;

  // For tests
  public ClientLockManagerImpl(final LockDistributionStrategy strategy, OrderedGroupIDs groupIds,
                               final TCLogger logger, final RemoteLockManager remoteLockManager,
                               final SessionManager sessionManager, final ClientLockStatManager lockStatManager,
                               final ClientLockManagerConfig clientLockManagerConfig) {
    this(strategy, groupIds, logger, remoteLockManager, sessionManager, lockStatManager, clientLockManagerConfig,
         new TCLockTimerImpl());
  }

  public ClientLockManagerImpl(final LockDistributionStrategy strategy, OrderedGroupIDs groupIds,
                               final TCLogger logger, final RemoteLockManager remoteLockManager,
                               final SessionManager sessionManager, final ClientLockStatManager lockStatManager,
                               final ClientLockManagerConfig clientLockManagerConfig, final TCLockTimer waitTimer) {
    this.logger = logger;
    this.remoteLockManager = remoteLockManager;
    this.sessionManager = sessionManager;
    this.lockStatManager = lockStatManager;
    this.clientLockManagerConfig = clientLockManagerConfig;
    this.waitTimer = waitTimer;
    this.waitTimer.getTimer().schedule(new LockGCTask(this), clientLockManagerConfig.getTimeoutInterval(),
                                       clientLockManagerConfig.getTimeoutInterval());
    this.lockDistributionStrategy = strategy;
    for (int i = 0, len = groupIds.length(); i < len; i++) {
      grpToState.put(groupIds.getGroup(i), RUNNING);
    }
  }

  // for testing
  public synchronized int getLocksByIDSize() {
    return this.locksByID.size();
  }

  public synchronized void pause(final NodeID remote, final int disconnected) {
    if (isState(PAUSED, remote)) { throw new AssertionError("Attempt to pause while already paused : " + grpToState); }
    setStateForNodeID(remote, PAUSED);
    for (Iterator iter = new HashSet(this.locksByID.values()).iterator(); iter.hasNext();) {
      ClientLock lock = (ClientLock) iter.next();
      GroupID grpId = lockDistributionStrategy.getGroupIdForLock(lock.getLockID().asString());
      if (grpId.equals(remote) || remote.equals(GroupID.ALL_GROUPS)) {
        lock.pause();
      }
    }
  }

  private boolean areAllState(State state) {
    for (Iterator<Map.Entry<NodeID, State>> iter = grpToState.entrySet().iterator(); iter.hasNext();) {
      State stateEntry = iter.next().getValue();
      if (stateEntry != state) return false;

    }
    return true;
  }

  private boolean isState(State state, NodeID remote) {
    if (GroupID.ALL_GROUPS.equals(remote)) { return areAllState(state); }

    return grpToState.get(remote) == state;
  }

  private void setStateForNodeID(final NodeID remoteNode, State state) {
    if (GroupID.ALL_GROUPS.equals(remoteNode)) {
      NodeID[] nodeids = grpToState.keySet().toArray(new NodeID[grpToState.size()]);
      for (int i = 0; i < nodeids.length; i++) {
        grpToState.put(nodeids[i], state);
      }
    } else {
      grpToState.put(remoteNode, state);
    }
  }

  public synchronized void unpause(final NodeID remote, final int disconnected) {
    if (!isState(STARTING, remote)) { throw new AssertionError("Attempt to unpause when not in starting : "
                                                               + grpToState); }
    setStateForNodeID(remote, RUNNING);
    notifyAll();
    for (Iterator iter = this.locksByID.values().iterator(); iter.hasNext();) {
      ClientLock lock = (ClientLock) iter.next();
      GroupID grpId = lockDistributionStrategy.getGroupIdForLock(lock.getLockID().asString());
      if (grpId.equals(remote) || remote.equals(GroupID.ALL_GROUPS)) {
        lock.unpause();
      }
    }
    resubmitQueryLockRequests();
  }

  public synchronized void initializeHandshake(final NodeID thisNode, final NodeID remoteNode,
                                               final ClientHandshakeMessage handshakeMessage) {
    if (!isState(PAUSED, remoteNode)) { throw new AssertionError("Attempt to initiateHandshake when not paused: "
                                                                 + grpToState); }
    setStateForNodeID(remoteNode, STARTING);

    for (Iterator i = addAllHeldLocksTo(new HashSet(), remoteNode).iterator(); i.hasNext();) {
      LockRequest request = (LockRequest) i.next();
      LockContext ctxt = new LockContext(request.lockID(), thisNode, request.threadID(), request.lockLevel(), request
          .lockType());
      handshakeMessage.addLockContext(ctxt);
    }

    for (Iterator i = addAllWaitersTo(new HashSet(), remoteNode).iterator(); i.hasNext();) {
      WaitLockRequest request = (WaitLockRequest) i.next();
      WaitContext ctxt = new WaitContext(request.lockID(), thisNode, request.threadID(), request.lockLevel(), request
          .lockType(), request.getTimerSpec());
      handshakeMessage.addWaitContext(ctxt);
    }

    for (Iterator i = addAllPendingLockRequestsTo(new HashSet(), remoteNode).iterator(); i.hasNext();) {
      LockRequest request = (LockRequest) i.next();
      LockContext ctxt = new LockContext(request.lockID(), thisNode, request.threadID(), request.lockLevel(), request
          .lockType());
      handshakeMessage.addPendingLockContext(ctxt);
    }

    for (Iterator i = addAllPendingTryLockRequestsTo(new HashSet(), remoteNode).iterator(); i.hasNext();) {
      TryLockRequest request = (TryLockRequest) i.next();
      TryLockContext ctxt = new TryLockContext(request.lockID(), thisNode, request.threadID(), request.lockLevel(),
                                               request.lockType(), request.getTimerSpec());
      handshakeMessage.addPendingTryLockContext(ctxt);
    }
  }

  public synchronized void runGC() {
    waitUntilRunning();

    long runGCStartTime = System.currentTimeMillis();
    if (this.logger.isDebugEnabled()) {
      this.logger.debug("Running Lock GC:  Total Locks  = " + this.locksByID.size() + " GC Candidates = "
                        + this.gcCandidates.size());
    }

    long findLocksToGC = System.currentTimeMillis();
    boolean continueGC = true;
    int totalGCCount = 0, iterationCount = 0;
    long timeOutMillis = this.clientLockManagerConfig.getTimeoutInterval();

    while (continueGC) {
      iterationCount++;
      ArrayList toGC = new ArrayList(1000);

      int k = 0;
      Iterator iter;
      for (iter = this.gcCandidates.iterator(); iter.hasNext() && k < 1000; k++) {
        LockID lockID = (LockID) iter.next();
        ClientLock lock = this.locksByID.get(lockID);
        if (lock.timedout(timeOutMillis)) {
          toGC.add(lockID);
        } else {
          // since this lock is not timed out other locks that we haven't processed yet couldn't possibly be timed out.
          continueGC = false;
          break;
        }
      }
      if (!iter.hasNext()) {
        continueGC = false;
      }

      if (this.logger.isDebugEnabled()) {
        this.logger.debug(" finding locks to GC took : ( " + (System.currentTimeMillis() - findLocksToGC)
                          + " )  ms : gcCandidates = " + this.gcCandidates.size() + " toGC = " + toGC.size());
      }

      if (toGC.size() > 0) {
        long recallingLocks = System.currentTimeMillis();
        if (this.logger.isDebugEnabled()) {
          this.logger.debug("GCing " + toGC.size() + " Locks ... out of " + this.locksByID.size());
        }

        for (Iterator recallIter = toGC.iterator(); recallIter.hasNext();) {
          LockID lockID = (LockID) recallIter.next();
          recall(lockID, ThreadID.VM_ID, LockLevel.WRITE, -1);
        }
        totalGCCount += toGC.size();

        if (this.logger.isDebugEnabled()) {
          this.logger.debug(" recalling " + toGC.size() + " locks took : ( "
                            + (System.currentTimeMillis() - recallingLocks) + " )  ms ");
        }
        if (continueGC) {
          // sleep every 1000th recall
          if (this.logger.isDebugEnabled()) {
            this.logger.debug("sleeping every 1000th recall in runGC()");
          }
          try {
            wait(1000);
          } catch (InterruptedException e) {
            throw new AssertionError(e);
          }
        }
      }
    }
    long timetaken = System.currentTimeMillis() - runGCStartTime;
    if (timetaken > 500) {
      this.logger.info("Running lock GC took " + timetaken + " ms " + iterationCount + " iterations for GCing "
                       + totalGCCount + " locks. gcCandidates remaining = " + this.gcCandidates.size()
                       + " total locks remaining = " + this.locksByID.size());
    }
  }

  private GlobalLockInfo getLockInfo(final LockID lockID, final ThreadID threadID) {
    Object waitLock = addToPendingQueryLockRequest(lockID, threadID);
    this.remoteLockManager.queryLock(lockID, threadID);
    waitForQueryReply(threadID, waitLock);
    GlobalLockInfo lockInfo;
    synchronized (this.lockInfoByID) {
      lockInfo = (GlobalLockInfo) this.lockInfoByID.remove(threadID);
    }
    return lockInfo;
  }

  // TODO:
  // Needs to take care of the greedy lock case.
  public int queueLength(final LockID lockID, final ThreadID threadID) {
    ClientLock lock;
    synchronized (this) {
      waitUntilRunning();
      lock = getLock(lockID);
    }
    GlobalLockInfo lockInfo = getLockInfo(lockID, threadID);

    int queueLength = lockInfo.getLockRequestQueueLength();
    if (lock != null) {
      queueLength += lock.queueLength();
    }
    return queueLength;
  }

  // TODO:
  // Needs to take care of the greedy lock case.
  public int waitLength(final LockID lockID, final ThreadID threadID) {
    ClientLock lock;
    synchronized (this) {
      waitUntilRunning();
      lock = getLock(lockID);
    }

    GlobalLockInfo lockInfo = getLockInfo(lockID, threadID);
    int waitLength = lockInfo.getWaitersInfo().size();

    if (lock != null) { return waitLength + lock.waitLength(); }

    return waitLength;
  }

  // This methods return the number of times a lock is being locked by threadID.
  public int localHeldCount(final LockID lockID, final int lockLevel, final ThreadID threadID) {
    ClientLock lock;
    synchronized (this) {
      waitUntilRunning();
      lock = this.locksByID.get(lockID);
    }
    if (lock == null) {
      return 0;
    } else {
      return lock.localHeldCount(threadID, lockLevel);
    }
  }

  // TODO:
  // Needs to take care of the greedy lock case.
  public boolean isLocked(final LockID lockID, final ThreadID threadID, final int lockLevel) {
    ClientLock lock;
    synchronized (this) {
      waitUntilRunning();
      lock = this.locksByID.get(lockID);
    }
    if (lock != null) {
      return lock.isHeldBy(threadID, lockLevel);
    } else {
      GlobalLockInfo lockInfo = getLockInfo(lockID, threadID);
      return lockInfo.isLocked(lockLevel);
    }
  }

  private void waitForQueryReply(final ThreadID threadID, final Object waitLock) {
    boolean isInterrupted = false;

    synchronized (waitLock) {
      while (!hasLockInfo(threadID)) {
        try {
          waitLock.wait();
        } catch (InterruptedException ioe) {
          isInterrupted = true;
        }
      }
    }
    Util.selfInterruptIfNeeded(isInterrupted);
  }

  private boolean hasLockInfo(final ThreadID threadID) {
    synchronized (this.lockInfoByID) {
      return this.lockInfoByID.containsKey(threadID);
    }
  }

  public void lock(final LockID lockID, final ThreadID threadID, final int lockType, final String lockObjectType,
                   final String contextInfo) {
    Assert.assertNotNull("threadID", threadID);
    final ClientLock lock;

    synchronized (this) {
      waitUntilRunning();
      lock = getOrCreateLock(lockID, lockObjectType);
      incrementUseCount(lock);
    }
    lock.lock(threadID, lockType, contextInfo);
  }

  private void incrementUseCount(final ClientLock lock) {
    int useCount = lock.incUseCount();
    if (useCount == 1) {
      this.gcCandidates.remove(lock.getLockID());
    }
  }

  private void decrementUseCount(final ClientLock lock) {
    int useCount = lock.decUseCount();
    if ((useCount == 0) && (!lock.pinned())) {
      this.gcCandidates.add(lock.getLockID());
    }
  }

  public void lockInterruptibly(final LockID lockID, final ThreadID threadID, final int lockType,
                                final String lockObjectType, final String contextInfo) throws InterruptedException {
    Assert.assertNotNull("threadID", threadID);
    final ClientLock lock;

    synchronized (this) {
      waitUntilRunning();
      lock = getOrCreateLock(lockID, lockObjectType);
      incrementUseCount(lock);
    }

    try {
      lock.lockInterruptibly(threadID, lockType, contextInfo);
    } catch (InterruptedException e) {
      synchronized (this) {
        decrementUseCount(lock);
        cleanUp(lock);
      }
      throw e;
    }
  }

  public boolean tryLock(final LockID lockID, final ThreadID threadID, final TimerSpec timeout, final int lockType,
                         final String lockObjectType) {
    Assert.assertNotNull("threadID", threadID);
    final ClientLock lock;

    synchronized (this) {
      waitUntilRunning();
      lock = getOrCreateLock(lockID, lockObjectType);
      incrementUseCount(lock);
    }
    boolean isLocked = lock.tryLock(threadID, timeout, lockType);
    if (!isLocked) {
      synchronized (this) {
        decrementUseCount(lock);
        // cleanup is anyway synchronized on this.
        cleanUp(lock);
      }
    }
    return isLocked;
  }

  public void unlock(final LockID lockID, final ThreadID threadID) {
    final ClientLock lock;

    synchronized (this) {
      waitUntilRunning();
      lock = this.locksByID.get(lockID);
      if (lock == null) { throw missingLockException(lockID); }
      decrementUseCount(lock);
    }

    lock.unlock(threadID);
    cleanUp(lock);
  }

  private AssertionError missingLockException(final LockID lockID) {
    return new AssertionError(MISSING_LOCK_TEXT + " Missing lock ID is " + lockID);
  }

  public void wait(final LockID lockID, final ThreadID threadID, final TimerSpec call, final Object waitLock,
                   final WaitListener listener) throws InterruptedException {
    final ClientLock myLock;
    synchronized (this) {
      waitUntilRunning();
      myLock = this.locksByID.get(lockID);
    }
    if (myLock == null) { throw missingLockException(lockID); }
    myLock.wait(threadID, call, waitLock, listener);
  }

  public Notify notify(final LockID lockID, final ThreadID threadID, final boolean all) {
    final ClientLock myLock;
    synchronized (this) {
      waitUntilRunning();
      myLock = this.locksByID.get(lockID);
    }
    if (myLock == null) { throw missingLockException(lockID); }
    return myLock.notify(threadID, all);
  }

  /*
   * The level represents the reason why server wanted a recall and will determine when a recall commit will happen.
   */
  public synchronized void recall(final LockID lockID, final ThreadID threadID, final int interestedLevel,
                                  final int leaseTimeInMs) {
    Assert.assertEquals(ThreadID.VM_ID, threadID);
    if (isPaused()) {
      this.logger.warn("Ignoring recall request from dead server : " + lockID + ", " + threadID + " interestedLevel : "
                       + LockLevel.toString(interestedLevel));
      return;
    }
    final ClientLock myLock = this.locksByID.get(lockID);
    if (myLock != null) {
      if (leaseTimeInMs > 0) {
        myLock.recall(interestedLevel, this, leaseTimeInMs);
      } else {
        myLock.recall(interestedLevel, this);
      }
      cleanUp(myLock);
    }
  }

  public void transactionsForLockFlushed(final LockID lockID) {
    final ClientLock myLock;
    synchronized (this) {
      waitUntilRunning();
      myLock = this.locksByID.get(lockID);
    }
    if (myLock != null) {
      myLock.transactionsForLockFlushed(lockID);
      cleanUp(myLock);
    }
  }

  /*
   * Called from a stage thread and should never be blocked XXX:: I am currently not ignoring reponses from dead server
   * because of a bug during server restart case. check out https://jira.terracotta.org/jira/browse/DEV-448 . After
   * fixing that, one can ignore responses while in paused state.
   */
  public synchronized void queryLockCommit(final ThreadID threadID, final GlobalLockInfo globalLockInfo) {
    synchronized (this.lockInfoByID) {
      this.lockInfoByID.put(threadID, globalLockInfo);
    }
    QueryLockRequest qRequest = (QueryLockRequest) this.pendingQueryLockRequestsByID.remove(threadID);
    if (qRequest == null) { throw new AssertionError("Query Lock request does not exist."); }
    Object waitLock = qRequest.getWaitLock();
    synchronized (waitLock) {
      waitLock.notifyAll();
    }
  }

  public synchronized void waitTimedOut(final LockID lockID, final ThreadID threadID) {
    notified(lockID, threadID);
  }

  private synchronized void cleanUp(final ClientLock lock) {
    if (lock.isClear()) {
      Object o = this.locksByID.get(lock.getLockID());
      if (o == lock) {
        // Sometimes when called from recall, the unlock would have already removed this lock
        // from the map and a new lock could be in the map from a new lock request. We dont want to
        // remove that
        this.locksByID.remove(lock.getLockID());
        this.gcCandidates.remove(lock.getLockID());
      }
    }
  }

  /*
   * Called from a stage thread and should never be blocked
   */
  public synchronized void notified(final LockID lockID, final ThreadID threadID) {
    if (isPaused()) {
      this.logger.warn("Ignoring notified call from dead server : " + lockID + ", " + threadID);
      return;
    }
    final ClientLock myLock = this.locksByID.get(lockID);
    if (myLock == null) { throw new AssertionError(lockID.toString()); }
    myLock.notified(threadID);
  }

  /*
   * XXX::This method is called from a stage thread. It operate on the lock inside the scope of the synchronization
   * unlike other methods because, we want to decide whether to process this award or not and go with it atomically
   */
  public synchronized void awardLock(final NodeID nid, final SessionID sessionID, final LockID lockID,
                                     final ThreadID threadID, final int level) {
    if (isPaused() || !this.sessionManager.isCurrentSession(nid, sessionID)) {
      this.logger.warn("Ignoring lock award from a dead server :" + sessionID + ", " + this.sessionManager + " : "
                       + lockID + " " + threadID + " " + LockLevel.toString(level) + " state = " + grpToState);
      return;
    }
    final ClientLock lock = this.locksByID.get(lockID);
    if (lock != null) {
      lock.awardLock(threadID, level);
    } else if (LockLevel.isGreedy(level)) {
      getOrCreateLock(lockID, MISSING_LOCK_TEXT).awardLock(threadID, level);
    } else {
      this.remoteLockManager.releaseLock(lockID, threadID);
    }
  }

  /*
   * XXX:: @read comment for awardLock();
   */
  public synchronized void cannotAwardLock(final NodeID nid, final SessionID sessionID, final LockID lockID,
                                           final ThreadID threadID, final int level) {
    if (isPaused() || !this.sessionManager.isCurrentSession(nid, sessionID)) {
      this.logger.warn("Ignoring lock award from a dead server :" + sessionID + ", " + this.sessionManager + " : "
                       + lockID + " " + threadID + " level = " + level + " state = " + grpToState);
      return;
    }
    final ClientLock lock = this.locksByID.get(lockID);
    if (lock != null) {
      lock.cannotAwardLock(threadID, level);
    }
  }

  public synchronized void pinLock(final LockID lockId) {
    ClientLock lock = getOrCreateLock(lockId, MISSING_LOCK_TEXT);
    lock.pin();
  }

  public synchronized void unpinLock(final LockID lockId) {
    ClientLock lock = locksByID.get(lockId);
    if (lock != null && lock.unpin()) {
      gcCandidates.add(lockId);
      cleanUp(lock);
    }
  }

  public synchronized void evictLock(final LockID lockId) {
    ClientLock lock = locksByID.get(lockId);
    if (lock != null) {
      if (lock.unpin()) {
        if (lock.isEvictable()) {
          recall(lockId, ThreadID.VM_ID, LockLevel.WRITE, -1);
        } else {
          gcCandidates.add(lockId);
          cleanUp(lock);
        }
      }
    }
  }

  // This method should be called within a synchronized(this) block.
  private ClientLock getLock(final LockID id) {
    return this.locksByID.get(id);
  }

  private synchronized ClientLock getOrCreateLock(final LockID id, final String lockObjectType) {

    ClientLock lock = this.locksByID.get(id);
    if (lock == null) {
      lock = new ClientLock(id, lockObjectType, this.remoteLockManager, this.waitTimer, this.lockStatManager);
      this.locksByID.put(id, lock);
    }
    return lock;
  }

  public LockID lockIDFor(final String id) {
    if (id == null) { return LockID.NULL_ID; }
    return new LockID(id);
  }

  public synchronized Collection addAllWaitersTo(final Collection c, NodeID nodeID) {
    for (Iterator i = this.locksByID.values().iterator(); i.hasNext();) {
      ClientLock lock = (ClientLock) i.next();
      GroupID grpId = lockDistributionStrategy.getGroupIdForLock(lock.getLockID().asString());
      if (grpId.equals(nodeID) || nodeID.equals(GroupID.ALL_GROUPS)) {
        lock.addAllWaitersTo(c);
      }
    }
    return c;
  }

  synchronized Collection addAllHeldLocksTo(final Collection c, NodeID nodeID) {
    for (Iterator i = this.locksByID.values().iterator(); i.hasNext();) {
      ClientLock lock = (ClientLock) i.next();
      GroupID grpId = lockDistributionStrategy.getGroupIdForLock(lock.getLockID().asString());
      if (grpId.equals(nodeID) || nodeID.equals(GroupID.ALL_GROUPS)) {
        lock.addHoldersToAsLockRequests(c);
      }
    }
    return c;
  }

  synchronized Collection addAllPendingLockRequestsTo(final Collection c, NodeID nodeID) {
    for (Iterator i = this.locksByID.values().iterator(); i.hasNext();) {
      ClientLock lock = (ClientLock) i.next();
      GroupID grpId = lockDistributionStrategy.getGroupIdForLock(lock.getLockID().asString());
      if (grpId.equals(nodeID) || nodeID.equals(GroupID.ALL_GROUPS)) {
        lock.addAllPendingLockRequestsTo(c);
      }
    }
    return c;
  }

  synchronized Collection addAllPendingTryLockRequestsTo(final Collection c, NodeID nodeID) {
    for (Iterator i = this.locksByID.values().iterator(); i.hasNext();) {
      ClientLock lock = (ClientLock) i.next();
      GroupID grpId = lockDistributionStrategy.getGroupIdForLock(lock.getLockID().asString());
      if (grpId.equals(nodeID) || nodeID.equals(GroupID.ALL_GROUPS)) {
        lock.addAllPendingTryLockRequestsTo(c);
      }
    }
    return c;
  }

  public synchronized void addAllLocksTo(final LockInfoByThreadID lockInfo) {
    for (Iterator i = this.locksByID.values().iterator(); i.hasNext();) {
      ClientLock lock = (ClientLock) i.next();
      lock.addAllLocksTo(lockInfo);
    }
  }

  public synchronized void setLockStatisticsConfig(final int traceDepth, final int gatherInterval) {
    waitUntilRunning();
    this.lockStatManager.setLockStatisticsConfig(traceDepth, gatherInterval);
  }

  public synchronized void setLockStatisticsEnabled(final boolean statEnable) {
    waitUntilRunning();
    this.lockStatManager.setLockStatisticsEnabled(statEnable);
  }

  public synchronized void requestLockSpecs(NodeID nodeID) {
    waitUntilRunning();
    this.lockStatManager.requestLockSpecs(nodeID, lockDistributionStrategy);
  }

  synchronized boolean haveLock(final LockID lockID, final ThreadID threadID, final int lockType) {
    ClientLock l = this.locksByID.get(lockID);
    if (l == null) { return false; }
    return l.isHeldBy(threadID, lockType);
  }

  private void waitUntilRunning() {
    boolean isInterrupted = false;
    while (!isRunning()) {
      try {
        wait();
      } catch (InterruptedException e) {
        isInterrupted = true;
      }
    }
    Util.selfInterruptIfNeeded(isInterrupted);
  }

  public synchronized boolean isRunning() {
    return areAllState(RUNNING);
  }

  public synchronized boolean isPaused() {
    return areAllState(PAUSED);
  }

  /*
   * @returns the wait object for lock request
   */
  private synchronized Object addToPendingQueryLockRequest(final LockID lockID, final ThreadID threadID) {
    // Add Lock Request
    Object o = new Object();
    QueryLockRequest qRequest = new QueryLockRequest(lockID, threadID, o);
    Object old = this.pendingQueryLockRequestsByID.put(threadID, qRequest);
    if (old != null) {
      // formatting
      throw new AssertionError("Query Lock request already outstanding - " + old);
    }

    return o;
  }

  private synchronized void resubmitQueryLockRequests() {
    for (Iterator i = this.pendingQueryLockRequestsByID.values().iterator(); i.hasNext();) {
      QueryLockRequest qRequest = (QueryLockRequest) i.next();
      this.remoteLockManager.queryLock(qRequest.lockID(), qRequest.threadID());
    }
  }

  private static String makeMissingLockText() {
    ParagraphFormatter formatter = new ConsoleParagraphFormatter(72, new StringFormatter());

    String message = "An operation to a DSO lock was attempted for a lock that does not yet exist. This is usually the result ";
    message += "of an object becoming shared in the middle of synchronized block on that object (in which case the monitorExit ";
    message += "call will produce this exception). Additionally, attempts to wait()/notify()/notifyAll() on an object in such a block will ";
    message += "also fail. To workaround this problem, the object/lock need to become shared in the scope of a different (earlier) ";
    message += "synchronization block.";

    return formatter.format(message);
  }

  static class LockGCTask extends TimerTask {

    final ClientLockManagerImpl lockManager;

    LockGCTask(final ClientLockManagerImpl mgr) {
      this.lockManager = mgr;
    }

    @Override
    public void run() {
      this.lockManager.runGC();
    }
  }

  public synchronized String dump() {
    StringWriter writer = new StringWriter();
    PrintWriter pw = new PrintWriter(writer);
    new PrettyPrinterImpl(pw).visit(this);
    writer.flush();
    return writer.toString();
  }

  public void dumpToLogger() {
    this.logger.info(dump());
  }

  public synchronized PrettyPrinter prettyPrint(final PrettyPrinter out) {
    out.println(getClass().getName());
    out.indent().println("locks: " + this.locksByID.size());
    for (Iterator i = this.locksByID.values().iterator(); i.hasNext();) {
      out.println(i.next());
    }
    return out;
  }

}
