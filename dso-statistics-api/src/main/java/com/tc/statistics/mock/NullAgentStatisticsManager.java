/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.statistics.mock;

import com.tc.statistics.AgentStatisticsManager;
import com.tc.statistics.StatisticData;

import java.util.Collection;
import java.util.Collections;

public class NullAgentStatisticsManager implements AgentStatisticsManager {

  public static final NullAgentStatisticsManager INSTANCE = new NullAgentStatisticsManager();

  public Collection getActiveSessionIDsForAction(String actionName) {
    return Collections.EMPTY_LIST;
  }

  public void injectStatisticData(String sessionId, StatisticData data) {
    //no-op
  }
}
