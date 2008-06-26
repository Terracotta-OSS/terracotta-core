/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.statistics.mock;

import com.tc.statistics.AgentStatisticsManager;
import com.tc.statistics.StatisticsAgentSubSystem;
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

  public void cleanup() throws Exception {
    //
  }
}
