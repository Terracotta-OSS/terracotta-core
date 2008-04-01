/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object.lockmanager.impl;

import com.tc.async.api.EventContext;
import com.tc.async.api.Sink;
import com.tc.async.impl.NullSink;
import com.tc.management.L2LockStatsManager;
import com.tc.net.groups.ClientID;
import com.tc.net.protocol.tcm.ChannelID;
import com.tc.object.lockmanager.api.ClientLockManager;
import com.tc.object.lockmanager.api.LockContext;
import com.tc.object.lockmanager.api.LockFlushCallback;
import com.tc.object.lockmanager.api.LockID;
import com.tc.object.lockmanager.api.LockRequest;
import com.tc.object.lockmanager.api.RemoteLockManager;
import com.tc.object.lockmanager.api.ThreadID;
import com.tc.object.lockmanager.api.TryLockContext;
import com.tc.object.lockmanager.api.TryLockRequest;
import com.tc.object.lockmanager.api.WaitContext;
import com.tc.object.lockmanager.api.WaitLockRequest;
import com.tc.object.session.SessionProvider;
import com.tc.object.tx.TimerSpec;
import com.tc.objectserver.api.TestSink;
import com.tc.objectserver.context.LockResponseContext;
import com.tc.objectserver.lockmanager.api.NotifiedWaiters;
import com.tc.objectserver.lockmanager.api.NullChannelManager;
import com.tc.objectserver.lockmanager.impl.LockManagerImpl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

public class ClientServerLockManagerGlue implements RemoteLockManager, Runnable {

  private static final Sink     NULL_SINK = new NullSink();

  private LockManagerImpl       serverLockManager;
  protected ClientLockManager     clientLockManager;

  protected TestSink              sink;
  private ClientID              clientID  = new ClientID(new ChannelID(1));
  protected boolean               stop      = false;
  protected Thread                eventNotifier;

  protected final SessionProvider sessionProvider;

  public ClientServerLockManagerGlue(SessionProvider sessionProvider) {
    this(sessionProvider, new TestSink(), "ClientServerLockManagerGlue");
  }
  
  protected ClientServerLockManagerGlue(SessionProvider sessionProvider, TestSink sink, String threadName) {
    super();
    this.sessionProvider = sessionProvider;
    this.sink = sink;
    eventNotifier = new Thread(this, threadName);
    eventNotifier.setDaemon(true);
    eventNotifier.start();
  }

  public void requestLock(LockID lockID, ThreadID threadID, int lockType, String lockObjectType) {
    serverLockManager.requestLock(lockID, clientID, threadID, lockType, lockObjectType, sink);
  }

  public void releaseLock(LockID lockID, ThreadID threadID) {
    serverLockManager.unlock(lockID, clientID, threadID);
  }

  public void releaseLockWait(LockID lockID, ThreadID threadID, TimerSpec call) {
    serverLockManager.wait(lockID, clientID, threadID, call, sink);
  }

  public void recallCommit(LockID lockID, Collection lockContext, Collection waitContext, Collection pendingRequests,
                           Collection pendingTryLockRequests) {
    Collection serverLC = new ArrayList();
    for (Iterator i = lockContext.iterator(); i.hasNext();) {
      LockRequest request = (LockRequest) i.next();
      LockContext ctxt = new LockContext(request.lockID(), clientID, request.threadID(), request.lockLevel(), request.lockType());
      serverLC.add(ctxt);
    }

    Collection serverWC = new ArrayList();
    for (Iterator i = waitContext.iterator(); i.hasNext();) {
      WaitLockRequest request = (WaitLockRequest) i.next();
      WaitContext ctxt = new WaitContext(request.lockID(), clientID, request.threadID(), request.lockLevel(), request.lockType(), request
          .getTimerSpec());
      serverWC.add(ctxt);
    }

    Collection serverPC = new ArrayList();
    for (Iterator i = pendingRequests.iterator(); i.hasNext();) {
      LockRequest request = (LockRequest) i.next();
      LockContext ctxt = new LockContext(request.lockID(), clientID, request.threadID(), request.lockLevel(), request.lockType());
      serverPC.add(ctxt);
    }

    Collection serverPTC = new ArrayList();
    for (Iterator i = pendingTryLockRequests.iterator(); i.hasNext();) {
      TryLockRequest request = (TryLockRequest) i.next();
      LockContext ctxt = new TryLockContext(request.lockID(), clientID, request.threadID(), request.lockLevel(), request.lockType(),
                                            request.getTimerSpec());
      serverPTC.add(ctxt);
    }

    serverLockManager.recallCommit(lockID, clientID, serverLC, serverWC, serverPC, serverPTC, sink);
  }

