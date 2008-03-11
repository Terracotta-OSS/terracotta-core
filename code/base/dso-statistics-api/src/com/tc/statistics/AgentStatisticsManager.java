/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.statistics;

import java.util.List;

public interface AgentStatisticsManager extends StatisticsManager {

  public List getActiveSessionsForAction(String actionName);

  public List getActiveSessionsForAction(StatisticRetrievalAction action);
}
