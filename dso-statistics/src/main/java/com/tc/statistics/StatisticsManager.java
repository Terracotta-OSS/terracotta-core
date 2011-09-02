/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.statistics;

public interface StatisticsManager  {
  public void enable();

  public void disable();

  public void reinitialize();

  public String[] getSupportedStatistics();

  public void createSession(String sessionId);

  public void disableAllStatistics(String sessionId);

  public boolean enableStatistic(String sessionId, String name);

  public String getStatisticType(String name);

  public StatisticData[] captureStatistic(String sessionId, String name);

  public StatisticData[] retrieveStatisticData(String name);

  public void startCapturing(String sessionId);

  public void stopCapturing(String sessionId);

  public void setGlobalParam(String key, Object value);

  public Object getGlobalParam(String key);

  public void setSessionParam(String sessionId, String key, Object value);

  public Object getSessionParam(String sessionId, String key);
}