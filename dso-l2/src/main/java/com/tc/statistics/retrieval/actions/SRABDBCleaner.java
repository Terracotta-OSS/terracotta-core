/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.statistics.retrieval.actions;

import com.sleepycat.je.EnvironmentStats;
import com.tc.statistics.StatisticData;
import com.tc.statistics.StatisticRetrievalAction;
import com.tc.statistics.StatisticType;

public class SRABDBCleaner implements StatisticRetrievalAction {
  public final static String  ACTION_NAME       = "db cleaner stats";

  // Cleaner stats
  // cleanerBacklog
  // nCleanerRuns
  // nCleanerDeletions

  private final static String CLEANER_BACKLOG   = "cleanerBacklog";
  private final static String CLEANER_RUNS      = "nCleanerRuns";
  private final static String CLEANER_DELETIONS = "nCleanerDeletions";

  private int                 cleanerBacklog    = 0;
  private long                nCleanerRuns      = 0;
  private long                nCleanerDeletions = 0;

  public String getName() {
    return ACTION_NAME;
  }

  public StatisticType getType() {
    return StatisticType.SNAPSHOT;
  }

  public StatisticData[] retrieveStatisticData() {
    StatisticData[] data = new StatisticData[3];
    data[0] = new StatisticData(ACTION_NAME, CLEANER_BACKLOG, (long) cleanerBacklog);
    data[1] = new StatisticData(ACTION_NAME, CLEANER_RUNS, nCleanerRuns);
    data[2] = new StatisticData(ACTION_NAME, CLEANER_DELETIONS, nCleanerDeletions);
    return data;
  }

  public void updateValues(EnvironmentStats envStats) {
    cleanerBacklog = envStats.getCleanerBacklog();
    nCleanerRuns = envStats.getNCleanerRuns();
    nCleanerDeletions = envStats.getNCleanerDeletions();
  }

}
