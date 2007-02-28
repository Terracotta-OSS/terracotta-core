/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object.lockmanager.impl;

import com.tc.async.api.EventContext;
import com.tc.async.api.Sink;
import com.tc.async.impl.NullSink;
import com.tc.net.protocol.tcm.ChannelID;
import com.tc.object.lockmanager.api.ClientLockManager;
import com.tc.object.lockmanager.api.LockContext;
import com.tc.object.lockmanager.api.LockFlushCallback;
import com.tc.object.lockmanager.api.LockID;
import com.tc.object.lockmanager.api.LockRequest;
import com.tc.object.lockmanager.api.RemoteLockManager;
import com.tc.object.lockmanager.api.ThreadID;
import com.tc.object.lockmanager.api.WaitContext;
import com.tc.object.lockmanager.api.WaitLockRequest;
import com.tc.object.session.SessionProvider;
import com.tc.object.tx.WaitInvocation;
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
  private ClientLockManager     clientLockManager;

  private TestSink              sink      = new TestSink();
  private ChannelID             channelID = new ChannelID(1);
  private boolean               stop      = false;
  private Thread                eventNotifier;

  private final SessionProvider sessionProvider;

  public ClientServerLockManagerGlue(SessionProvider sessionProvider) {
    super();
    this.sessionProvider = sessionProvider;
    eventNotifier = new Thread(this, "ClientServerLockManagerGlue");
    eventNotifier.setDaemon(true);
    eventNotifier.start();
  }

  public void requestLock(LockID lockID, ThreadID threadID, int lockType) {
    serverLockManager.requestLock(lockID, channelID, threadID, lockType, sink);
  }

  public void releaseLock(LockID lockID, ThreadID threadID) {
    serverLockManager.unlock(lockID, channelID, threadID);
  }

  public void releaseLockWait(LockID lockID, ThreadID threadID, WaitInvocation call) {
    serverLockManager.wait(lockID, channelID, threadID, call, sink);
  }

  public void recallCommit(LockID lockID, Collection lockContext, Collection waitContext, Collection pendingRequests) {
    Collection serverLC = new ArrayList();
    for (Iterator i = lockContext.iterator(); i.hasNext();) {
      LockRequest request = (LockRequest) i.next();
      LockContext ctxt = new LockContext(request.lockID(), channelID, request.threadID(), request.lockLevel());
      serverLC.add(ctxt);
    }

    Collection serverWC = new ArrayList();
    for (Iterator i = waitContext.iterator(); i.hasNext();) {
      WaitLockRequest request = (WaitLockRequest) i.next();
      WaitContext ctxt = new WaitContext(request.lockID(), channelID, request.threadID(), request.lockLevel(), request
          .getWaitInvocation());
      serverWC.add(ctxt);
    }

    Collection serverPC = new ArrayList();
    for (Iterator i = pendingRequests.iterator(); i.hasNext();) {
      LockRequest request = (LockRequest) i.next();
      LockContext ctxt = new LockContext(request.lockID(), channelID, request.threadID(), request.lockLevel());
      serverPC.add(ctxt);
    }

    serverLockManager.recallCommit(lockID, channelID, serverLC, serverWC, serverPC, sink);
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
    this.serverLockManager = new LockManagerImpl(new NullChannelManager());
    if (!clientLockManager.isStarting()) clientLockManager.starting();
    for (Iterator i = clientLockManager.addAllHeldLocksTo(new HashSet()).iterator(); i.hasNext();) {
      LockRequest request = (LockRequest) i.next();
      serverLockManager
          .reestablishLock(request.lockID(), channelID, request.threadID(), request.lockLevel(), NULL_SINK);
    }

    for (Iterator i = clientLockManager.addAllWaitersTo(new HashSet()).iterator(); i.hasNext();) {
      WaitLockRequest request = (WaitLockRequest) i.next();
      serverLockManager.reestablishWait(request.lockID(), channelID, request.threadID(), request.lockLevel(), request
          .getWaitInvocation(), sink);
    }

    for (Iterator i = clientLockManager.addAllPendingLockRequestsTo(new HashSet()).iterator(); i.hasNext();) {
      LockRequest request = (LockRequest) i.next();
      serverLockManager.requestLock(request.lockID(), channelID, request.threadID(), request.lockLevel(), sink);
    }

    if (policy == LockManagerImpl.ALTRUISTIC_LOCK_POLICY) {
      this.serverLockManager.setLockPolicy(policy);
    }
    this.serverLockManager.start();
    return this.serverLockManager;
  }

  public void notify(LockID lockID1, ThreadID tx2, boolean all) {
    NotifiedWaiters waiters = new NotifiedWaiters();
    serverLockManager.notify(lockID1, channelID, tx2, all, waiters);
    Set s = waiters.getNotifiedFor(channelID);
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
    serverLockManager.queryLock(lockID, channelID, threadID, sink);
  }

  public void tryRequestLock(LockID lockID, ThreadID threadID, int lockType) {
    serverLockManager.tryRequestLock(lockID, channelID, threadID, lockType, sink);
  }

  public void interrruptWait(LockID lockID, ThreadID threadID) {
    serverLockManager.interrupt(lockID, channelID, threadID);

  }
}
