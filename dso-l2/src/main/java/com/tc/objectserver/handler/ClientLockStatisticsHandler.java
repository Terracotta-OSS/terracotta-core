/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.objectserver.handler;

import com.tc.async.api.AbstractEventHandler;
import com.tc.async.api.EventContext;
import com.tc.management.L2LockStatsManager;
import com.tc.management.lock.stats.LockStatisticsResponseMessageImpl;
import com.tc.net.NodeID;

import java.util.Collection;

public class ClientLockStatisticsHandler extends AbstractEventHandler {

  private L2LockStatsManager lockStatsManager;
  
  public ClientLockStatisticsHandler(L2LockStatsManager lockStatsManager) {
    this.lockStatsManager = lockStatsManager;
  }

  public void handleEvent(EventContext context) {
    LockStatisticsResponseMessageImpl lsrm = (LockStatisticsResponseMessageImpl)context;
    NodeID nodeID = lsrm.getSourceNodeID();
    Collection lockStatElements = lsrm.getStackTraceElements();
    lockStatsManager.recordClientStat(nodeID, lockStatElements);
  }

}
