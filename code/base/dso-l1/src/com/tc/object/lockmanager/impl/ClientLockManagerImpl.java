/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object.lockmanager.impl;

import com.tc.logging.TCLogger;
import com.tc.object.lockmanager.api.ClientLockManager;
import com.tc.object.lockmanager.api.LockFlushCallback;
import com.tc.object.lockmanager.api.LockID;
import com.tc.object.lockmanager.api.LockLevel;
import com.tc.object.lockmanager.api.Notify;
import com.tc.object.lockmanager.api.RemoteLockManager;
import com.tc.object.lockmanager.api.ThreadID;
import com.tc.object.lockmanager.api.WaitListener;
import com.tc.object.lockmanager.api.WaitTimer;
import com.tc.object.session.SessionID;
import com.tc.object.session.SessionManager;
import com.tc.object.tx.WaitInvocation;
import com.tc.text.ConsoleParagraphFormatter;
import com.tc.text.ParagraphFormatter;
import com.tc.text.StringFormatter;
import com.tc.util.Assert;
import com.tc.util.State;
import com.tc.util.Util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.TimerTask;

/**
 * @author steve
 */
public class ClientLockManagerImpl implements ClientLockManager, LockFlushCallback {

  public static final long        TIMEOUT                      = 60 * 1000;

  private static final State      RUNNING                      = new State("RUNNING");
  private static final State      STARTING                     = new State("STARTING");
  private static final State      PAUSED                       = new State("PAUSED");

  private static final String     MISSING_LOCK_TEXT            = makeMissingLockText();

  private State                   state                        = RUNNING;
  private final Map               locksByID                    = new HashMap();
  private final Map               pendingQueryLockRequestsByID = new HashMap();
  private final Map               lockInfoByID                 = new HashMap();
  private final RemoteLockManager remoteLockManager;
  private final WaitTimer         waitTimer                    = new WaitTimerImpl();
  private final TCLogger          logger;
  private final SessionManager    sessionManager;

  public ClientLockManagerImpl(TCLogger logger, RemoteLockManager remoteLockManager, SessionManager sessionManager) {
    this.logger = logger;
    this.remoteLockManager = remoteLockManager;
    this.sessionManager = sessionManager;
    waitTimer.getTimer().schedule(new LockGCTask(this), TIMEOUT, TIMEOUT);
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
  }

  public synchronized boolean isStarting() {
    return state == STARTING;
  }

  public synchronized void runGC() {
    waitUntilRunning();
    logger.info("Running Lock GC...");
    ArrayList toGC = new ArrayList(locksByID.size());
    for (Iterator iter = locksByID.values().iterator(); iter.hasNext();) {
      ClientLock lock = (ClientLock) iter.next();
      if (lock.timedout()) {
        toGC.add(lock.getLockID());
      }
    }
    if (toGC.size() > 0) {
      logger.debug("GCing " + (toGC.size() < 11 ? toGC.toString() : toGC.size() + " Locks ..."));
      for (Iterator iter = toGC.iterator(); iter.hasNext();) {
        LockID lockID = (LockID) iter.next();
        recall(lockID, ThreadID.VM_ID, LockLevel.WRITE);
      }
    }
  }

