/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object.handler;

import com.tc.async.api.AbstractEventHandler;
import com.tc.async.api.EventContext;
import com.tc.management.ClientLockStatManager;
import com.tc.management.lock.stats.LockStatisticsMessage;
import com.tc.net.NodeID;

public class LockStatisticsEnableDisableHandler extends AbstractEventHandler {
  // private static final TCLogger logger = TCLogging.getLogger(LockStatisticsEnableDisableHandler.class);

  private ClientLockStatManager     clientLockStatManager;
  
  public LockStatisticsEnableDisableHandler(ClientLockStatManager statManager) {
    this.clientLockStatManager = statManager;
  }
  
  public void handleEvent(EventContext context) {
    LockStatisticsMessage msg = (LockStatisticsMessage) context;
    if (msg.isLockStatsEnable()) {
      clientLockStatManager.setLockStatisticsConfig(msg.getTraceDepth(), msg.getGatherInterval());
    } else if (msg.isLockStatsDisable()) {
      clientLockStatManager.setLockStatisticsEnabled(false);
    } else if (msg.isGatherLockStatistics()) {
      NodeID nodeID = msg.getSourceNodeID();
      clientLockStatManager.requestLockSpecs(nodeID);
    } else {
      throw new AssertionError("Invalid tasks type from Lock Statistics Message.");
    }
  }
}
