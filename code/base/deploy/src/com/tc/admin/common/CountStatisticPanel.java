/*
 * Copyright (c) 2003-2006 Terracotta, Inc. All rights reserved.
 */
package com.tc.admin.common;

import java.util.Date;

import javax.management.j2ee.statistics.CountStatistic;
import javax.management.j2ee.statistics.Statistic;

import org.jfree.data.time.Second;

import com.tc.admin.ConnectionContext;

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
