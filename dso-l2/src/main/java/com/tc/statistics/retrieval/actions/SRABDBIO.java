/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.statistics.retrieval.actions;

import com.sleepycat.je.EnvironmentStats;
import com.tc.statistics.StatisticData;
import com.tc.statistics.StatisticRetrievalAction;
import com.tc.statistics.StatisticType;

public class SRABDBIO implements StatisticRetrievalAction {
  public final static String  ACTION_NAME            = "db io stats";

  // IO Stats
  // nRandomReads
  // nRandomWrites
  // nSequentialReads
  // nSequentialWrites
  // nRandomReadBytes
  // nRandomWriteBytes
  // nSequentialReadBytes
  // nSequentialWriteBytes

  private final static String RANDOM_READS           = "nRandomReads";
  private final static String RANDOM_WRITES          = "nRandomWrites";
  private final static String SEQUENTIAL_READS       = "nSequentialReads";
  private final static String SEQUENTIAL_WRITES      = "nSequentialWrites";
  private final static String RANDOM_READ_BYTES      = "nRandomReadBytes";
  private final static String RANDOM_WRITE_BYTES     = "nRandomWriteBytes";
  private final static String SEQUENTIAL_READ_BYTES  = "nSequentialReadBytes";
  private final static String SEQUENTIAL_WRITE_BYTES = "nSequentialWriteBytes";

  private long                nRandomReads           = 0;
  private long                nRandomWrites          = 0;
  private long                nSequentialReads       = 0;
  private long                nSequentialWrites      = 0;
  private long                nRandomReadBytes       = 0;
  private long                nRandomWriteBytes      = 0;
  private long                nSequentialReadBytes   = 0;
  private long                nSequentialWriteBytes  = 0;

  public String getName() {
    return ACTION_NAME;
  }

  public StatisticType getType() {
    return StatisticType.SNAPSHOT;
  }

  public StatisticData[] retrieveStatisticData() {
    StatisticData[] data = new StatisticData[8];
    data[0] = new StatisticData(ACTION_NAME, RANDOM_READS, nRandomReads);
    data[1] = new StatisticData(ACTION_NAME, RANDOM_WRITES, nRandomWrites);
    data[2] = new StatisticData(ACTION_NAME, SEQUENTIAL_READS, nSequentialReads);
    data[3] = new StatisticData(ACTION_NAME, SEQUENTIAL_WRITES, nSequentialWrites);
    data[4] = new StatisticData(ACTION_NAME, RANDOM_READ_BYTES, nRandomReadBytes);
    data[5] = new StatisticData(ACTION_NAME, RANDOM_WRITE_BYTES, nRandomWriteBytes);
    data[6] = new StatisticData(ACTION_NAME, SEQUENTIAL_READ_BYTES, nSequentialReadBytes);
    data[7] = new StatisticData(ACTION_NAME, SEQUENTIAL_WRITE_BYTES, nSequentialWriteBytes);
    return data;
  }

  public void updateValues(EnvironmentStats envStats) {
    nRandomReads = envStats.getNRandomReads();
    nRandomWrites = envStats.getNRandomWrites();
    nSequentialReads = envStats.getNSequentialReads();
    nSequentialWrites = envStats.getNSequentialWrites();
    nRandomReadBytes = envStats.getNRandomReadBytes();
    nRandomWriteBytes = envStats.getNRandomWriteBytes();
    nSequentialReadBytes = envStats.getNSequentialReadBytes();
    nSequentialWriteBytes = envStats.getNSequentialWriteBytes();
  }
}
