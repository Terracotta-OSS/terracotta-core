/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.admin.common;

import org.jfree.data.time.Second;

import com.tc.admin.ConnectionContext;
import com.tc.stats.statistics.CountStatistic;
import com.tc.stats.statistics.Statistic;

import java.util.Date;

public class CountStatisticPanel extends StatisticPanel {
  private Date date = new Date();

  public CountStatisticPanel(ConnectionContext cc) {
    super(cc);
  }

  public void setStatistic(Statistic stat) {
    super.setStatistic(stat);

    CountStatistic countStat = (CountStatistic)stat;
    long           ts        = countStat.getLastSampleTime();
    long           count     = countStat.getCount();

    date.setTime(ts);

    getTimeSeries().addOrUpdate(new Second(date), count);
  }

  public void tearDown() {
    super.tearDown();
    date = null;
  }
}
