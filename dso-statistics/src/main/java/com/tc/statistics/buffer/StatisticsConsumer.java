/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.statistics.buffer;

import com.tc.statistics.StatisticData;

public interface StatisticsConsumer {
  public long getMaximumConsumedDataCount();
  public boolean consumeStatisticData(StatisticData data);
}