  private GlobalLockInfo getLockInfo(LockID lockID, ThreadID threadID) {
    Object waitLock = addToPendingQueryLockRequest(lockID, threadID);
    remoteLockManager.queryLock(lockID, threadID);
    waitForQueryReply(threadID, waitLock);
    GlobalLockInfo lockInfo;
    synchronized (this) {
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

    int queueLength = lockInfo.getLockRequestQueueLength() + lockInfo.getLockUpgradeQueueLength();
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

    if (lock != null) {
      waitLength += lock.waitLength();
    }

    return waitLength;
  }

  public int heldCount(LockID lockID, int lockLevel, ThreadID threadID) {
    ClientLock lock;
    synchronized (this) {
      waitUntilRunning();
      lock = (ClientLock) locksByID.get(lockID);
    }
    if (lock == null) {
      return 0;
    } else {
      return lock.heldCount(threadID, lockLevel);
    }
  }

  // TODO:
  // Needs to take care of the greedy lock case.
  public boolean isLocked(LockID lockID, ThreadID threadID) {
    ClientLock lock;
    synchronized (this) {
      waitUntilRunning();
      lock = (ClientLock) locksByID.get(lockID);
    }
    if (lock != null) {
      return lock.isHeld();
    } else {
      GlobalLockInfo lockInfo = getLockInfo(lockID, threadID);
      return lockInfo.isLocked();
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

  private synchronized boolean hasLockInfo(ThreadID threadID) {
    return lockInfoByID.containsKey(threadID);
  }

  public void lock(LockID lockID, ThreadID threadID, int type) {
    Assert.assertNotNull("threadID", threadID);
    final ClientLock lock;

    synchronized (this) {
      waitUntilRunning();
      lock = getOrCreateLock(lockID);
      lock.incUseCount();
    }
    lock.lock(threadID, type);
  }

  public boolean tryLock(LockID lockID, ThreadID threadID, int type) {
    Assert.assertNotNull("threadID", threadID);
    final ClientLock lock;

    synchronized (this) {
      waitUntilRunning();
      lock = getOrCreateLock(lockID);
      lock.incUseCount();
    }
    boolean isLocked = lock.tryLock(threadID, type);
    if (!isLocked) {
      synchronized (this) {
        lock.decUseCount();
      }
      cleanUp(lock);
    }
    return isLocked;
  }

  public void unlock(LockID lockID, ThreadID threadID) {
    final ClientLock myLock;

    synchronized (this) {
      waitUntilRunning();
      myLock = (ClientLock) locksByID.get(lockID);
      if (myLock == null) { throw missingLockException(lockID); }
      myLock.decUseCount();
    }

    myLock.unlock(threadID);
    cleanUp(myLock);
  }

  private AssertionError missingLockException(LockID lockID) {
    return new AssertionError(MISSING_LOCK_TEXT + " Missing lock ID is " + lockID);
  }

  public void wait(LockID lockID, ThreadID threadID, WaitInvocation call, Object waitLock, WaitListener listener)
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
   * The level represents the reason why server wanted a recall and will determite when a recall commit will happen.
   */
  public synchronized void recall(LockID lockID, ThreadID threadID, int interestedLevel) {
    Assert.assertEquals(ThreadID.VM_ID, threadID);
    if (isPaused()) {
      logger.warn("Ignoring recall request from dead server : " + lockID + ", " + threadID + " interestedLevel : "
                  + LockLevel.toString(interestedLevel));
      return;
    }
    final ClientLock myLock = (ClientLock) locksByID.get(lockID);
    if (myLock != null) {
      myLock.recall(interestedLevel, this);
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
    lockInfoByID.put(threadID, globalLockInfo);
    Object waitLock = pendingQueryLockRequestsByID.remove(threadID);
    if (waitLock == null) { throw new AssertionError("Query Lock request does not exist."); }
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
    if (lock == null) { throw new AssertionError("awardLock(): Lock not found" + lockID.toString() + " :: " + threadID
                                                 + " :: " + LockLevel.toString(level)); }
    lock.cannotAwardLock(threadID, level);
  }

  // This method should be called within a synchronized(this) block.
  private ClientLock getLock(LockID id) {
    return (ClientLock) locksByID.get(id);
  }

  private synchronized ClientLock getOrCreateLock(LockID id) {
    ClientLock lock = (ClientLock) locksByID.get(id);
    if (lock == null) {
      lock = new ClientLock(id, remoteLockManager, waitTimer);
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
    Object old = pendingQueryLockRequestsByID.put(threadID, o);
    if (old != null) {
      // formatting
      throw new AssertionError("Query Lock request already outstanding - " + old);
    }

    return o;
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
}