  public void flush(LockID lockID) {
    return;
  }

  public boolean isTransactionsForLockFlushed(LockID lockID, LockFlushCallback callback) {
    return true;
  }

  public void set(ClientLockManager clmgr, LockManagerImpl slmgr) {
    this.clientLockManager = clmgr;
    this.serverLockManager = slmgr;
    this.serverLockManager.start();
  }

  public void run() {
    while (!stop) {
      EventContext ec = null;
      try {
        ec = sink.take();
      } catch (InterruptedException e) {
        //
      }
      if (ec instanceof LockResponseContext) {
        LockResponseContext lrc = (LockResponseContext) ec;
        if (lrc.isLockAward()) {
          clientLockManager.awardLock(sessionProvider.getSessionID(), lrc.getLockID(), lrc.getThreadID(), lrc
              .getLockLevel());
        }
      }
      // ToDO :: implment WaitContext etc..
    }
  }

  public LockManagerImpl restartServer() {
    int policy = this.serverLockManager.getLockPolicy();
    this.serverLockManager = new LockManagerImpl(new NullChannelManager(), L2LockStatsManager.NULL_LOCK_STATS_MANAGER);
    if (!clientLockManager.isStarting()) clientLockManager.starting();
    for (Iterator i = clientLockManager.addAllHeldLocksTo(new HashSet()).iterator(); i.hasNext();) {
      LockRequest request = (LockRequest) i.next();
      serverLockManager.reestablishLock(request.lockID(), clientID, request.threadID(), request.lockLevel(), NULL_SINK);
    }

    for (Iterator i = clientLockManager.addAllWaitersTo(new HashSet()).iterator(); i.hasNext();) {
      WaitLockRequest request = (WaitLockRequest) i.next();
      serverLockManager.reestablishWait(request.lockID(), clientID, request.threadID(), request.lockLevel(), request
          .getTimerSpec(), sink);
    }

    for (Iterator i = clientLockManager.addAllPendingLockRequestsTo(new HashSet()).iterator(); i.hasNext();) {
      LockRequest request = (LockRequest) i.next();
      serverLockManager.requestLock(request.lockID(), clientID, request.threadID(), request.lockLevel(), request.lockType(), sink);
    }

    if (policy == LockManagerImpl.ALTRUISTIC_LOCK_POLICY) {
      this.serverLockManager.setLockPolicy(policy);
    }
    this.serverLockManager.start();
    return this.serverLockManager;
  }

  public void notify(LockID lockID1, ThreadID tx2, boolean all) {
    NotifiedWaiters waiters = new NotifiedWaiters();
    serverLockManager.notify(lockID1, clientID, tx2, all, waiters);
    Set s = waiters.getNotifiedFor(clientID);
    for (Iterator i = s.iterator(); i.hasNext();) {
      LockContext lc = (LockContext) i.next();
      clientLockManager.notified(lc.getLockID(), lc.getThreadID());
    }
  }

  public void stop() {
    stop = true;
    eventNotifier.interrupt();
  }

  public void queryLock(LockID lockID, ThreadID threadID) {
    serverLockManager.queryLock(lockID, clientID, threadID, sink);
  }

  public void tryRequestLock(LockID lockID, ThreadID threadID, int lockType, String lockObjectType) {
    serverLockManager.tryRequestLock(lockID, clientID, threadID, lockType, lockObjectType, new TimerSpec(0), sink);
  }

  public void interrruptWait(LockID lockID, ThreadID threadID) {
    serverLockManager.interrupt(lockID, clientID, threadID);

  }

  public void tryRequestLock(LockID lockID, ThreadID threadID, TimerSpec timeout, int lockType, String lockObjectType) {
    serverLockManager.tryRequestLock(lockID, clientID, threadID, lockType, lockObjectType, timeout, sink);
  }
}
