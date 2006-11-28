package com.tc.stats.statistics;

import javax.management.j2ee.statistics.BoundedRangeStatistic;

/**
 * An implementation of javax.management.j2ee.statistics.BoundedRangeStatistic.
 *
 * @see javax.management.j2ee.statistics.BoundedRangeStatistic
 * @see StatisticImpl
 */

public class BoundedRangeStatisticImpl extends StatisticImpl
  implements BoundedRangeStatistic
{
  private long m_upper;
  private long m_lower;
  private long m_high;
  private long m_low;
  private long m_current;

  public BoundedRangeStatisticImpl() {
    super("BoundedRangeStatistic", "", "", 0, 0);
  }

  public BoundedRangeStatisticImpl(String name, String unit, String description) {
    super(name, unit, description, 0, 0);
  }

  public BoundedRangeStatisticImpl(long upper, long lower, long high, long low, long current) {
    m_upper   = upper;
    m_lower   = lower;
    m_high    = high;
    m_low     = low;
    m_current = current;
  }

  public void setUpperBound(long upper) {
    m_upper = upper;
  }

  public long getUpperBound() {
    return m_upper;
  }

  public void setLowerBound(long lower) {
    m_lower = lower;
  }

  public long getLowerBound() {
    return m_lower;
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

  public void set(long upper, long lower, long high, long low, long current) {
    setUpperBound(upper);
    setLowerBound(lower);
    setHighWaterMark(high);
    setLowWaterMark(low);
    setCurrent(current);
  }
}
