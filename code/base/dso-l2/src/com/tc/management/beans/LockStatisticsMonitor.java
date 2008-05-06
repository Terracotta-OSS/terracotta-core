/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.management.beans;

import com.tc.management.L2LockStatsManager;
import com.tc.management.lock.stats.LockSpec;
import com.tc.stats.AbstractNotifyingMBean;

import java.io.Serializable;
import java.util.Collection;

import javax.management.AttributeChangeNotification;
import javax.management.MBeanNotificationInfo;
import javax.management.NotCompliantMBeanException;

public class LockStatisticsMonitor extends AbstractNotifyingMBean implements LockStatisticsMonitorMBean, Serializable {
  private final L2LockStatsManager lockStatsManager;
  
  public LockStatisticsMonitor(L2LockStatsManager lockStatsManager) throws NotCompliantMBeanException {
    super(LockStatisticsMonitorMBean.class);
    this.lockStatsManager = lockStatsManager;
  }

  public Collection<LockSpec> getLockSpecs() {
    return this.lockStatsManager.getLockSpecs();
  }

  public void setLockStatisticsConfig(int traceDepth, int gatherInterval) {
    this.lockStatsManager.setLockStatisticsConfig(traceDepth, gatherInterval);
    sendNotification(TRACE_DEPTH, this);
    sendNotification(GATHER_INTERVAL, this);
  }

  public void setLockStatisticsEnabled(boolean lockStatsEnabled) {
    this.lockStatsManager.setLockStatisticsEnabled(lockStatsEnabled);
    sendNotification(TRACES_ENABLED, this);
  }

  public boolean isLockStatisticsEnabled() {
    return this.lockStatsManager.isLockStatisticsEnabled();
  }

  public int getTraceDepth() {
    return this.lockStatsManager.getTraceDepth();
  }

  public int getGatherInterval() {
    return this.lockStatsManager.getGatherInterval();
  }

  public MBeanNotificationInfo[] getNotificationInfo() {
    return new MBeanNotificationInfo[] { new MBeanNotificationInfo(ALL_EVENTS, AttributeChangeNotification.class
        .getName(), DESCRIPTION) };
  }

  public void reset() {
    // nothing to do
  }
}
