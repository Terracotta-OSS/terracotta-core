/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object.lockmanager.impl;

import com.tc.async.api.EventContext;
import com.tc.management.ClientLockStatManager;
import com.tc.management.L2LockStatsManager;
import com.tc.net.protocol.tcm.ChannelID;
import com.tc.object.msg.LockStatisticsResponseMessage;
import com.tc.objectserver.api.TestSink;
import com.tc.objectserver.context.LockResponseContext;

public class ClientServerLockStatManagerGlue implements Runnable {
  private ClientLockStatManager clientLockStatManager;
  private L2LockStatsManager    serverLockStatManager;

  private TestSink              sink;
  private ChannelID             channelID = new ChannelID(1);
  private boolean               stop      = false;
  private Thread                eventNotifier;

  public ClientServerLockStatManagerGlue(TestSink sink) {
    super();
    this.sink = sink;
    eventNotifier = new Thread(this, "ClientServerLockStatManagerGlue");
    eventNotifier.setDaemon(true);
    eventNotifier.start();
  }

  public void set(ClientLockStatManager clientLockStatManager, L2LockStatsManager serverLockStatManager) {
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
        if (lrc.isLockStatEnabled()) {
          clientLockStatManager.enableStat(lrc.getLockID(), lrc.getStackTraceDepth(), lrc.getStatCollectFrequency());
        }
      } else if (ec instanceof LockStatisticsResponseMessage) {
        LockStatisticsResponseMessage lsrm = (LockStatisticsResponseMessage)ec;
        serverLockStatManager.recordStackTraces(lsrm.getLockID(), lsrm.getClientID(), lsrm.getStackTraces());
      }
      // ToDO :: implment WaitContext etc..
    }
  }

  public void stop() {
    stop = true;
    eventNotifier.interrupt();
  }
}
