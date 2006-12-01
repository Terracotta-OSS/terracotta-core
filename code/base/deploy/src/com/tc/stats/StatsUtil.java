/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.stats;

import com.tc.stats.counter.sampled.SampledCounter;
import com.tc.stats.counter.sampled.TimeStampedCounterValue;
import com.tc.stats.statistics.CountStatistic;
import com.tc.stats.statistics.CountStatisticImpl;


public class StatsUtil {

  public static CountStatistic makeCountStat(SampledCounter counter) {
    CountStatisticImpl stat = new CountStatisticImpl();
    TimeStampedCounterValue sample = counter.getMostRecentSample();
    // TODO: we could include the min/max/avg in the returned stat
    stat.setLastSampleTime(sample.getTimestamp());
    stat.setCount(sample.getCounterValue());
    return stat;
  }

}
