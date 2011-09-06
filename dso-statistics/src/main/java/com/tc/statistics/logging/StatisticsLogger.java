/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.statistics.logging;

import com.tc.statistics.StatisticRetrievalAction;
import com.tc.statistics.config.DSOStatisticsConfig;

public interface StatisticsLogger {
  public final static int DEFAULT_LOGGING_INTERVAL = 15 * 60; // 15 minutes

  public void startup();

  public void shutdown();

  public void removeAllActions();

  public void registerAction(StatisticRetrievalAction action);

  public DSOStatisticsConfig getConfig();

  public boolean containsAction(StatisticRetrievalAction action);
}