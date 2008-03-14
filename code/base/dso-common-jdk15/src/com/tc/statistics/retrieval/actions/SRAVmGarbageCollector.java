/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.statistics.retrieval.actions;

import com.tc.statistics.StatisticData;
import com.tc.statistics.StatisticRetrievalAction;
import com.tc.statistics.StatisticType;

import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

/**
 * This statistic represents statistics for the Java VM garbage collector.
 *
 * This statistic contains multiple {@link StatisticData} for each of the memory managers. Each memory manager has the following
 * element
 * <ul>
 * <li>collection count</li>
 * <li>collection time</li>
 * <li>memory pool names</li>
 * </ul>
 *
 * The element in each of the {@link com.tc.statistics.StatisticData} is a combination of the name of the memory manager and
 * the element separated by a colon (:).
 *
 * This statistic will only be available in nodes running in JRE-1.5 or higher
 */
public class SRAVmGarbageCollector implements StatisticRetrievalAction {

  public static final String ACTION_NAME = "vm garbage collector";

  private static final List<GarbageCollectorMXBean> gcBeans = ManagementFactory.getGarbageCollectorMXBeans();

  public StatisticData[] retrieveStatisticData() {
    if (gcBeans == null) {
      return EMPTY_STATISTIC_DATA;
    }

    List<StatisticData> data = new ArrayList<StatisticData>();
    for (GarbageCollectorMXBean gcBean : gcBeans) {
      data.addAll(getStatisticData(gcBean));
    }
    return data.toArray(new StatisticData[data.size()]);
  }

  private Collection<? extends StatisticData> getStatisticData(GarbageCollectorMXBean gcBean) {
    Collection<StatisticData> data = new ArrayList<StatisticData>();
    data.add(new StatisticData(ACTION_NAME, gcBean.getName() + ":collection count", gcBean.getCollectionCount()));
    data.add(new StatisticData(ACTION_NAME, gcBean.getName() + ":collection time", gcBean.getCollectionTime()));
    data.add(new StatisticData(ACTION_NAME, gcBean.getName() + ":memory pool names", Arrays.asList(gcBean.getMemoryPoolNames()).toString()));
    return data;
  }

  public String getName() {
    return ACTION_NAME;
  }

  public StatisticType getType() {
    return StatisticType.SNAPSHOT;
  }
}
