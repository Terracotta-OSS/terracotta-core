/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.statistics.retrieval.actions;

import com.sleepycat.je.EnvironmentStats;
import com.tc.objectserver.persistence.db.TCDatabaseException;
import com.tc.objectserver.storage.api.DBEnvironment;
import com.tc.objectserver.storage.berkeleydb.BerkeleyDBEnvironment;
import com.tc.statistics.StatisticData;
import com.tc.statistics.StatisticRetrievalAction;
import com.tc.statistics.StatisticType;

public class SRAForBerkeleyDB implements StatisticRetrievalAction {
  public final static String          ACTION_NAME = "db stats";

  private final SRABDBLogging         sraLogging;
  private final SRABDBCache           sraCache;
  private final SRABDBCleaner         sraCleaner;
  private final SRABDBIO              sraIo;

  private final BerkeleyDBEnvironment dbEnv;

  public SRAForBerkeleyDB(DBEnvironment env) {
    sraLogging = new SRABDBLogging();
    sraCache = new SRABDBCache();
    sraCleaner = new SRABDBCleaner();
    sraIo = new SRABDBIO();
    dbEnv = (BerkeleyDBEnvironment) env;
  }

  private void forceUpdate() {
    if (dbEnv == null) return;

    EnvironmentStats stats;
    try {
      stats = dbEnv.getStats();
    } catch (TCDatabaseException e) {
      return;
    }
    if (stats == null) return;
    updateValues(stats);
  }

  private void updateValues(EnvironmentStats envStats) {
    sraLogging.updateValues(envStats);
    sraCache.updateValues(envStats);
    sraCleaner.updateValues(envStats);
    sraIo.updateValues(envStats);
  }

  public StatisticRetrievalAction[] getBDBSRAs() {
    return new StatisticRetrievalAction[] { sraLogging, sraCache, sraCleaner, sraIo };
  }

  public String getName() {
    return ACTION_NAME;
  }

  public StatisticType getType() {
    return StatisticType.SNAPSHOT;
  }

  public StatisticData[] retrieveStatisticData() {
    forceUpdate();
    StatisticData[] dataCache = sraCache.retrieveStatisticData();
    StatisticData[] dataLogging = sraLogging.retrieveStatisticData();
    StatisticData[] dataCleaner = sraCleaner.retrieveStatisticData();
    StatisticData[] dataIO = sraIo.retrieveStatisticData();
    int len = dataCache.length + dataLogging.length + dataCleaner.length + dataIO.length;
    StatisticData[] datas = new StatisticData[len];

    int index = 0;
    index += setFromTo(datas, dataCache, index);
    index += setFromTo(datas, dataLogging, index);
    index += setFromTo(datas, dataCleaner, index);
    index += setFromTo(datas, dataIO, index);

    return datas;
  }

  private int setFromTo(StatisticData[] to, StatisticData[] from, int start) {
    for (int i = 0; i < from.length; i++) {
      to[start + i] = from[i];
    }
    return from.length;
  }
}
