/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object.lockmanager.impl;

import org.apache.commons.collections.map.ListOrderedMap;

import com.tc.logging.TCLogger;
import com.tc.management.ClientLockStatManager;
import com.tc.net.NodeID;
import com.tc.object.handshakemanager.ClientHandshakeCallback;
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

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
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
public class ClientLockManagerImpl implements ClientLockManager, LockFlushCallback, ClientHandshakeCallback {

  private static final int              INIT_LOCK_MAP_SIZE           = 10000;

  private static final State            RUNNING                      = new State("RUNNING");
  private static final State            STARTING                     = new State("STARTING");
  private static final State            PAUSED                       = new State("PAUSED");

  private static final String           MISSING_LOCK_TEXT            = makeMissingLockText();

  private State                         state                        = RUNNING;
  private final Map                     pendingQueryLockRequestsByID = new ListOrderedMap();
  private final Map                     lockInfoByID                 = new HashMap();
  private final RemoteLockManager       remoteLockManager;
  private final Map                     locksByID                    = new HashMap(INIT_LOCK_MAP_SIZE);

  // This is specifically insertion ordered so that locks that are likely to be garbage are in the front, added first
  private final LinkedHashSet           gcCandidates                 = new LinkedHashSet();
  private final TCLogger                logger;
  private final SessionManager          sessionManager;
  private final ClientLockStatManager   lockStatManager;
  private final ClientLockManagerConfig clientLockManagerConfig;
  private final TCLockTimer             waitTimer;

  // For tests
  public ClientLockManagerImpl(TCLogger logger, RemoteLockManager remoteLockManager, SessionManager sessionManager,
                               ClientLockStatManager lockStatManager, ClientLockManagerConfig clientLockManagerConfig) {
    this(logger, remoteLockManager, sessionManager, lockStatManager, clientLockManagerConfig, new TCLockTimerImpl());

  }

  public ClientLockManagerImpl(TCLogger logger, RemoteLockManager remoteLockManager, SessionManager sessionManager,
                               ClientLockStatManager lockStatManager, ClientLockManagerConfig clientLockManagerConfig,
                               TCLockTimer waitTimer) {
    this.logger = logger;
    this.remoteLockManager = remoteLockManager;
    this.sessionManager = sessionManager;
    this.lockStatManager = lockStatManager;
    this.clientLockManagerConfig = clientLockManagerConfig;
    this.waitTimer = waitTimer;
    this.waitTimer.getTimer().schedule(new LockGCTask(this), clientLockManagerConfig.getTimeoutInterval(),
                                       clientLockManagerConfig.getTimeoutInterval());
  }

  // for testing
  public synchronized int getLocksByIDSize() {
    return this.locksByID.size();
  }

  public synchronized void pause(NodeID remote, int disconnected) {
    if (this.state == PAUSED) { throw new AssertionError("Attempt to pause while already paused : " + this.state); }
    this.state = PAUSED;
    for (Iterator iter = new HashSet(this.locksByID.values()).iterator(); iter.hasNext();) {
      ClientLock lock = (ClientLock) iter.next();
      lock.pause();
    }
  }

  public synchronized void unpause(NodeID remote, int disconnected) {
    if (this.state != STARTING) { throw new AssertionError("Attempt to unpause when not in starting : " + this.state); }
    this.state = RUNNING;
    notifyAll();
    for (Iterator iter = this.locksByID.values().iterator(); iter.hasNext();) {
      ClientLock lock = (ClientLock) iter.next();
      lock.unpause();
    }
    resubmitQueryLockRequests();
  }

