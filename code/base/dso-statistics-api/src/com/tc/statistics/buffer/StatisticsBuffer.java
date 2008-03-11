/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.statistics.buffer;

import com.tc.statistics.StatisticData;
import com.tc.statistics.buffer.exceptions.TCStatisticsBufferException;
import com.tc.statistics.retrieval.StatisticsRetriever;

public interface StatisticsBuffer {
  public void open() throws TCStatisticsBufferException;

  public void close() throws TCStatisticsBufferException;

  public void reinitialize() throws TCStatisticsBufferException;

  public StatisticsRetriever createCaptureSession(String sessionId) throws TCStatisticsBufferException;

  public void startCapturing(String sessionId) throws TCStatisticsBufferException;

  public void stopCapturing(String sessionId) throws TCStatisticsBufferException;

  public void storeStatistic(StatisticData data) throws TCStatisticsBufferException;

  public void consumeStatistics(String sessionId, StatisticsConsumer consumer) throws TCStatisticsBufferException;

  public void setDefaultAgentIp(String defaultAgentIp);

  public void setDefaultAgentDifferentiator(String defaultAgentDifferentiator);

  public String getDefaultAgentIp();

  public String getDefaultAgentDifferentiator();

  public void addListener(StatisticsBufferListener listener);

  public void removeListener(StatisticsBufferListener listener);
}