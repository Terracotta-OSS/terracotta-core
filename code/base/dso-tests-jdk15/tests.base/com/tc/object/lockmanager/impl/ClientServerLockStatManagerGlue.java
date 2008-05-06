/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object.lockmanager.impl;

import com.tc.async.api.EventContext;
import com.tc.management.ClientLockStatManager;
import com.tc.management.L2LockStatsManager;
import com.tc.management.lock.stats.LockStatisticsMessage;
import com.tc.management.lock.stats.LockStatisticsResponseMessage;
import com.tc.object.lockmanager.api.ClientLockManager;
import com.tc.object.session.SessionProvider;
import com.tc.objectserver.api.TestSink;
import com.tc.objectserver.context.LockResponseContext;
import com.tc.objectserver.lockmanager.impl.LockManagerImpl;

public class ClientServerLockStatManagerGlue extends ClientServerLockManagerGlue {
  private ClientLockStatManager clientLockStatManager;
  private L2LockStatsManager    serverLockStatManager;

  public ClientServerLockStatManagerGlue(SessionProvider sessionProvider, TestSink sink) {
    super(sessionProvider, sink, "ClientServerLockStatManagerGlue");
  }

  public void set(ClientLockManager clmgr, LockManagerImpl slmgr, ClientLockStatManager clientLockStatManager, L2LockStatsManager serverLockStatManager) {
    super.set(clmgr, slmgr);
    this.clientLockStatManager = clientLockStatManager;
    this.serverLockStatManager = serverLockStatManager;
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
      } else if (ec instanceof LockStatisticsMessage) {
        LockStatisticsMessage lsm = (LockStatisticsMessage)ec;
        if (lsm.isLockStatsEnableDisable()) {
          clientLockStatManager.setLockStatisticsConfig(lsm.getTraceDepth(), lsm.getGatherInterval());
        } else if (lsm.isGatherLockStatistics()) {
          clientLockStatManager.requestLockSpecs();
        }
      } else if (ec instanceof LockStatisticsResponseMessage) {
        LockStatisticsResponseMessage lsrm = (LockStatisticsResponseMessage)ec;
        serverLockStatManager.recordClientStat(lsrm.getClientID(), lsrm.getStackTraceElements());
      }
    }
  }

}
