/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.statistics.retrieval.actions;

import com.tc.management.beans.sessions.SessionStatisticsMBean;
import com.tc.object.bytecode.ManagerUtil;
import com.tc.statistics.StatisticData;
import com.tc.statistics.StatisticRetrievalAction;
import com.tc.statistics.StatisticType;

public class SRAHttpSessions implements StatisticRetrievalAction {
  public static final String ACTION_NAME    = "http sessions";

  public static final String CREATED        = "created";
  public static final String CREATED_RATE   = "creation rate (per minute)";
  public static final String DESTROYED      = "destroyed";
  public static final String DESTROYED_RATE = "destruction rate (per minute)";
  public static final String REQUESTS       = "requests";
  public static final String REQUEST_RATE   = "request rate (per minute)";

  public String getName() {
    return ACTION_NAME;
  }

  public StatisticType getType() {
    return StatisticType.SNAPSHOT;
  }

  public StatisticData[] retrieveStatisticData() {
    SessionStatisticsMBean bean = ManagerUtil.getHttpSessionMonitor();

    if (bean == null) return EMPTY_STATISTIC_DATA;

    // if (!bean.isEnabled())
    // return EMPTY_STATISTIC_DATA;

    return new StatisticData[] { new StatisticData(ACTION_NAME, CREATED, Long.valueOf(bean.getCreatedSessionCount())),
        new StatisticData(ACTION_NAME, CREATED_RATE, Long.valueOf(bean.getSessionCreationRatePerMinute())),
        new StatisticData(ACTION_NAME, DESTROYED, Long.valueOf(bean.getDestroyedSessionCount())),
        new StatisticData(ACTION_NAME, DESTROYED_RATE, Long.valueOf(bean.getSessionDestructionRatePerMinute())),
        new StatisticData(ACTION_NAME, REQUESTS, Long.valueOf(bean.getRequestCount())),
        new StatisticData(ACTION_NAME, REQUEST_RATE, Long.valueOf(bean.getRequestRatePerSecond())) };
  }
}
