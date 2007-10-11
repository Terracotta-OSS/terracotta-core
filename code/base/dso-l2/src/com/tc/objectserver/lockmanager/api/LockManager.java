/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.objectserver.lockmanager.api;

import com.tc.async.api.Sink;
import com.tc.net.groups.NodeID;
import com.tc.object.lockmanager.api.LockID;
import com.tc.object.lockmanager.api.ThreadID;
import com.tc.object.tx.WaitInvocation;

import java.util.Collection;
import java.util.Set;

/**
 * The Lock Manager interface implemented at the L2
 */
public interface LockManager {
  
  public void enableClientStat(LockID lockID, Sink sink , int stackTraceDepth, int statCollectFrequency);
  
  public void disableClientStat(LockID lockID, Set statEnabledClients, Sink sink);

  public void notify(LockID lid, NodeID cid, ThreadID tid, boolean all, NotifiedWaiters addNotifiedWaitersTo);

  public void wait(LockID lid, NodeID cid, ThreadID tid, WaitInvocation waitInvocation, Sink lockResponseSink);

  public void reestablishLock(LockID lid, NodeID cid, ThreadID tid, int level, Sink lockResponseSink);

  public void reestablishWait(LockID lid, NodeID cid, ThreadID tid, int level, WaitInvocation waitInvocation,
                              Sink lockResponseSink);

  public boolean requestLock(LockID lockID, NodeID cid, ThreadID source, int level, Sink awardLockSink);

  public boolean tryRequestLock(LockID lockID, NodeID cid, ThreadID threadID, int level, WaitInvocation timeout, Sink awardLockSink);

  public void unlock(LockID id, NodeID receiverID, ThreadID threadID);

  public void queryLock(LockID lockID, NodeID cid, ThreadID threadID, Sink lockResponseSink);
  
  public void interrupt(LockID lockID, NodeID cid, ThreadID threadID);

  public boolean hasPending(LockID id);

  public void clearAllLocksFor(NodeID cid);

  public void scanForDeadlocks(DeadlockResults output);

  public void start();

  public void stop() throws InterruptedException;

  public void recallCommit(LockID lid, NodeID cid, Collection lockContexts, Collection waitContexts,
                           Collection pendingLockContexts, Collection pendingTryLockContexts, Sink lockResponseSink);

  public void dump();

}
