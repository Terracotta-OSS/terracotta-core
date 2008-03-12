/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.statistics;

import com.tc.statistics.exceptions.AgentStatisticsManagerException;

import java.util.Collection;
import java.util.Collections;

public class NullAgentStatisticsManager implements AgentStatisticsManager {

  public static final NullAgentStatisticsManager INSTANCE = new NullAgentStatisticsManager();

  public Collection getActiveSessionIDsForAction(String actionName) {
    return Collections.EMPTY_LIST;
  }

  public void injectStatisticData(String sessionId, StatisticData data) throws AgentStatisticsManagerException {
    //no-op
  }
}
