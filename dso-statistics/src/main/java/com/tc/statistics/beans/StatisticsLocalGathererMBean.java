/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.statistics.beans;

import com.tc.management.TerracottaMBean;
import com.tc.statistics.StatisticData;

public interface StatisticsLocalGathererMBean extends TerracottaMBean {
  public final static String STATISTICS_LOCALGATHERER_STARTEDUP_TYPE = "tc.statistics.localgatherer.startedup";
  public final static String STATISTICS_LOCALGATHERER_SHUTDOWN_TYPE = "tc.statistics.localgatherer.shutdown";
  public final static String STATISTICS_LOCALGATHERER_REINITIALIZED_TYPE = "tc.statistics.localgatherer.reinitialized";
  public final static String STATISTICS_LOCALGATHERER_CAPTURING_STARTED_TYPE = "tc.statistics.localgatherer.capturing.started";
  public final static String STATISTICS_LOCALGATHERER_CAPTURING_STOPPED_TYPE = "tc.statistics.localgatherer.capturing.stopped";
  public final static String STATISTICS_LOCALGATHERER_SESSION_CREATED_TYPE = "tc.statistics.localgatherer.session.created";
  public final static String STATISTICS_LOCALGATHERER_SESSION_CLOSED_TYPE = "tc.statistics.localgatherer.session.closed";
  public final static String STATISTICS_LOCALGATHERER_SESSION_CLEARED_TYPE = "tc.statistics.localgatherer.session.cleared";
  public final static String STATISTICS_LOCALGATHERER_ALLSESSIONS_CLEARED_TYPE = "tc.statistics.localgatherer.allsessions.cleared";
  public final static String STATISTICS_LOCALGATHERER_STORE_OPENED_TYPE = "tc.statistics.localgatherer.store.opened";
  public final static String STATISTICS_LOCALGATHERER_STORE_CLOSED_TYPE = "tc.statistics.localgatherer.store.closed";
  public final static String STATISTICS_LOCALGATHERER_STATISTICS_ENABLED_TYPE = "tc.statistics.localgatherer.statistics.enabled";

  public boolean isActive();

  public void startup();

  public void shutdown();

  public void reinitialize();

  public void createSession(String sessionId);

  public void closeSession();

  public String getActiveSessionId();

  public String[] getAvailableSessionIds();

  public String[] getSupportedStatistics();

  public void enableStatistics(String[] names);

  public StatisticData[] captureStatistic(String name);

  public StatisticData[] retrieveStatisticData(String name);

  public void startCapturing();

  public void stopCapturing();

  public boolean isCapturing();

  public void setGlobalParam(String key, Object value);

  public Object getGlobalParam(String key);

  public void setSessionParam(String key, Object value);

  public Object getSessionParam(String key);

  public void clearStatistics(String sessionId);

  public void clearAllStatistics();

  public void setUsername(String username);

  public void setPassword(String password);
}