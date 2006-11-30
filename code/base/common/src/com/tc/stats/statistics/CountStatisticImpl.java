/*
 * Copyright (c) 2003-2006 Terracotta, Inc. All rights reserved.
 */
package com.tc.stats.statistics;

import javax.management.j2ee.statistics.CountStatistic;

/**
 * An implementation of javax.management.j2ee.statistics.CountStatistic.
 *
 * @see javax.management.j2ee.statistics.CountStatistic
 * @see StatisticImpl
 */

public class CountStatisticImpl extends StatisticImpl
  implements CountStatistic
{
  private long m_count;

  public CountStatisticImpl() {
    super("CountStatistic", "", "", 0, 0);
  }

  public CountStatisticImpl(String name, String unit, String description) {
    super(name, unit, description, 0, 0);
  }

  public CountStatisticImpl(long count) {
    m_count = count;
  }

  public void setCount(long count) {
    m_count = count;
  }

  public long getCount() {
    return m_count;
  }

  public void update(long count) {
    setCount(count);
  }
}
