/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.object.handler;

import com.tc.async.api.AbstractEventHandler;
import com.tc.async.api.ConfigurationContext;
import com.tc.async.api.EventContext;
import com.tc.management.lock.stats.LockStatisticsMessage;
import com.tc.object.ClientConfigurationContext;
import com.tc.object.lockmanager.api.ClientLockManager;

public class LockStatisticsEnableDisableHandler extends AbstractEventHandler {
//  private static final TCLogger logger = TCLogging.getLogger(LockStatisticsEnableDisableHandler.class);
  
  private ClientLockManager clientLockManager;

  public void handleEvent(EventContext context) {
    LockStatisticsMessage msg = (LockStatisticsMessage) context;
    if (msg.isLockStatsEnable()) {
      clientLockManager.setLockStatisticsConfig(msg.getTraceDepth(), msg.getGatherInterval());
    } else if (msg.isLockStatsDisable()) {
      clientLockManager.setLockStatisticsEnabled(false);
    } else if (msg.isGatherLockStatistics()) {
      clientLockManager.requestLockSpecs();
    } else {
      throw new AssertionError("Invalid tasks type from Lock Statistics Message.");
    }
  }
  
  public void initialize(ConfigurationContext context) {
    super.initialize(context);
    ClientConfigurationContext ccc = (ClientConfigurationContext) context;
    this.clientLockManager = ccc.getLockManager();
  }

}
