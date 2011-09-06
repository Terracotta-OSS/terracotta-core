/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.statistics.retrieval.actions;

import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.objectserver.dgc.impl.GCStatisticsAgentSubSystemEventListener;
import com.tc.statistics.StatisticData;
import com.tc.statistics.StatisticRetrievalAction;
import com.tc.statistics.StatisticType;

/**
 * This statistics represents statistics for the Distributed garbage collector. <p/> This statistic contains
 * {@link StatisticData} with the following elements:
 * <ul>
 * <li>iteration</li>
 * <li>type</li>
 * <li>start time</li>
 * <li>elapsed time</li>
 * <li>begin object count</li>
 * <li>candidate garbage count</li>
 * <li>actual garbage count</li>
 * <li>pause time</li>
 * <li>delete time</li>
 * </ul>
 * <p/> This statistic action should not be used to retrieve the Distributed garbage collector statistic. The actual
 * collection of the distributed garbage collector statistics is done in individual garbage collectors like
 * {@link com.tc.objectserver.dgc.impl.MarkAndSweepGarbageCollector} and the statistics are injected into the statistics
 * sub-system.
 */
public class SRADistributedGC implements StatisticRetrievalAction {

  public final static TCLogger LOGGER                         = TCLogging.getLogger(StatisticRetrievalAction.class);

  public static final String   ACTION_NAME                    = GCStatisticsAgentSubSystemEventListener.DISTRIBUTED_GC_STATISTICS;

  public static final String   ITERATION_ELEMENT              = "iteration";
  public static final String   TYPE_ELEMENT                   = "type";
  public static final String   START_TIME_ELEMENT             = "start time";
  public static final String   ELAPSED_TIME_ELEMENT           = "elapsed time";
  public static final String   BEGIN_OBJECT_COUNT_ELEMENT     = "begin object count";
  public static final String   CANDIDATE_OBJECT_COUNT_ELEMENT = "candidate garbage count";
  public static final String   ACTUAL_GARBAGE_COUNT_ELEMENT   = "actual garbage count";
  public static final String   PAUSE_TIME_ELEMENT             = "pause time";
  public static final String   MARK_TIME_ELEMENT              = "mark time";
  public static final String   DELETE_TIME_ELEMENT            = "delete time";

  public static final String   TYPE_FULL                      = "Full";
  public static final String   TYPE_YOUNG_GEN                 = "YoungGen";

  public StatisticData[] retrieveStatisticData() {
    LOGGER.warn("Data for statistic '" + ACTION_NAME + " can't be retrieved using the action instance. "
                + "It will be collected automatically when triggered by the system.");
    return EMPTY_STATISTIC_DATA;
  }

  public String getName() {
    return ACTION_NAME;
  }

  public StatisticType getType() {
    return StatisticType.TRIGGERED;
  }
}
