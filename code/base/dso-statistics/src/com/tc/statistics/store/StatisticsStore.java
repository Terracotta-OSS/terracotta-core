/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.statistics.store;

import com.tc.statistics.StatisticData;
import com.tc.statistics.store.exceptions.StatisticsStoreException;

import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;

public interface StatisticsStore {

  public void open() throws StatisticsStoreException;

  public void close() throws StatisticsStoreException;

  public void recreateCaches() throws StatisticsStoreException;

  public void reinitialize() throws StatisticsStoreException;

  public void storeStatistic(StatisticData data) throws StatisticsStoreException;

  public void retrieveStatistics(StatisticsRetrievalCriteria criteria, StatisticDataUser user) throws StatisticsStoreException;

  public String[] getAvailableSessionIds() throws StatisticsStoreException;

  public String[] getAvailableAgentDifferentiators(String sessionId) throws StatisticsStoreException;

  public void clearStatistics(String sessionId) throws StatisticsStoreException;

  public void clearAllStatistics() throws StatisticsStoreException;

  public void importCsvStatistics(Reader reader, StatisticsStoreImportListener listener) throws StatisticsStoreException;

  public void retrieveStatisticsAsCsvStream(OutputStream os, String filenameBase, StatisticsRetrievalCriteria criteria, boolean zipContents) throws StatisticsStoreException;

  public void aggregateStatisticsData(Writer writer, TextualDataFormat format, String sessionId, String agentDifferentiator, String[] names, String[] elements, Long interval) throws StatisticsStoreException;

  public void addListener(StatisticsStoreListener listener);

  public void removeListener(StatisticsStoreListener listener);
}
