/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.dgc.impl;

import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.objectserver.dgc.api.GarbageCollectionInfo;
import com.tc.statistics.AgentStatisticsManager;
import com.tc.statistics.StatisticData;
import com.tc.statistics.StatisticsAgentSubSystem;
import com.tc.statistics.exceptions.AgentStatisticsManagerException;
import com.tc.statistics.retrieval.actions.SRADistributedGC;

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
  public void garbageCollectorCompleted(GarbageCollectionInfo info) {
    if (info.isInlineDGC()) { return; }
    if (statisticsAgentSubSystem.isActive()) {
      storeGCInfo(info);
    }
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
    System.err.println(gcInfo);
    List<StatisticData> datas = new ArrayList<StatisticData>();
    datas.add(new StatisticData(DISTRIBUTED_GC_STATISTICS, SRADistributedGC.ITERATION_ELEMENT, (long) gcInfo
        .getIteration()));
    datas.add(new StatisticData(DISTRIBUTED_GC_STATISTICS, SRADistributedGC.TYPE_ELEMENT,
                                gcInfo.isFullGC() ? SRADistributedGC.TYPE_FULL : SRADistributedGC.TYPE_YOUNG_GEN));
    datas.add(new StatisticData(DISTRIBUTED_GC_STATISTICS, SRADistributedGC.START_TIME_ELEMENT, gcInfo.getStartTime()));
    datas.add(new StatisticData(DISTRIBUTED_GC_STATISTICS, SRADistributedGC.ELAPSED_TIME_ELEMENT, gcInfo
        .getElapsedTime()));
    datas.add(new StatisticData(DISTRIBUTED_GC_STATISTICS, SRADistributedGC.BEGIN_OBJECT_COUNT_ELEMENT, Long
        .valueOf(gcInfo.getBeginObjectCount())));
    datas.add(new StatisticData(DISTRIBUTED_GC_STATISTICS, SRADistributedGC.CANDIDATE_OBJECT_COUNT_ELEMENT, Long
        .valueOf(gcInfo.getCandidateGarbageCount())));
    datas.add(new StatisticData(DISTRIBUTED_GC_STATISTICS, SRADistributedGC.ACTUAL_GARBAGE_COUNT_ELEMENT, Long
        .valueOf(gcInfo.getActualGarbageCount())));
    datas.add(new StatisticData(DISTRIBUTED_GC_STATISTICS, SRADistributedGC.PAUSE_TIME_ELEMENT, gcInfo
        .getPausedStageTime()));
    datas.add(new StatisticData(DISTRIBUTED_GC_STATISTICS, SRADistributedGC.MARK_TIME_ELEMENT, gcInfo
        .getMarkStageTime()));
    datas.add(new StatisticData(DISTRIBUTED_GC_STATISTICS, SRADistributedGC.DELETE_TIME_ELEMENT, gcInfo
        .getDeleteStageTime()));
    return datas.toArray(new StatisticData[datas.size()]);
  }

  private synchronized void storeStatisticsDatas(Date moment, Collection sessions, StatisticData[] datas) {
    try {
      for (Iterator sessionsIterator = sessions.iterator(); sessionsIterator.hasNext();) {
        String session = (String) sessionsIterator.next();
        for (StatisticData data : datas) {
          statisticsAgentSubSystem.getStatisticsManager().injectStatisticData(session, data.moment(moment));
        }
      }
    } catch (AgentStatisticsManagerException e) {
      logger.error("Unexpected error while trying to store Cache Objects Evict Request statistics statistics.", e);
    }
  }

}
