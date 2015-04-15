/* 
 * The contents of this file are subject to the Terracotta Public License Version
 * 2.0 (the "License"); You may not use this file except in compliance with the
 * License. You may obtain a copy of the License at 
 *
 *      http://terracotta.org/legal/terracotta-public-license.
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 * the specific language governing rights and limitations under the License.
 *
 * The Covered Software is Terracotta Platform.
 *
 * The Initial Developer of the Covered Software is 
 *      Terracotta, Inc., a Software AG company
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
