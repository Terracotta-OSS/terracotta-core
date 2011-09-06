/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.statistics.gatherer;

import com.tc.statistics.StatisticData;
import com.tc.statistics.gatherer.exceptions.StatisticsGathererException;

public interface StatisticsGatherer {
  public void connect(String managerHostName, int managerPort) throws StatisticsGathererException;

  public void connect(String username, String password, String managerHostName, int managerPort)
      throws StatisticsGathererException;

  public void disconnect() throws StatisticsGathererException;

  public void reinitialize() throws StatisticsGathererException;

  public void createSession(String sessionId) throws StatisticsGathererException;

  public void closeSession() throws StatisticsGathererException;

  public String getActiveSessionId();

  public String[] getSupportedStatistics() throws StatisticsGathererException;

  public void enableStatistics(String[] names) throws StatisticsGathererException;

  public StatisticData[] captureStatistic(String name) throws StatisticsGathererException;

  public StatisticData[] retrieveStatisticData(String name) throws StatisticsGathererException;

  public void startCapturing() throws StatisticsGathererException;

  public void stopCapturing() throws StatisticsGathererException;

  public boolean isCapturing();

  public void setGlobalParam(String key, Object value) throws StatisticsGathererException;

  public Object getGlobalParam(String key) throws StatisticsGathererException;

  public void setSessionParam(String key, Object value) throws StatisticsGathererException;

  public Object getSessionParam(String key) throws StatisticsGathererException;

  public void addListener(StatisticsGathererListener listener);

  public void removeListener(StatisticsGathererListener listener);
}