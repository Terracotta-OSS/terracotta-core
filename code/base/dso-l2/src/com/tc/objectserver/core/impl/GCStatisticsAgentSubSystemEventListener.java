/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.core.impl;

import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.objectserver.core.api.GarbageCollectionInfo;
import com.tc.statistics.AgentStatisticsManager;
import com.tc.statistics.StatisticData;
import com.tc.statistics.StatisticsAgentSubSystem;
import com.tc.statistics.exceptions.AgentStatisticsManagerException;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

public class GCStatisticsAgentSubSystemEventListener extends GarbageCollectorEventListenerAdapter {

  public static final String             DISTRIBUTED_GC_STATISTICS = "distributed gc";

  private final StatisticsAgentSubSystem statisticsAgentSubSystem;

  private final TCLogger                 logger                    = TCLogging
                                                                       .getLogger(GCStatisticsAgentSubSystemEventListener.class);

  public GCStatisticsAgentSubSystemEventListener(StatisticsAgentSubSystem statisticsAgentSubSystem) {
    this.statisticsAgentSubSystem = statisticsAgentSubSystem;
  }

  @Override
  public void garbageCollectorCycleCompleted(GarbageCollectionInfo info) {
    storeGCInfo(info);
  }

  private void storeGCInfo(GarbageCollectionInfo gcInfo) {
    Date moment = new Date();
    AgentStatisticsManager agentStatisticsManager = statisticsAgentSubSystem.getStatisticsManager();
    Collection sessions = agentStatisticsManager.getActiveSessionIDsForAction(DISTRIBUTED_GC_STATISTICS);
    if (sessions != null && sessions.size() > 0) {
      StatisticData[] datas = getGCStatisticsData(gcInfo);
      storeStatisticsDatas(moment, sessions, datas);
    }
  }

  private StatisticData[] getGCStatisticsData(GarbageCollectionInfo gcInfo) {
    List<StatisticData> datas = new ArrayList<StatisticData>();
    datas.add(new StatisticData(DISTRIBUTED_GC_STATISTICS, "iteration", (long) gcInfo.getIteration()));
    datas.add(new StatisticData(DISTRIBUTED_GC_STATISTICS, "type", gcInfo.isYoungGen() ? "Young" : "Full"));
    datas.add(new StatisticData(DISTRIBUTED_GC_STATISTICS, "start time", gcInfo.getStartTime()));
    datas.add(new StatisticData(DISTRIBUTED_GC_STATISTICS, "elapsed time", gcInfo.getElapsedTime()));
    datas.add(new StatisticData(DISTRIBUTED_GC_STATISTICS, "begin object count", gcInfo.getBeginObjectCount()));
    datas
        .add(new StatisticData(DISTRIBUTED_GC_STATISTICS, "candidate garbage count", gcInfo.getCandidateGarbageCount()));
    datas.add(new StatisticData(DISTRIBUTED_GC_STATISTICS, "actual garbage count", gcInfo.getActualGarbageCount()));
    datas.add(new StatisticData(DISTRIBUTED_GC_STATISTICS, "pauseTime", gcInfo.getPausedStageTime()));
    datas.add(new StatisticData(DISTRIBUTED_GC_STATISTICS, "deleteTime", gcInfo.getDeleteStageTime()));
    return datas.toArray(new StatisticData[datas.size()]);
  }

  private synchronized void storeStatisticsDatas(Date moment, Collection sessions, StatisticData[] datas) {
    try {
      for (Iterator sessionsIterator = sessions.iterator(); sessionsIterator.hasNext();) {
        String session = (String) sessionsIterator.next();
        for (int i = 0; i < datas.length; i++) {
          StatisticData data = datas[i];
          statisticsAgentSubSystem.getStatisticsManager().injectStatisticData(session, data.moment(moment));
        }
      }
    } catch (AgentStatisticsManagerException e) {
      logger.error("Unexpected error while trying to store Cache Objects Evict Request statistics statistics.", e);
    }
  }

}
