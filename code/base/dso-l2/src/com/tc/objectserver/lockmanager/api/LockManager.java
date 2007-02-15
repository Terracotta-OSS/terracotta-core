/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.objectserver.lockmanager.api;

import com.tc.async.api.Sink;
import com.tc.net.protocol.tcm.ChannelID;
import com.tc.object.lockmanager.api.LockID;
import com.tc.object.lockmanager.api.ThreadID;
import com.tc.object.tx.WaitInvocation;

import java.util.Collection;

/**
 * @author steve
 */
public interface LockManager {

  public void notify(LockID lid, ChannelID cid, ThreadID tid, boolean all, NotifiedWaiters addNotifiedWaitersTo);

  public void wait(LockID lid, ChannelID cid, ThreadID tid, WaitInvocation waitInvocation, Sink lockResponseSink);

  public void reestablishLock(LockID lid, ChannelID cid, ThreadID tid, int level, Sink lockResponseSink);

  public void reestablishWait(LockID lid, ChannelID cid, ThreadID tid, int level, WaitInvocation waitInvocation,
                              Sink lockResponseSink);

  public boolean requestLock(LockID lockID, ChannelID channelID, ThreadID source, int level, Sink awardLockSink);

  public boolean tryRequestLock(LockID lockID, ChannelID channelID, ThreadID threadID, int level, Sink awardLockSink);

  public void unlock(LockID id, ChannelID receiverID, ThreadID threadID);

  public void queryLock(LockID lockID, ChannelID channelID, ThreadID threadID, Sink lockResponseSink);
  
  public void interrupt(LockID lockID, ChannelID channelID, ThreadID threadID);

  public boolean hasPending(LockID id);

  public void clearAllLocksFor(ChannelID channelID);

  public void scanForDeadlocks(DeadlockResults output);

  public void start();

  public void stop() throws InterruptedException;

  public void recallCommit(LockID lid, ChannelID cid, Collection lockContexts, Collection waitContexts,
                           Collection pendingLockContexts, Sink lockResponseSink);

  public void dump();

}