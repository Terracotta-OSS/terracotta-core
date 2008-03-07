/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.statistics.retrieval;

import com.tc.statistics.StatisticRetrievalAction;

import java.util.Collection;

public interface StatisticsRetrievalRegistry {
  public void removeAllActionInstances();

  public Collection getSupportedStatistics();

  public Collection getRegisteredActionInstances();
  
  public StatisticRetrievalAction getActionInstance(String name);

  public void registerActionInstance(StatisticRetrievalAction action);

  public void registerActionInstance(String sraClassName);
}