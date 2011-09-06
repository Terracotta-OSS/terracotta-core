/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object.locks;

import com.tc.async.api.EventContext;
import com.tc.io.TCByteBufferOutputStream;
import com.tc.management.L2LockStatsManager;
import com.tc.net.ClientID;
import com.tc.net.GroupID;
import com.tc.net.protocol.tcm.TCMessageType;
import com.tc.object.msg.ClientHandshakeMessageImpl;
import com.tc.object.session.SessionID;
import com.tc.object.session.SessionProvider;
import com.tc.objectserver.api.ObjectStatsManager;
import com.tc.objectserver.api.TestSink;
import com.tc.objectserver.core.api.DSOGlobalServerStatsImpl;
import com.tc.objectserver.locks.LockFactory;
import com.tc.objectserver.locks.LockManagerImpl;
import com.tc.objectserver.locks.LockResponseContext;
import com.tc.objectserver.locks.NotifiedWaiters;
import com.tc.objectserver.locks.NullChannelManager;
import com.tc.objectserver.locks.ServerLock.NotifyAction;
import com.tc.stats.counter.CounterManager;
import com.tc.stats.counter.CounterManagerImpl;
import com.tc.stats.counter.sampled.SampledCounter;
import com.tc.stats.counter.sampled.SampledCounterConfig;

import java.util.Collection;
import java.util.Iterator;
import java.util.Set;

public class ClientServerLockManagerGlue implements RemoteLockManager, Runnable {

  private LockManagerImpl         serverLockManager;
  protected ClientLockManagerImpl clientLockManager;

  protected TestSink              sink;
  private final ClientID          clientID = new ClientID(1);
  protected boolean               stop     = false;
  protected Thread                eventNotifier;
  private LockFactory             factory  = null;

  protected final SessionProvider sessionProvider;

  public ClientServerLockManagerGlue(SessionProvider sessionProvider, TestSink sink, LockFactory factory) {
    this(sessionProvider, sink, "ClientServerLockManagerGlue", factory);
  }

  protected ClientServerLockManagerGlue(SessionProvider sessionProvider, TestSink sink, String threadName,
                                        LockFactory factory) {
    super();
    this.sessionProvider = sessionProvider;
    this.sink = sink;
    eventNotifier = new Thread(this, threadName);
    eventNotifier.setDaemon(true);
    eventNotifier.start();
    this.factory = factory;
  }

  public void lock(LockID lockID, ThreadID threadID, ServerLockLevel level) {
    serverLockManager.lock(lockID, clientID, threadID, level);
  }

  public void unlock(LockID lockID, ThreadID threadID, ServerLockLevel level) {
    serverLockManager.unlock(lockID, clientID, threadID);
  }

  public void wait(LockID lockID, ThreadID threadID, long timeout) {
    if (timeout > 0) {
      serverLockManager.wait(lockID, clientID, threadID, timeout);
    } else {
      System.out.println("Going to wait on lock id " + lockID + " threadId=" + threadID + " timeout=" + timeout);
      serverLockManager.wait(lockID, clientID, threadID, 0);
    }
  }

  public void recallCommit(LockID lockID, Collection<ClientServerExchangeLockContext> contexts, boolean batch) {
    serverLockManager.recallCommit(lockID, clientID, contexts);
  }

  public void flush(LockID lockID, ServerLockLevel level) {
    return;
  }

  public boolean asyncFlush(LockID lockID, LockFlushCallback callback, ServerLockLevel level) {
    return true;
  }

  public void set(ClientLockManagerImpl clmgr, LockManagerImpl slmgr) {
    this.clientLockManager = clmgr;
    this.serverLockManager = slmgr;
    this.serverLockManager.start();
    final CounterManager counterManager = new CounterManagerImpl();
    final SampledCounterConfig sampledCounterConfig = new SampledCounterConfig(1, 300, true, 0L);
    final SampledCounter lockRecallCounter = (SampledCounter) counterManager.createCounter(sampledCounterConfig);
    final SampledCounter lockCounter = (SampledCounter) counterManager.createCounter(sampledCounterConfig);

    DSOGlobalServerStatsImpl serverStats = new DSOGlobalServerStatsImpl(null, null, null, null, null, null, null, null,
                                                                        lockRecallCounter, null, null, lockCounter);
    L2LockStatsManager.UNSYNCHRONIZED_LOCK_STATS_MANAGER.start(new NullChannelManager(), serverStats,
                                                               ObjectStatsManager.NULL_OBJECT_STATS_MANAGER);
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
          clientLockManager.award(GroupID.NULL_ID, sessionProvider.getSessionID(lrc.getNodeID()), lrc.getLockID(), lrc
              .getThreadID(), lrc.getLockLevel());
        }
      }
      // ToDO :: implment WaitContext etc..
    }
  }

  public LockManagerImpl restartServer() {
    this.serverLockManager = new LockManagerImpl(sink, new NullChannelManager(), factory);
    clientLockManager.pause(GroupID.ALL_GROUPS, 1);
    ClientHandshakeMessageImpl handshakeMessage = new ClientHandshakeMessageImpl(SessionID.NULL_ID, null,
                                                                                 new TCByteBufferOutputStream(), null,
                                                                                 TCMessageType.CLIENT_HANDSHAKE_MESSAGE);
    clientLockManager.initializeHandshake(ClientID.NULL_ID, GroupID.ALL_GROUPS, handshakeMessage);
    serverLockManager.reestablishState(clientID, handshakeMessage.getLockContexts());

    this.serverLockManager.start();
    clientLockManager.unpause(GroupID.ALL_GROUPS, 0);
    return this.serverLockManager;
  }

  public void notify(LockID lockID1, ThreadID tx2, boolean all) {
    NotifiedWaiters waiters = new NotifiedWaiters();
    NotifyAction action = all ? NotifyAction.ALL : NotifyAction.ONE;
    waiters = serverLockManager.notify(lockID1, clientID, tx2, action, waiters);
    Set s = waiters.getNotifiedFor(clientID);
    System.out.println("Notified waiters " + waiters + " lockID = " + lockID1 + " threadID=" + tx2);
    for (Iterator i = s.iterator(); i.hasNext();) {
      ClientServerExchangeLockContext lc = (ClientServerExchangeLockContext) i.next();
      clientLockManager.notified(lc.getLockID(), lc.getThreadID());
    }
  }

  public void stop() {
    stop = true;
    eventNotifier.interrupt();
  }

  public void query(LockID lockID, ThreadID threadID) {
    serverLockManager.queryLock(lockID, clientID, threadID);
  }

  public void interrupt(LockID lockID, ThreadID threadID) {
    serverLockManager.interrupt(lockID, clientID, threadID);
  }

  public void tryLock(LockID lockID, ThreadID threadID, ServerLockLevel level, long timeout) {
    serverLockManager.tryLock(lockID, clientID, threadID, level, timeout);
  }

  public ClientID getClientID() {
    return ClientID.NULL_ID;
  }

  public void waitForServerToReceiveTxnsForThisLock(LockID lock) {
    //
  }

  public void shutdown() {
    // no op
  }

  public boolean isShutdown() {
    return false;
  }

}
