/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.management.beans;

import com.tc.management.TerracottaMBean;
import com.tc.management.lock.stats.LockSpec;

import java.util.Collection;

public interface LockStatisticsMonitorMBean extends TerracottaMBean {
  public static final String   TRACE_DEPTH     = "com.tc.management.lock.traceDepth";
  public static final String   GATHER_INTERVAL = "com.tc.management.lock.gatherInterval";
  public static final String   TRACES_ENABLED  = "com.tc.management.lock.tracesEnabled";
  public static final String[] ALL_EVENTS      = new String[] { TRACE_DEPTH, GATHER_INTERVAL, TRACES_ENABLED };
  public static final String   DESCRIPTION     = "Terracotta Lock Statistics Event Notification";

  public Collection<LockSpec> getLockSpecs();

  public void setLockStatisticsConfig(int traceDepth, int gatherInterval);

  public void setLockStatisticsEnabled(boolean lockStatsEnabled);

  public boolean isLockStatisticsEnabled();

  public int getTraceDepth();

  public int getGatherInterval();
}
