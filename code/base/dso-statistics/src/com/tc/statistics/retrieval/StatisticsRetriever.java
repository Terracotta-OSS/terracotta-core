/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.statistics.retrieval;

import com.tc.statistics.StatisticRetrievalAction;
import com.tc.statistics.config.DSOStatisticsConfig;

public interface StatisticsRetriever {
  public final static Long DEFAULT_GLOBAL_FREQUENCY = new Long(1000L);

  public void startup();

  public void shutdown();

  public String getSessionId();

  public void removeAllActions();

  public void registerAction(StatisticRetrievalAction action);

  public DSOStatisticsConfig getConfig();

  public boolean containsAction(StatisticRetrievalAction action);
}