/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.statistics.retrieval.actions;

import com.tc.objectserver.core.impl.MarkAndSweepGarbageCollector;
import com.tc.statistics.StatisticData;
import com.tc.statistics.StatisticRetrievalAction;
import com.tc.statistics.StatisticType;

/**
 * This statistics represents statistics for the Distributed garbage collector.
 * <p/>
 * This statistic contains {@link StatisticData} with the following elements:
 * <ul>
 * <li>iteration</li>
 * <li>start time</li>
 * <li>elapsed time</li>
 * <li>begin object count</li>
 * <li>candidate garbage count</li>
 * <li>actual garbage count</li>
 * </ul>
 * <p/>
 * This statistic action should not be used to retrieve the Distributed garbage collector statistic.
 * The actual collection of the distributed garbage collector statistics is done in individual
 * garbage collectors like {@link com.tc.objectserver.core.impl.MarkAndSweepGarbageCollector} and the
 * statistics are injected into the statistics sub-system.
 */
public class SRADistributedGC implements StatisticRetrievalAction {

  public static final String ACTION_NAME = MarkAndSweepGarbageCollector.DISTRIBUTED_GC_STATISTICS;

  public StatisticData[] retrieveStatisticData() {
    LOGGER.warn(ACTION_NAME + " Statistics cannot be retrieved using the action instance." +
                " It will be collected automatically when triggered by the system.");
    return EMPTY_STATISTIC_DATA;
  }

  public String getName() {
    return ACTION_NAME;
  }

  public StatisticType getType() {
    return StatisticType.TRIGGERED;
  }
}
