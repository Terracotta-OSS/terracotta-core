/*
 * Copyright (c) 2003-2006 Terracotta, Inc. All rights reserved.
 */
package com.tc.stats;

import com.tc.stats.counter.sampled.SampledCounter;
import com.tc.stats.counter.sampled.TimeStampedCounterValue;
import com.tc.stats.statistics.CountStatisticImpl;

import javax.management.j2ee.statistics.CountStatistic;


public class StatsUtil {

  public static CountStatistic makeCountStat(SampledCounter counter, String name, String unit, String desc) {
    CountStatisticImpl stat = new CountStatisticImpl(name, unit, desc);
    TimeStampedCounterValue sample = counter.getMostRecentSample();
    // TODO: we could include the min/max/avg in the returned stat
    stat.setLastSampleTime(sample.getTimestamp());
    stat.setCount(sample.getCounterValue());
    return stat;
  }

}
