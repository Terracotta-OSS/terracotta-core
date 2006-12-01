/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.admin.common;

import org.jfree.data.time.Second;

import com.tc.admin.ConnectionContext;
import com.tc.stats.statistics.DoubleStatistic;
import com.tc.stats.statistics.Statistic;

import java.util.Date;

public class DoubleStatisticPanel extends StatisticPanel {
  private Date date = new Date();

  public DoubleStatisticPanel(ConnectionContext cc) {
    super(cc);
  }

  public void setStatistic(Statistic stat) {
    super.setStatistic(stat);

    DoubleStatistic doubleStat = (DoubleStatistic)stat;
    long            ts         = doubleStat.getLastSampleTime();
    double          value      = doubleStat.getDoubleValue();

    date.setTime(ts);

    getTimeSeries().addOrUpdate(new Second(date), value);
  }

  public void tearDown() {
    super.tearDown();
    date = null;
  }
}
