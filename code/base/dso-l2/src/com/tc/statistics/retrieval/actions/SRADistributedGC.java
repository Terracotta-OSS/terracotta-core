/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.statistics.retrieval.actions;

import com.tc.objectserver.core.impl.MarkAndSweepGarbageCollector;
import com.tc.statistics.StatisticData;
import com.tc.statistics.StatisticRetrievalAction;
import com.tc.statistics.StatisticType;

public class SRADistributedGC implements StatisticRetrievalAction {

  public static final String ACTION_NAME = MarkAndSweepGarbageCollector.DISTRIBUTED_GC_STATISTICS;

  public StatisticData[] retrieveStatisticData() {
    throw new UnsupportedOperationException(ACTION_NAME + " Statistics cannot be retrieved using the action instance." +
                                            " It will be collected automatically when triggered by the system.");
  }

  public String getName() {
    return ACTION_NAME;
  }

  public StatisticType getType() {
    return StatisticType.TRIGGERED;
  }
}
