/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.statistics.gatherer;

import com.tc.statistics.gatherer.exceptions.TCStatisticsGathererException;
import com.tc.statistics.StatisticData;

public interface StatisticsGatherer {
  public void connect(String managerHostName, int managerPort) throws TCStatisticsGathererException;

  public void disconnect() throws TCStatisticsGathererException;

  public void reinitialize() throws TCStatisticsGathererException;

  public void createSession(String sessionId) throws TCStatisticsGathererException;

  public void closeSession() throws TCStatisticsGathererException;

  public String getActiveSessionId();

  public String[] getSupportedStatistics() throws TCStatisticsGathererException;

  public void enableStatistics(String[] names) throws TCStatisticsGathererException;

  public StatisticData[] captureStatistic(String name) throws TCStatisticsGathererException;

  public void startCapturing() throws TCStatisticsGathererException;

  public void stopCapturing() throws TCStatisticsGathererException;

  public void setGlobalParam(String key, Object value) throws TCStatisticsGathererException;

  public Object getGlobalParam(String key) throws TCStatisticsGathererException;

  public void setSessionParam(String key, Object value) throws TCStatisticsGathererException;

  public Object getSessionParam(String key) throws TCStatisticsGathererException;

  public void addListener(StatisticsGathererListener listener);

  public void removeListener(StatisticsGathererListener listener);
}