/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.lockmanager.api;

import com.tc.async.api.Sink;
import com.tc.config.lock.LockContextInfo;
import com.tc.exception.ImplementMe;
import com.tc.net.groups.NodeID;
import com.tc.object.lockmanager.api.LockContext;
import com.tc.object.lockmanager.api.LockID;
import com.tc.object.lockmanager.api.ThreadID;
import com.tc.object.tx.TimerSpec;
import com.tc.text.PrettyPrinter;

import java.io.Writer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

public class TestLockManager implements LockManager {

  public final List reestablishLockCalls = new ArrayList();
  public final List reestablishWaitCalls = new ArrayList();
  public final List startCalls           = new ArrayList();
  public final List notifyCalls          = new ArrayList();

  public void notify(LockID lid, NodeID cid, ThreadID tid, boolean all, NotifiedWaiters addNotifiedWaitersTo) {
    notifyCalls.add(new Object[] { lid, cid, tid, new Boolean(all), addNotifiedWaitersTo });
  }

  public void wait(LockID lid, NodeID cid, ThreadID tid, TimerSpec waitInvocation, Sink lockResponseSink) {
    throw new ImplementMe();
  }

  public static final class WaitCallContext {
    public final LockID         lockID;
    public final NodeID         nid;
    public final ThreadID       threadID;
    public final TimerSpec waitInvocation;
    public final Sink           lockResponseSink;

    private WaitCallContext(LockID lockID, NodeID cid, ThreadID tid, int level, TimerSpec waitInvocation,
                            Sink lockResponseSink) {
      this.lockID = lockID;
      this.nid = cid;
      this.threadID = tid;
      this.waitInvocation = waitInvocation;
      this.lockResponseSink = lockResponseSink;
    }
  }

  public boolean requestLock(LockID lockID, NodeID channelID, ThreadID source, int level, String lockType, Sink awardLockSink) {
    throw new ImplementMe();
  }

  public void unlock(LockID id, NodeID receiverID, ThreadID threadID) {
    throw new ImplementMe();
  }

  public boolean isLocked(LockID id) {
    throw new ImplementMe();
  }

  public boolean hasPending(LockID id) {
    throw new ImplementMe();
  }

  public void clearAllLocksFor(NodeID cid) {
    return;
  }

  public Iterator getAllLocks() {
    throw new ImplementMe();
  }

  public void scanForDeadlocks(DeadlockResults output) {
    throw new ImplementMe();
  }

  public void start() {
    this.startCalls.add(new Object());
  }

  public void stop() {
    throw new ImplementMe();
  }

  public void waitTimeout(LockWaitContext context) {
    throw new ImplementMe();
  }

  public void reestablishWait(LockID lid, NodeID cid, ThreadID tid, int level, TimerSpec waitInvocation,
                              Sink lockResponseSink) {
    reestablishWaitCalls.add(new WaitCallContext(lid, cid, tid, level, waitInvocation, lockResponseSink));
  }

  public void reestablishLock(LockID lid, NodeID cid, ThreadID tid, int level, Sink lockResponseSink) {
    reestablishLockCalls.add(new ReestablishLockContext(new LockContext(lid, cid, tid, level, LockContextInfo.NULL_LOCK_OBJECT_TYPE), lockResponseSink));
  }

  public static class ReestablishLockContext {
    public final LockContext lockContext;
    public final Sink        lockResponseSink;

    private ReestablishLockContext(LockContext lockContext, Sink lockResponseSink) {
      this.lockContext = lockContext;
      this.lockResponseSink = lockResponseSink;
    }
  }
  
  

  public void queryLock(LockID lockID, NodeID cid, ThreadID threadID, Sink lockResponseSink) {
    throw new ImplementMe();
  }

  public void interrupt(LockID lockID, NodeID cid, ThreadID threadID) {
    throw new ImplementMe();
  }

  public boolean tryRequestLock(LockID lockID, NodeID channelID, ThreadID threadID, int level, String lockType, TimerSpec timeout,
                                Sink awardLockSink) {
    throw new ImplementMe();
  }

  public void recallCommit(LockID lid, NodeID cid, Collection lockContexts, Collection waitContexts,
                           Collection pendingLockContexts, Collection pendingTryLockContexts, Sink lockResponseSink) {
    throw new ImplementMe();
  }

  public void enableLockStatsForNodeIfNeeded(NodeID nid) {
    throw new ImplementMe();
  }

  public String dump() {
    return null;
  }

  public void dump(Writer writer) {
    throw new ImplementMe();
    
  }

  public void dumpToLogger() {
    throw new ImplementMe();
    
  }

  public PrettyPrinter prettyPrint(PrettyPrinter out) {
    return null;
  }
}
