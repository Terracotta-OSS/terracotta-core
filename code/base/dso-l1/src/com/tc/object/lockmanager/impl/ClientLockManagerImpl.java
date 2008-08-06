/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object.lockmanager.impl;

import org.apache.commons.collections.map.ListOrderedMap;

import com.tc.logging.TCLogger;
import com.tc.management.ClientLockStatManager;
import com.tc.object.bytecode.ManagerUtil;
import com.tc.object.lockmanager.api.ClientLockManager;
import com.tc.object.lockmanager.api.ClientLockManagerConfig;
import com.tc.object.lockmanager.api.LockFlushCallback;
import com.tc.object.lockmanager.api.LockID;
import com.tc.object.lockmanager.api.LockLevel;
import com.tc.object.lockmanager.api.Notify;
import com.tc.object.lockmanager.api.QueryLockRequest;
import com.tc.object.lockmanager.api.RemoteLockManager;
import com.tc.object.lockmanager.api.TCLockTimer;
import com.tc.object.lockmanager.api.ThreadID;
import com.tc.object.lockmanager.api.WaitListener;
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
public class ClientLockManagerImpl implements ClientLockManager, LockFlushCallback {

  private static final int              INIT_LOCK_MAP_SIZE           = 10000;

  private static final State            RUNNING                      = new State("RUNNING");
  private static final State            STARTING                     = new State("STARTING");
  private static final State            PAUSED                       = new State("PAUSED");

  private static final String           MISSING_LOCK_TEXT            = makeMissingLockText();

  private State                         state                        = RUNNING;
  private final Map                     pendingQueryLockRequestsByID = new ListOrderedMap();
  private final Map                     lockInfoByID                 = new HashMap();
  private final RemoteLockManager       remoteLockManager;
  private final TCLockTimer             waitTimer                    = new TCLockTimerImpl();
  private final Map                     locksByID                    = new HashMap(INIT_LOCK_MAP_SIZE);

  // This is specifically insertion ordered so that locks that are likely to be garbage are in the front, added first
  private final LinkedHashSet           gcCandidates                 = new LinkedHashSet();
  private final TCLogger                logger;
  private final SessionManager          sessionManager;
  private final ClientLockStatManager   lockStatManager;
  private final ClientLockManagerConfig clientLockManagerConfig;

  public ClientLockManagerImpl(TCLogger logger, RemoteLockManager remoteLockManager, SessionManager sessionManager,
                               ClientLockStatManager lockStatManager, ClientLockManagerConfig clientLockManagerConfig) {
    this.logger = logger;
    this.remoteLockManager = remoteLockManager;
    this.sessionManager = sessionManager;
    this.lockStatManager = lockStatManager;
    this.clientLockManagerConfig = clientLockManagerConfig;
    waitTimer.getTimer().schedule(new LockGCTask(this), clientLockManagerConfig.getTimeoutInterval(),
                                  clientLockManagerConfig.getTimeoutInterval());
  }

  // for testing
  public int getLocksByIDSize() {
    return locksByID.size();
  }

  public synchronized void pause() {
    if (state == PAUSED) throw new AssertionError("Attempt to pause while already paused : " + state);
    this.state = PAUSED;
    for (Iterator iter = new HashSet(locksByID.values()).iterator(); iter.hasNext();) {
      ClientLock lock = (ClientLock) iter.next();
      lock.pause();
    }
  }

  public synchronized void starting() {
    if (state != PAUSED) throw new AssertionError("Attempt to start when not paused: " + state);
    this.state = STARTING;
  }

  public synchronized void unpause() {
    if (state != STARTING) throw new AssertionError("Attempt to unpause when not starting: " + state);
    this.state = RUNNING;
    notifyAll();
    for (Iterator iter = locksByID.values().iterator(); iter.hasNext();) {
      ClientLock lock = (ClientLock) iter.next();
      lock.unpause();
    }
    resubmitQueryLockRequests();
  }

