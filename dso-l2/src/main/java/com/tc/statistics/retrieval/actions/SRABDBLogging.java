/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.statistics.retrieval.actions;

import com.sleepycat.je.EnvironmentStats;
import com.tc.statistics.StatisticData;
import com.tc.statistics.StatisticRetrievalAction;
import com.tc.statistics.StatisticType;

public class SRABDBLogging implements StatisticRetrievalAction {
  public final static String  ACTION_NAME           = "db logging stats";

  // Logging stats
  // nFSyncs
  // nFSyncRequests
  // nFSyncTimeouts
  // nRepeatFaultReads
  // nTempBufferWrite
  // nRepeatIteratorReads
  // nFileOpens
  // nOpenFiles
  // totalLogSize

  private final static String FILE_SYNCS            = "nFSyncs";
  private final static String FILE_SYNC_REQUESTS    = "nFSyncRequests";
  private final static String FILE_SYNC_TIMEOUTS    = "nFSyncTimeouts";
  private final static String REPEAT_FAULT_THREADS  = "nRepeatFaultReads";
  private final static String TEMP_BUFFER_WRITE     = "nTempBufferWrites";
  private final static String REPEAT_ITERATOR_READS = "nRepeatIteratorReads";
  private final static String FILE_OPENS            = "nFileOpens";
  private final static String OPEN_FILES            = "nOpenFiles";
  private final static String FILE_LOGGING_SIZE     = "nFileLoggingSize";

  private long                nFSyncs               = 0;
  private long                nFSyncRequests        = 0;
  private long                nFSyncTimeouts        = 0;
  private long                nRepeatFaultReads     = 0;
  private long                nTempBufferWrites     = 0;
  private long                nRepeatIteratorReads  = 0;
  private int                 nFileOpens            = 0;
  private int                 nOpenFiles            = 0;
  private long                nFileLoggingSize      = 0;

  public String getName() {
    return ACTION_NAME;
  }

  public StatisticType getType() {
    return StatisticType.SNAPSHOT;
  }

  public StatisticData[] retrieveStatisticData() {
    StatisticData[] data = new StatisticData[9];
    data[0] = new StatisticData(ACTION_NAME, FILE_SYNCS, nFSyncs);
    data[1] = new StatisticData(ACTION_NAME, FILE_SYNC_REQUESTS, nFSyncRequests);
    data[2] = new StatisticData(ACTION_NAME, FILE_SYNC_TIMEOUTS, nFSyncTimeouts);
    data[3] = new StatisticData(ACTION_NAME, REPEAT_FAULT_THREADS, nRepeatFaultReads);
    data[4] = new StatisticData(ACTION_NAME, TEMP_BUFFER_WRITE, nTempBufferWrites);
    data[5] = new StatisticData(ACTION_NAME, REPEAT_ITERATOR_READS, nRepeatIteratorReads);
    data[6] = new StatisticData(ACTION_NAME, FILE_OPENS, (long) nFileOpens);
    data[7] = new StatisticData(ACTION_NAME, OPEN_FILES, (long) nOpenFiles);
    data[8] = new StatisticData(ACTION_NAME, FILE_LOGGING_SIZE, nFileLoggingSize);
    return data;
  }

  public void updateValues(EnvironmentStats envStats) {
    nFSyncs = envStats.getNFSyncs();
    nFSyncRequests = envStats.getNFSyncRequests();
    nFSyncTimeouts = envStats.getNFSyncTimeouts();
    nRepeatFaultReads = envStats.getNRepeatFaultReads();
    nTempBufferWrites = envStats.getNTempBufferWrites();
    nRepeatIteratorReads = envStats.getNRepeatIteratorReads();
    nFileOpens = envStats.getNFileOpens();
    nOpenFiles = envStats.getNOpenFiles();
    nFileLoggingSize = envStats.getTotalLogSize();
  }

}
