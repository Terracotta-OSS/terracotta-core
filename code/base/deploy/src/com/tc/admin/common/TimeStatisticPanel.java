package com.tc.admin.common;

import java.util.Date;

import javax.management.j2ee.statistics.Statistic;
import javax.management.j2ee.statistics.TimeStatistic;

import org.jfree.data.time.Second;

import com.tc.admin.ConnectionContext;

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
