/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.statistics.retrieval.actions;

import com.sleepycat.je.EnvironmentStats;
import com.tc.statistics.StatisticData;
import com.tc.statistics.StatisticRetrievalAction;
import com.tc.statistics.StatisticType;

public class SRABDBCache implements StatisticRetrievalAction {
  public final static String  ACTION_NAME               = "db cache stats";

  // Cache stats
  // nNotResident
  // nCacheMiss
  // nLogBuffers
  // bufferBytes
  // dataBytes
  // adminBytes
  // lockBytes
  // cacheTotalBytes
  // sharedCacheTotalBytes
  // nSharedCacheEnvironments

  private final static String NOT_RESIDENT              = "nNotResident";
  private final static String CACHE_MISS                = "nCacheMiss";
  private final static String LOG_BUFFERS               = "nLogBuffers";
  private final static String BUFFER_BYTES              = "bufferBytes";
  private final static String DATA_BYTES                = "dataBytes";
  private final static String ADMIN_BYTES               = "adminBytes";
  private final static String LOCK_BYTES                = "lockBytes";
  private final static String CACHE_TOTAL_BYTES         = "cacheTotalBytes";
  private final static String SHARED_CACHE_TOTAL_BYTES  = "sharedCacheTotalBytes";
  private final static String SHARED_CACHE_ENVIRONMENTS = "nSharedCacheEnvironments";

  private long                nNotResident              = 0;
  private long                nCacheMiss                = 0;
  private int                 nLogBuffers               = 0;
  private long                bufferBytes               = 0;
  private long                dataBytes                 = 0;
  private long                adminBytes                = 0;
  private long                lockBytes                 = 0;
  private long                cacheTotalBytes           = 0;
  private long                sharedCacheTotalBytes     = 0;
  private int                 nSharedCacheEnvironments  = 0;

  public String getName() {
    return ACTION_NAME;
  }

  public StatisticType getType() {
    return StatisticType.SNAPSHOT;
  }

  public StatisticData[] retrieveStatisticData() {
    StatisticData[] data = new StatisticData[10];
    data[0] = new StatisticData(ACTION_NAME, NOT_RESIDENT, nNotResident);
    data[1] = new StatisticData(ACTION_NAME, CACHE_MISS, nCacheMiss);
    data[2] = new StatisticData(ACTION_NAME, LOG_BUFFERS, (long) nLogBuffers);
    data[3] = new StatisticData(ACTION_NAME, BUFFER_BYTES, bufferBytes);
    data[4] = new StatisticData(ACTION_NAME, DATA_BYTES, dataBytes);
    data[5] = new StatisticData(ACTION_NAME, ADMIN_BYTES, adminBytes);
    data[6] = new StatisticData(ACTION_NAME, LOCK_BYTES, lockBytes);
    data[7] = new StatisticData(ACTION_NAME, CACHE_TOTAL_BYTES, cacheTotalBytes);
    data[8] = new StatisticData(ACTION_NAME, SHARED_CACHE_TOTAL_BYTES, sharedCacheTotalBytes);
    data[9] = new StatisticData(ACTION_NAME, SHARED_CACHE_ENVIRONMENTS, (long) nSharedCacheEnvironments);
    return data;
  }

  public void updateValues(EnvironmentStats envStats) {
    nNotResident = envStats.getNNotResident();
    nCacheMiss = envStats.getNCacheMiss();
    nLogBuffers = envStats.getNLogBuffers();
    bufferBytes = envStats.getBufferBytes();
    dataBytes = envStats.getDataBytes();
    adminBytes = envStats.getAdminBytes();
    lockBytes = envStats.getLockBytes();
    cacheTotalBytes = envStats.getCacheTotalBytes();
    sharedCacheTotalBytes = envStats.getSharedCacheTotalBytes();
    nSharedCacheEnvironments = envStats.getNSharedCacheEnvironments();
  }

}
