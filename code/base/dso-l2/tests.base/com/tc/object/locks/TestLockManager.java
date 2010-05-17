/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object.locks;

import com.tc.async.api.Sink;
import com.tc.exception.ImplementMe;
import com.tc.net.ClientID;
import com.tc.net.NodeID;
import com.tc.object.locks.ServerLockContext.State;
import com.tc.object.locks.ServerLockContext.Type;
import com.tc.object.tx.TimerSpec;
import com.tc.objectserver.locks.LockMBean;
import com.tc.objectserver.locks.LockManager;
import com.tc.objectserver.locks.NotifiedWaiters;
import com.tc.objectserver.locks.ServerLock.NotifyAction;
import com.tc.text.PrettyPrinter;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class TestLockManager implements LockManager {

  public final List reestablishLockCalls = new ArrayList();
  public final List reestablishWaitCalls = new ArrayList();
  public final List startCalls           = new ArrayList();
  public final List notifyCalls          = new ArrayList();

  public void wait(LockID lid, NodeID cid, ThreadID tid, TimerSpec waitInvocation, Sink lockResponseSink) {
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

  public LockMBean[] getAllLocks() {
    throw new ImplementMe();
  }

  public void start() {
    this.startCalls.add(new Object());
  }

  public void stop() {
    throw new ImplementMe();
  }

  public static class ReestablishLockContext {
    private final ClientServerExchangeLockContext lockContext;

    private ReestablishLockContext(ClientServerExchangeLockContext lockContext) {
      this.lockContext = lockContext;
    }

    public ClientServerExchangeLockContext getLockContext() {
      return lockContext;
    }
  }

  public void queryLock(LockID lockID, NodeID cid, ThreadID threadID, Sink lockResponseSink) {
    throw new ImplementMe();
  }

  public void interrupt(LockID lockID, NodeID cid, ThreadID threadID) {
    throw new ImplementMe();
  }

  public boolean tryRequestLock(LockID lockID, NodeID channelID, ThreadID threadID, int level, String lockType,
                                TimerSpec timeout, Sink awardLockSink) {
    throw new ImplementMe();
  }

  public void enableLockStatsForNodeIfNeeded(NodeID nid) {
    throw new ImplementMe();
  }

  public String dump() {
    return null;
  }

  public PrettyPrinter prettyPrint(PrettyPrinter out) {
    return null;
  }

  public void clearAllLocksFor(ClientID cid) {
    return;
  }

  public void enableLockStatsForNodeIfNeeded(ClientID cid) {
    throw new ImplementMe();

  }

  public void interrupt(LockID lid, ClientID cid, ThreadID threadID) {
    throw new ImplementMe();

  }

  public void lock(LockID lid, ClientID cid, ThreadID tid, ServerLockLevel level) {
    throw new ImplementMe();

  }

  public NotifiedWaiters notify(LockID lid, ClientID cid, ThreadID tid, NotifyAction action,
                                NotifiedWaiters addNotifiedWaitersTo) {
    boolean isAll = false;
    if (action == NotifyAction.ALL) {
      isAll = true;
    }
    this.notifyCalls.add(new Object[] { lid, cid, tid, new Boolean(isAll), addNotifiedWaitersTo });
    return addNotifiedWaitersTo;
  }

  public void queryLock(LockID lid, ClientID cid, ThreadID threadID) {
    throw new ImplementMe();

  }

  public void recallCommit(LockID lid, ClientID cid, Collection<ClientServerExchangeLockContext> serverLockContexts) {
    throw new ImplementMe();

  }

  public void reestablishState(ClientID cid, Collection<ClientServerExchangeLockContext> serverLockContexts) {
    for (ClientServerExchangeLockContext lockContext : serverLockContexts) {
      if (lockContext.getState().getType() == Type.HOLDER || lockContext.getState().getType() == Type.GREEDY_HOLDER) {
        this.reestablishLockCalls.add(new ReestablishLockContext(lockContext));
      } else if (lockContext.getState() == State.WAITER) {
        this.reestablishWaitCalls.add(new ReestablishLockContext(lockContext));
      }
    }
  }

  public void tryLock(LockID lid, ClientID cid, ThreadID threadID, ServerLockLevel level, long timeout) {
    throw new ImplementMe();

  }

  public void unlock(LockID lid, ClientID receiverID, ThreadID threadID) {
    throw new ImplementMe();

  }

  public void wait(LockID lid, ClientID cid, ThreadID tid, long timeout) {
    throw new ImplementMe();

  }
}
