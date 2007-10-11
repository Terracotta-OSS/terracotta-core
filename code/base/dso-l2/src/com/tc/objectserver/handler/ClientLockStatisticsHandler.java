/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.objectserver.handler;

import com.tc.async.api.AbstractEventHandler;
import com.tc.async.api.EventContext;
import com.tc.management.L2LockStatsManager;
import com.tc.net.groups.NodeID;
import com.tc.object.lockmanager.api.LockID;
import com.tc.object.msg.LockStatisticsResponseMessage;

import java.util.List;

public class ClientLockStatisticsHandler extends AbstractEventHandler {

  private L2LockStatsManager lockStatsManager;
  
  public ClientLockStatisticsHandler(L2LockStatsManager lockStatsManager) {
    this.lockStatsManager = lockStatsManager;
  }

  public void handleEvent(EventContext context) {
    LockStatisticsResponseMessage lsrm = (LockStatisticsResponseMessage)context;
    NodeID nodeID = lsrm.getClientID();
    LockID lockID = lsrm.getLockID();
    List stackTraces = lsrm.getStackTraces();
    lockStatsManager.recordStackTraces(lockID, nodeID, stackTraces);
  }

}
