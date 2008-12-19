/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.statistics.retrieval.actions;

import com.tc.management.beans.sessions.SessionStatisticsMBean;
import com.tc.object.bytecode.ManagerUtil;
import com.tc.statistics.StatisticData;
import com.tc.statistics.StatisticRetrievalAction;
import com.tc.statistics.StatisticType;

public class SRAHttpSessions implements StatisticRetrievalAction {
  public static final String ACTION_NAME = "http sessions";

  public static final String CREATED = ACTION_NAME + ": created";
  public static final String CREATED_RATE = ACTION_NAME + ": creation rate (per minute)";
  public static final String DESTROYED = ACTION_NAME + ": destroyed";
  public static final String DESTROYED_RATE = ACTION_NAME + ": destruction rate (per minute)";
  public static final String REQUEST = ACTION_NAME + ": requests";
  public static final String REQUEST_RATE = ACTION_NAME + ": request rate (per minute)";
  
  public String getName() {
    return ACTION_NAME;
  }

  public StatisticType getType() {
    return StatisticType.SNAPSHOT;
  }

  public StatisticData[] retrieveStatisticData() {
    SessionStatisticsMBean bean = ManagerUtil.getHttpSessionMonitor();

    if (bean == null)
      return EMPTY_STATISTIC_DATA;

//    if (!bean.isEnabled())
//      return EMPTY_STATISTIC_DATA;
    
    return new StatisticData[] {
      new StatisticData(CREATED, Long.valueOf(bean.getCreatedSessionCount())),
      new StatisticData(CREATED_RATE, Long.valueOf(bean.getSessionCreationRatePerMinute())),
      new StatisticData(DESTROYED, Long.valueOf(bean.getDestroyedSessionCount())),
      new StatisticData(DESTROYED_RATE, Long.valueOf(bean.getSessionDestructionRatePerMinute())),
      new StatisticData(REQUEST, Long.valueOf(bean.getRequestCount())),
      new StatisticData(REQUEST_RATE, Long.valueOf(bean.getRequestRatePerSecond()))
    };
  }
}
