/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.stats;

import com.tc.stats.counter.Counter;
import com.tc.stats.counter.sampled.SampledCounter;
import com.tc.stats.counter.sampled.TimeStampedCounterValue;
import com.tc.stats.statistics.CountStatistic;
import com.tc.stats.statistics.CountStatisticImpl;

// TODO: remove me

public class StatsUtil {

  public static CountStatistic makeCountStat(SampledCounter counter) {
    TimeStampedCounterValue sample = counter.getMostRecentSample();
    return makeCountStat(sample);
  }

  public static CountStatistic makeCountStat(TimeStampedCounterValue sample) {
    CountStatisticImpl stat = new CountStatisticImpl();
    // TODO: we could include the min/max/avg in the returned stat
    stat.setLastSampleTime(sample.getTimestamp());
    stat.setCount(sample.getCounterValue());
    return stat;
  }

  public static CountStatistic makeCountStat(Counter counter) {
    CountStatisticImpl stat = new CountStatisticImpl();
    stat.setLastSampleTime(System.currentTimeMillis());
    stat.setCount(counter.getValue());
    return stat;
  }

}
