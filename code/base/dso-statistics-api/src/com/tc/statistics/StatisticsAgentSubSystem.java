/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.statistics;

import com.tc.statistics.retrieval.StatisticsRetrievalRegistry;

public interface StatisticsAgentSubSystem {
  public boolean isActive();
  public StatisticsRetrievalRegistry getStatisticsRetrievalRegistry();
  public AgentStatisticsManager getStatisticsManager();
}