  public synchronized boolean isStarting() {
    return state == STARTING;
  }

  public synchronized void runGC() {
    waitUntilRunning();

    long runGCStartTime = System.currentTimeMillis();
    if (logger.isDebugEnabled()) {
      logger.debug("Running Lock GC:  Total Locks  = " + locksByID.size() + " GC Candidates = " + gcCandidates.size());
    }

    long findLocksToGC = System.currentTimeMillis();
    boolean continueGC = true;
    int totalGCCount = 0, iterationCount = 0;
    long timeOutMillis = clientLockManagerConfig.getTimeoutInterval();

    while (continueGC) {
      iterationCount++;
      ArrayList toGC = new ArrayList(1000);

      int k = 0;
      Iterator iter;
      for (iter = gcCandidates.iterator(); iter.hasNext() && k < 1000; k++) {
        LockID lockID = (LockID) iter.next();
        ClientLock lock = (ClientLock) locksByID.get(lockID);
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

      if (logger.isDebugEnabled()) {
        logger.debug(" finding locks to GC took : ( " + (System.currentTimeMillis() - findLocksToGC)
                     + " )  ms : gcCandidates = " + gcCandidates.size() + " toGC = " + toGC.size());
      }

      if (toGC.size() > 0) {
        long recallingLocks = System.currentTimeMillis();
        if (logger.isDebugEnabled()) {
          logger.debug("GCing " + toGC.size() + " Locks ... out of " + locksByID.size());
        }

        for (Iterator recallIter = toGC.iterator(); recallIter.hasNext();) {
          LockID lockID = (LockID) recallIter.next();
          recall(lockID, ThreadID.VM_ID, LockLevel.WRITE, -1);
        }
        totalGCCount += toGC.size();

        if (logger.isDebugEnabled()) {
          logger.debug(" recalling " + toGC.size() + " locks took : ( " + (System.currentTimeMillis() - recallingLocks)
                       + " )  ms ");
        }
        if (continueGC) {
          // sleep every 1000th recall
          if (logger.isDebugEnabled()) {
            logger.debug("sleeping every 1000th recall in runGC()");
          }
          try {
            wait(1000);
          } catch (InterruptedException e) {
            throw new AssertionError(e);
          }
        }
      }
    }
    logger.info("Running lock GC took " + (System.currentTimeMillis() - runGCStartTime) + " ms " + iterationCount
                + " iterations for GCing " + totalGCCount + " locks. gcCandidates remaining = " + gcCandidates.size()
                + " total locks remaining = " + locksByID.size());
  }

  private GlobalLockInfo getLockInfo(LockID lockID, ThreadID threadID) {
    Object waitLock = addToPendingQueryLockRequest(lockID, threadID);
    remoteLockManager.queryLock(lockID, threadID);
    waitForQueryReply(threadID, waitLock);
    GlobalLockInfo lockInfo;
    synchronized (lockInfoByID) {
      lockInfo = (GlobalLockInfo) lockInfoByID.remove(threadID);
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
      lock = (ClientLock) locksByID.get(lockID);
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
      lock = (ClientLock) locksByID.get(lockID);
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
    synchronized (lockInfoByID) {
      return lockInfoByID.containsKey(threadID);
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
      gcCandidates.remove(lock.getLockID());
    }
  }

  private void decrementUseCount(ClientLock lock) {
    int useCount = lock.decUseCount();
    if (useCount == 0) {
      gcCandidates.add(lock.getLockID());
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
      lock = (ClientLock) locksByID.get(lockID);
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
      myLock = (ClientLock) locksByID.get(lockID);
    }
    if (myLock == null) { throw missingLockException(lockID); }
    myLock.wait(threadID, call, waitLock, listener);
  }

  public Notify notify(LockID lockID, ThreadID threadID, boolean all) {
    final ClientLock myLock;
    synchronized (this) {
      waitUntilRunning();
      myLock = (ClientLock) locksByID.get(lockID);
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
      logger.warn("Ignoring recall request from dead server : " + lockID + ", " + threadID + " interestedLevel : "
                  + LockLevel.toString(interestedLevel));
      return;
    }
    final ClientLock myLock = (ClientLock) locksByID.get(lockID);
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
      myLock = (ClientLock) locksByID.get(lockID);
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
    synchronized (lockInfoByID) {
      lockInfoByID.put(threadID, globalLockInfo);
    }
    QueryLockRequest qRequest = (QueryLockRequest) pendingQueryLockRequestsByID.remove(threadID);
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
      Object o = locksByID.get(lock.getLockID());
      if (o == lock) {
        // Sometimes when called from recall, the unlock would have already removed this lock
        // from the map and a new lock could be in the map from a new lock request. We dont want to
        // remove that
        locksByID.remove(lock.getLockID());
        gcCandidates.remove(lock.getLockID());
      }
    }
  }

  /*
   * Called from a stage thread and should never be blocked
   */
  public synchronized void notified(LockID lockID, ThreadID threadID) {
    if (isPaused()) {
      logger.warn("Ignoring notified call from dead server : " + lockID + ", " + threadID);
      return;
    }
    final ClientLock myLock = (ClientLock) locksByID.get(lockID);
    if (myLock == null) { throw new AssertionError(lockID.toString()); }
    myLock.notified(threadID);
  }

  /*
   * XXX::This method is called from a stage thread. It operate on the lock inside the scope of the synchronization
   * unlike other methods because, we want to decide whether to process this award or not and go with it atomically
   */
  public synchronized void awardLock(SessionID sessionID, LockID lockID, ThreadID threadID, int level) {
    if (isPaused() || !sessionManager.isCurrentSession(sessionID)) {
      logger.warn("Ignoring lock award from a dead server :" + sessionID + ", " + sessionManager + " : " + lockID + " "
                  + threadID + " " + LockLevel.toString(level) + " state = " + state);
      return;
    }
    final ClientLock lock = (ClientLock) locksByID.get(lockID);
    if (lock == null) { throw new AssertionError("awardLock(): Lock not found" + lockID.toString() + " :: " + threadID
                                                 + " :: " + LockLevel.toString(level)); }
    lock.awardLock(threadID, level);
  }

  /*
   * XXX:: @read comment for awardLock();
   */
  public synchronized void cannotAwardLock(SessionID sessionID, LockID lockID, ThreadID threadID, int level) {
    if (isPaused() || !sessionManager.isCurrentSession(sessionID)) {
      logger.warn("Ignoring lock award from a dead server :" + sessionID + ", " + sessionManager + " : " + lockID + " "
                  + threadID + " level = " + level + " state = " + state);
      return;
    }
    final ClientLock lock = (ClientLock) locksByID.get(lockID);
    if (lock == null) { throw new AssertionError("Client id: " + ManagerUtil.getClientID()
                                                 + ", cannotAwardLock(): Lock not found" + lockID.toString() + " :: "
                                                 + threadID + " :: " + LockLevel.toString(level)); }
    lock.cannotAwardLock(threadID, level);
  }

  // This method should be called within a synchronized(this) block.
  private ClientLock getLock(LockID id) {
    return (ClientLock) locksByID.get(id);
  }

  private synchronized ClientLock getOrCreateLock(LockID id, String lockObjectType) {

    ClientLock lock = (ClientLock) locksByID.get(id);
    if (lock == null) {
      lock = new ClientLock(id, lockObjectType, remoteLockManager, waitTimer, lockStatManager);
      locksByID.put(id, lock);
    }
    return lock;
  }

  public LockID lockIDFor(String id) {
    if (id == null) return LockID.NULL_ID;
    return new LockID(id);
  }

  public synchronized Collection addAllWaitersTo(Collection c) {
    assertStarting();
    for (Iterator i = locksByID.values().iterator(); i.hasNext();) {
      ClientLock lock = (ClientLock) i.next();
      lock.addAllWaitersTo(c);
    }
    return c;
  }

  public synchronized Collection addAllHeldLocksTo(Collection c) {
    assertStarting();
    for (Iterator i = locksByID.values().iterator(); i.hasNext();) {
      ClientLock lock = (ClientLock) i.next();
      lock.addHoldersToAsLockRequests(c);
    }
    return c;
  }

  public synchronized Collection addAllPendingLockRequestsTo(Collection c) {
    assertStarting();
    for (Iterator i = locksByID.values().iterator(); i.hasNext();) {
      ClientLock lock = (ClientLock) i.next();
      lock.addAllPendingLockRequestsTo(c);
    }
    return c;
  }

  public synchronized void addAllHeldLocksAndPendingLockRequestsTo(Collection heldLocks, Collection pendingLocks) {
    for (Iterator i = locksByID.values().iterator(); i.hasNext();) {
      ClientLock lock = (ClientLock) i.next();
      lock.addHoldersToAsLockRequests(heldLocks);
      lock.addAllPendingLockRequestsTo(pendingLocks);
    }
  }

  public synchronized Collection addAllPendingTryLockRequestsTo(Collection c) {
    assertStarting();
    for (Iterator i = locksByID.values().iterator(); i.hasNext();) {
      ClientLock lock = (ClientLock) i.next();
      lock.addAllPendingTryLockRequestsTo(c);
    }
    return c;
  }

  public synchronized void setLockStatisticsConfig(int traceDepth, int gatherInterval) {
    waitUntilRunning();
    lockStatManager.setLockStatisticsConfig(traceDepth, gatherInterval);
  }

  public synchronized void setLockStatisticsEnabled(boolean statEnable) {
    waitUntilRunning();
    lockStatManager.setLockStatisticsEnabled(statEnable);
  }

  public synchronized void requestLockSpecs() {
    waitUntilRunning();
    lockStatManager.requestLockSpecs();
  }

  synchronized boolean haveLock(LockID lockID, ThreadID threadID, int lockType) {
    ClientLock l = (ClientLock) locksByID.get(lockID);
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
    return (state == RUNNING);
  }

  public synchronized boolean isPaused() {
    return (state == PAUSED);
  }

  private void assertStarting() {
    if (state != STARTING) throw new AssertionError("ClientLockManager is not STARTING : " + state);
  }

  /*
   * @returns the wait object for lock request
   */
  private synchronized Object addToPendingQueryLockRequest(LockID lockID, ThreadID threadID) {
    // Add Lock Request
    Object o = new Object();
    QueryLockRequest qRequest = new QueryLockRequest(lockID, threadID, o);
    Object old = pendingQueryLockRequestsByID.put(threadID, qRequest);
    if (old != null) {
      // formatting
      throw new AssertionError("Query Lock request already outstanding - " + old);
    }

    return o;
  }

  private synchronized void resubmitQueryLockRequests() {
    for (Iterator i = pendingQueryLockRequestsByID.values().iterator(); i.hasNext();) {
      QueryLockRequest qRequest = (QueryLockRequest) i.next();
      remoteLockManager.queryLock(qRequest.lockID(), qRequest.threadID());
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

    final ClientLockManager lockManager;

    LockGCTask(ClientLockManager mgr) {
      lockManager = mgr;
    }

    public void run() {
      lockManager.runGC();
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
    logger.info(dump());
  }

  public synchronized PrettyPrinter prettyPrint(PrettyPrinter out) {
    out.println(getClass().getName());
    out.indent().println("locks: " + locksByID.size());
    for (Iterator i = locksByID.values().iterator(); i.hasNext();) {
      out.println(i.next());
    }
    return out;
  }
}
