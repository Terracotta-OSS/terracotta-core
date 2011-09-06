/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.statistics.retrieval.actions;

import com.tc.async.api.Stage;
import com.tc.async.api.StageManager;
import com.tc.async.api.StageQueueStats;
import com.tc.statistics.DynamicSRA;
import com.tc.statistics.StatisticData;
import com.tc.statistics.StatisticType;
import com.tc.stats.Stats;
import com.tc.util.Assert;

import java.util.Collection;
import java.util.Iterator;

/**
 * This statistics gives the depths of all the {@link Stage} sinks in a {@link StageManager}.
 * <p/>
 * It will contain a {@link StatisticData} for each of the {@link Stage} present in the {@link StageManager}
 * <p/>
 * This statistic is a {@link DynamicSRA} meaning that the system will turn on the collection of this statistic only
 * when at least one session has enabled this statistic. When there are no more sessions using this statistic,
 * collection of this statistic will be turned off.
 */
public class SRAStageQueueDepths implements DynamicSRA {

  public static final String ACTION_NAME = "stage queue depth";

  private final StageManager stageManager;

  public SRAStageQueueDepths(final StageManager stageManager) {
    Assert.assertNotNull(stageManager);
    this.stageManager = stageManager;
  }

  public String getName() {
    return ACTION_NAME;
  }

  public StatisticType getType() {
    return StatisticType.SNAPSHOT;
  }

  public StatisticData[] retrieveStatisticData() {
    if (!isStatisticsCollectionEnabled()) { return EMPTY_STATISTIC_DATA; }
    Stats[] stats = stageManager.getStats();
    StatisticData[] data = new StatisticData[stats.length];
    for (int i = 0; i < stats.length; i++) {
      StageQueueStats stageStat = (StageQueueStats) stats[i];
      data[i] = new StatisticData(ACTION_NAME, stageStat.getName(), Long.valueOf(stageStat.getDepth()));
    }
    return data;
  }

  private boolean isStatisticsCollectionEnabled() {
    synchronized (stageManager) {
      Collection stages = stageManager.getStages();
      for (Iterator it = stages.iterator(); it.hasNext();) {
        if (((Stage) it.next()).getSink().isStatsCollectionEnabled()) { return true; }
      }
    }
    return false;
  }

  public void enableStatisticCollection() {
    synchronized (stageManager) {
      Collection stages = stageManager.getStages();
      for (Iterator it = stages.iterator(); it.hasNext();) {
        ((Stage) it.next()).getSink().enableStatsCollection(true);
      }
    }
  }

  public void disableStatisticCollection() {
    synchronized (stageManager) {
      Collection stages = stageManager.getStages();
      for (Iterator it = stages.iterator(); it.hasNext();) {
        ((Stage) it.next()).getSink().enableStatsCollection(false);
      }
    }
  }
}
