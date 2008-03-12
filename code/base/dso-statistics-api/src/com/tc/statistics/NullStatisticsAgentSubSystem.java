/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.statistics;

import com.tc.statistics.retrieval.NullStatisticsRetrievalRegistry;
import com.tc.statistics.retrieval.StatisticsRetrievalRegistry;

public class NullStatisticsAgentSubSystem implements StatisticsAgentSubSystem {

  public boolean isActive() {
    return false;
  }

  public StatisticsRetrievalRegistry getStatisticsRetrievalRegistry() {
    return NullStatisticsRetrievalRegistry.INSTANCE;
  }

  public AgentStatisticsManager getStatisticsManager() {
    return NullAgentStatisticsManager.INSTANCE;
  }
}
