/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.locks;

import com.tc.management.L2LockStatsManager;

public interface L2LockStatisticsChangeListener {
  public void setLockStatisticsEnabled(boolean lockStatsEnabled, L2LockStatsManager manager);
}