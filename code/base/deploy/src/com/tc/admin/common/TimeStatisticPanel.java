/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.admin.common;

import org.jfree.data.time.Second;

import com.tc.admin.ConnectionContext;
import com.tc.stats.statistics.Statistic;
import com.tc.stats.statistics.TimeStatistic;

import java.util.Date;

public class TimeStatisticPanel extends StatisticPanel {
  public TimeStatisticPanel(ConnectionContext cc) {
    super(cc);
  }

  public void setStatistic(Statistic stat) {
    super.setStatistic(stat);

    TimeStatistic timeStat = (TimeStatistic)stat;
    long          ts       = timeStat.getLastSampleTime();
    long          count    = timeStat.getCount();

    getTimeSeries().addOrUpdate(new Second(new Date(ts)), count);
  }
}
