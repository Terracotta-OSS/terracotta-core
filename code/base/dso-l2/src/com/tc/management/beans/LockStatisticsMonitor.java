/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.management.beans;

import com.tc.management.L2LockStatsManager;
import com.tc.management.lock.stats.LockSpec;
import com.tc.objectserver.locks.L2LockStatisticsChangeListener;
import com.tc.stats.AbstractNotifyingMBean;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.management.AttributeChangeNotification;
import javax.management.MBeanNotificationInfo;
import javax.management.NotCompliantMBeanException;

public class LockStatisticsMonitor extends AbstractNotifyingMBean implements LockStatisticsMonitorMBean, Serializable {
  private final L2LockStatsManager                                    lockStatsManager;
  private final List<L2LockStatisticsChangeListener> listeners = new ArrayList<L2LockStatisticsChangeListener>();

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
    for (L2LockStatisticsChangeListener listener : listeners) {
      listener.setLockStatisticsEnabled(lockStatsEnabled, lockStatsManager);
    }
    this.lockStatsManager.setLockStatisticsEnabled(lockStatsEnabled);
    sendNotification(TRACES_ENABLED, this);
  }

  public void addL2LockStatisticsEnableDisableListener(L2LockStatisticsChangeListener listener) {
    listeners.add(listener);
    listener.setLockStatisticsEnabled(isLockStatisticsEnabled(), lockStatsManager);
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
