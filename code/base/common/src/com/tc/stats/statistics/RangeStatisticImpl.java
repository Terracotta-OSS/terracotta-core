/*
 * Copyright (c) 2003-2006 Terracotta, Inc. All rights reserved.
 */
package com.tc.stats.statistics;

import javax.management.j2ee.statistics.RangeStatistic;

/**
 * An implementation of javax.management.j2ee.statistics.RangeStatistic.
 *
 * @see javax.management.j2ee.statistics.RangeStatistic
 * @see StatisticImpl
 */

public class RangeStatisticImpl extends StatisticImpl
  implements RangeStatistic
{
  private long m_high;
  private long m_low;
  private long m_current;

  public RangeStatisticImpl() {
    super("RangeStatistic", "", "", 0, 0);
  }

  public RangeStatisticImpl(String name, String unit, String description) {
    super(name, unit, description, 0, 0);
  }

  public RangeStatisticImpl(long high, long low, long current) {
    m_high    = high;
    m_low     = low;
    m_current = current;
  }

  public void setHighWaterMark(long high) {
    m_high = high;
  }

  public long getHighWaterMark() {
    return m_high;
  }

  public void setLowWaterMark(long low) {
    m_low = low;
  }

  public long getLowWaterMark() {
    return m_low;
  }

  public void setCurrent(long current) {
    m_current = current;
  }

  public long getCurrent() {
    return m_current;
  }

  public void set(long high, long low, long current) {
    setHighWaterMark(high);
    setLowWaterMark(low);
    setCurrent(current);
  }
}
