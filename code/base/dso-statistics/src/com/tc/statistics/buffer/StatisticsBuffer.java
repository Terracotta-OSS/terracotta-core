/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.statistics.buffer;

import com.tc.statistics.StatisticData;
import com.tc.statistics.buffer.exceptions.StatisticsBufferException;
import com.tc.statistics.retrieval.StatisticsRetriever;

public interface StatisticsBuffer {

  public void open() throws StatisticsBufferException;

  public void close() throws StatisticsBufferException;

  public void reinitialize() throws StatisticsBufferException;

  public StatisticsRetriever createCaptureSession(String sessionId) throws StatisticsBufferException;

  public void startCapturing(String sessionId) throws StatisticsBufferException;

  public void stopCapturing(String sessionId) throws StatisticsBufferException;

  public void storeStatistic(StatisticData data) throws StatisticsBufferException;

  public void consumeStatistics(String sessionId, StatisticsConsumer consumer) throws StatisticsBufferException;

  public void fillInDefaultValues(StatisticData data);

  public void setDefaultAgentIp(String defaultAgentIp);

  public void setDefaultAgentDifferentiator(String defaultAgentDifferentiator);

  public String getDefaultAgentIp();

  public String getDefaultAgentDifferentiator();

  public String getDefaultNodeName();
  
  public void addListener(StatisticsBufferListener listener);

  public void removeListener(StatisticsBufferListener listener);
}