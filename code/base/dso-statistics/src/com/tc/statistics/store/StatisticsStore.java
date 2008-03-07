/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.statistics.store;

import com.tc.statistics.StatisticData;
import com.tc.statistics.store.exceptions.TCStatisticsStoreException;

import java.io.Reader;

public interface StatisticsStore {
  public void open() throws TCStatisticsStoreException;

  public void close() throws TCStatisticsStoreException;

  public void recreateCaches() throws TCStatisticsStoreException;

  public void reinitialize() throws TCStatisticsStoreException;

  public void storeStatistic(StatisticData data) throws TCStatisticsStoreException;

  public void retrieveStatistics(StatisticsRetrievalCriteria criteria, StatisticDataUser user) throws TCStatisticsStoreException;

  public String[] getAvailableSessionIds() throws TCStatisticsStoreException;

  public void clearStatistics(String sessionId) throws TCStatisticsStoreException;

  public void clearAllStatistics() throws TCStatisticsStoreException;

  public void importCsvStatistics(Reader reader, StatisticsStoreImportListener listener) throws TCStatisticsStoreException;

  public void addListener(StatisticsStoreListener listener);

  public void removeListener(StatisticsStoreListener listener);
}