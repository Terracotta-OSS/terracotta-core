/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.stats;

import com.tc.objectserver.api.GCStats;
import com.tc.stats.statistics.CountStatistic;
import com.tc.stats.statistics.DoubleStatistic;
import com.tc.stats.statistics.Statistic;

/**
 * This defines the statistics that DSO can provide. Implementation classes make use of the com.tc.stats.statistics
 * package, consisting of implementations of the javax.management.j2ee.statistics.
 *
 * @see com.tc.stats.statistics.TimeStatisticImpl
 * @see javax.management.j2ee.statistics.TimeStatistic
 */

public interface DSOStats { // extends Stats { XXX: (TE) Not extending the generic Stats interface for now

  Statistic[] getStatistics(String[] names);

  DoubleStatistic getCacheHitRatio();

  CountStatistic getCacheMissRate();

  CountStatistic getTransactionRate();

  CountStatistic getObjectFaultRate();

  CountStatistic getObjectFlushRate();

  GCStats[] getGarbageCollectorStats();

}