  public synchronized void initializeHandshake(NodeID thisNode, NodeID remoteNode,
                                               ClientHandshakeMessage handshakeMessage) {
    if (this.state != PAUSED) { throw new AssertionError("Attempt to initiateHandshake when not paused: " + this.state); }
    this.state = STARTING;
    for (Iterator i = addAllHeldLocksTo(new HashSet()).iterator(); i.hasNext();) {
      LockRequest request = (LockRequest) i.next();
      LockContext ctxt = new LockContext(request.lockID(), thisNode, request.threadID(), request.lockLevel(), request
          .lockType());
      handshakeMessage.addLockContext(ctxt);
    }

    for (Iterator i = addAllWaitersTo(new HashSet()).iterator(); i.hasNext();) {
      WaitLockRequest request = (WaitLockRequest) i.next();
      WaitContext ctxt = new WaitContext(request.lockID(), thisNode, request.threadID(), request.lockLevel(), request
          .lockType(), request.getTimerSpec());
      handshakeMessage.addWaitContext(ctxt);
    }

    for (Iterator i = addAllPendingLockRequestsTo(new HashSet()).iterator(); i.hasNext();) {
      LockRequest request = (LockRequest) i.next();
      LockContext ctxt = new LockContext(request.lockID(), thisNode, request.threadID(), request.lockLevel(), request
          .lockType());
      handshakeMessage.addPendingLockContext(ctxt);
    }

    for (Iterator i = addAllPendingTryLockRequestsTo(new HashSet()).iterator(); i.hasNext();) {
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
        ClientLock lock = (ClientLock) this.locksByID.get(lockID);
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

  private GlobalLockInfo getLockInfo(LockID lockID, ThreadID threadID) {
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
  public int queueLength(LockID lockID, ThreadID threadID) {
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
  public int waitLength(LockID lockID, ThreadID threadID) {
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
  public int localHeldCount(LockID lockID, int lockLevel, ThreadID threadID) {
    ClientLock lock;
    synchronized (this) {
      waitUntilRunning();
      lock = (ClientLock) this.locksByID.get(lockID);
    }
    if (lock == null) {
      return 0;
    } else {
      return lock.localHeldCount(threadID, lockLevel);
    }
  }

  // TODO:
  // Needs to take care of the greedy lock case.
  public boolean isLocked(LockID lockID, ThreadID threadID, int lockLevel) {
    ClientLock lock;
    synchronized (this) {
      waitUntilRunning();
      lock = (ClientLock) this.locksByID.get(lockID);
    }
    if (lock != null) {
      return lock.isHeldBy(threadID, lockLevel);
    } else {
      GlobalLockInfo lockInfo = getLockInfo(lockID, threadID);
      return lockInfo.isLocked(lockLevel);
    }
  }

  private void waitForQueryReply(ThreadID threadID, Object waitLock) {
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

  private boolean hasLockInfo(ThreadID threadID) {
    synchronized (this.lockInfoByID) {
      return this.lockInfoByID.containsKey(threadID);
    }
  }

  public void lock(LockID lockID, ThreadID threadID, int lockType, String lockObjectType, String contextInfo) {
    Assert.assertNotNull("threadID", threadID);
    final ClientLock lock;

    synchronized (this) {
      waitUntilRunning();
      lock = getOrCreateLock(lockID, lockObjectType);
      incrementUseCount(lock);
    }
    lock.lock(threadID, lockType, contextInfo);
  }

  private void incrementUseCount(ClientLock lock) {
    int useCount = lock.incUseCount();
    if (useCount == 1) {
      this.gcCandidates.remove(lock.getLockID());
    }
  }

  private void decrementUseCount(ClientLock lock) {
    int useCount = lock.decUseCount();
    if (useCount == 0) {
      this.gcCandidates.add(lock.getLockID());
    }
  }

  public void lockInterruptibly(LockID lockID, ThreadID threadID, int lockType, String lockObjectType,
                                String contextInfo) throws InterruptedException {
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

  public boolean tryLock(LockID lockID, ThreadID threadID, TimerSpec timeout, int lockType, String lockObjectType) {
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

  public void unlock(LockID lockID, ThreadID threadID) {
    final ClientLock lock;

    synchronized (this) {
      waitUntilRunning();
      lock = (ClientLock) this.locksByID.get(lockID);
      if (lock == null) { throw missingLockException(lockID); }
      decrementUseCount(lock);
    }

    lock.unlock(threadID);
    cleanUp(lock);
  }

  private AssertionError missingLockException(LockID lockID) {
    return new AssertionError(MISSING_LOCK_TEXT + " Missing lock ID is " + lockID);
  }

  public void wait(LockID lockID, ThreadID threadID, TimerSpec call, Object waitLock, WaitListener listener)
      throws InterruptedException {
    final ClientLock myLock;
    synchronized (this) {
      waitUntilRunning();
      myLock = (ClientLock) this.locksByID.get(lockID);
    }
    if (myLock == null) { throw missingLockException(lockID); }
    myLock.wait(threadID, call, waitLock, listener);
  }

  public Notify notify(LockID lockID, ThreadID threadID, boolean all) {
    final ClientLock myLock;
    synchronized (this) {
      waitUntilRunning();
      myLock = (ClientLock) this.locksByID.get(lockID);
    }
    if (myLock == null) { throw missingLockException(lockID); }
    return myLock.notify(threadID, all);
  }

  /*
   * The level represents the reason why server wanted a recall and will determine when a recall commit will happen.
   */
  public synchronized void recall(LockID lockID, ThreadID threadID, int interestedLevel, int leaseTimeInMs) {
    Assert.assertEquals(ThreadID.VM_ID, threadID);
    if (isPaused()) {
      this.logger.warn("Ignoring recall request from dead server : " + lockID + ", " + threadID + " interestedLevel : "
                       + LockLevel.toString(interestedLevel));
      return;
    }
    final ClientLock myLock = (ClientLock) this.locksByID.get(lockID);
    if (myLock != null) {
      if (leaseTimeInMs > 0) {
        myLock.recall(interestedLevel, this, leaseTimeInMs);
      } else {
        myLock.recall(interestedLevel, this);
      }
      cleanUp(myLock);
    }
  }

  public void transactionsForLockFlushed(LockID lockID) {
    final ClientLock myLock;
    synchronized (this) {
      waitUntilRunning();
      myLock = (ClientLock) this.locksByID.get(lockID);
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
  public synchronized void queryLockCommit(ThreadID threadID, GlobalLockInfo globalLockInfo) {
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

  public synchronized void waitTimedOut(LockID lockID, ThreadID threadID) {
    notified(lockID, threadID);
  }

  private synchronized void cleanUp(ClientLock lock) {
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
  public synchronized void notified(LockID lockID, ThreadID threadID) {
    if (isPaused()) {
      this.logger.warn("Ignoring notified call from dead server : " + lockID + ", " + threadID);
      return;
    }
    final ClientLock myLock = (ClientLock) this.locksByID.get(lockID);
    if (myLock == null) { throw new AssertionError(lockID.toString()); }
    myLock.notified(threadID);
  }

  /*
   * XXX::This method is called from a stage thread. It operate on the lock inside the scope of the synchronization
   * unlike other methods because, we want to decide whether to process this award or not and go with it atomically
   */
  public synchronized void awardLock(NodeID nid, SessionID sessionID, LockID lockID, ThreadID threadID, int level) {
    if (isPaused() || !this.sessionManager.isCurrentSession(nid, sessionID)) {
      this.logger.warn("Ignoring lock award from a dead server :" + sessionID + ", " + this.sessionManager + " : "
                       + lockID + " " + threadID + " " + LockLevel.toString(level) + " state = " + this.state);
      return;
    }
    final ClientLock lock = (ClientLock) this.locksByID.get(lockID);
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
  public synchronized void cannotAwardLock(NodeID nid, SessionID sessionID, LockID lockID, ThreadID threadID, int level) {
    if (isPaused() || !this.sessionManager.isCurrentSession(nid, sessionID)) {
      this.logger.warn("Ignoring lock award from a dead server :" + sessionID + ", " + this.sessionManager + " : "
                       + lockID + " " + threadID + " level = " + level + " state = " + this.state);
      return;
    }
    final ClientLock lock = (ClientLock) this.locksByID.get(lockID);
    if (lock != null) {
      lock.cannotAwardLock(threadID, level);
    }
  }

  // This method should be called within a synchronized(this) block.
  private ClientLock getLock(LockID id) {
    return (ClientLock) this.locksByID.get(id);
  }

  private synchronized ClientLock getOrCreateLock(LockID id, String lockObjectType) {

    ClientLock lock = (ClientLock) this.locksByID.get(id);
    if (lock == null) {
      lock = new ClientLock(id, lockObjectType, this.remoteLockManager, this.waitTimer, this.lockStatManager);
      this.locksByID.put(id, lock);
    }
    return lock;
  }

  public LockID lockIDFor(String id) {
    if (id == null) { return LockID.NULL_ID; }
    return new LockID(id);
  }

  public synchronized Collection addAllWaitersTo(Collection c) {
    for (Iterator i = this.locksByID.values().iterator(); i.hasNext();) {
      ClientLock lock = (ClientLock) i.next();
      lock.addAllWaitersTo(c);
    }
    return c;
  }

  synchronized Collection addAllHeldLocksTo(Collection c) {
    for (Iterator i = this.locksByID.values().iterator(); i.hasNext();) {
      ClientLock lock = (ClientLock) i.next();
      lock.addHoldersToAsLockRequests(c);
    }
    return c;
  }

  synchronized Collection addAllPendingLockRequestsTo(Collection c) {
    for (Iterator i = this.locksByID.values().iterator(); i.hasNext();) {
      ClientLock lock = (ClientLock) i.next();
      lock.addAllPendingLockRequestsTo(c);
    }
    return c;
  }

  synchronized Collection addAllPendingTryLockRequestsTo(Collection c) {
    for (Iterator i = this.locksByID.values().iterator(); i.hasNext();) {
      ClientLock lock = (ClientLock) i.next();
      lock.addAllPendingTryLockRequestsTo(c);
    }
    return c;
  }

  public synchronized void addAllLocksTo(LockInfoByThreadID lockInfo) {
    for (Iterator i = this.locksByID.values().iterator(); i.hasNext();) {
      ClientLock lock = (ClientLock) i.next();
      lock.addAllLocksTo(lockInfo);
    }
  }

  public synchronized void setLockStatisticsConfig(int traceDepth, int gatherInterval) {
    waitUntilRunning();
    this.lockStatManager.setLockStatisticsConfig(traceDepth, gatherInterval);
  }

  public synchronized void setLockStatisticsEnabled(boolean statEnable) {
    waitUntilRunning();
    this.lockStatManager.setLockStatisticsEnabled(statEnable);
  }

  public synchronized void requestLockSpecs() {
    waitUntilRunning();
    this.lockStatManager.requestLockSpecs();
  }

  synchronized boolean haveLock(LockID lockID, ThreadID threadID, int lockType) {
    ClientLock l = (ClientLock) this.locksByID.get(lockID);
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
    return (this.state == RUNNING);
  }

  public synchronized boolean isPaused() {
    return (this.state == PAUSED);
  }

  /*
   * @returns the wait object for lock request
   */
  private synchronized Object addToPendingQueryLockRequest(LockID lockID, ThreadID threadID) {
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

    LockGCTask(ClientLockManagerImpl mgr) {
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

  public void dump(Writer writer) {
    try {
      writer.write(dump());
      writer.flush();
    } catch (IOException e) {
      throw new AssertionError(e);
    }
  }

  public void dumpToLogger() {
    this.logger.info(dump());
  }

  public synchronized PrettyPrinter prettyPrint(PrettyPrinter out) {
    out.println(getClass().getName());
    out.indent().println("locks: " + this.locksByID.size());
    for (Iterator i = this.locksByID.values().iterator(); i.hasNext();) {
      out.println(i.next());
    }
    return out;
  }

}
