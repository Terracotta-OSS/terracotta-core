/*
 * Copyright (c) 2003-2006 Terracotta, Inc. All rights reserved.
 */
package com.tc.stats.statistics;

import javax.management.j2ee.statistics.BoundaryStatistic;

/**
 * An implementation of javax.management.j2ee.statistics.BoundaryStatistic.
 *
 * @see javax.management.j2ee.statistics.BoundaryStatistic
 * @see StatisticImpl
 */

public class BoundaryStatisticImpl extends StatisticImpl
  implements BoundaryStatistic
{
  private long m_upper;
  private long m_lower;

  public BoundaryStatisticImpl() {
    super("BoundaryStatistic", "", "", 0, 0);
  }

  public BoundaryStatisticImpl(String name, String unit, String description) {
    super(name, unit, description, 0, 0);
  }

  public BoundaryStatisticImpl(long upper, long lower) {
    m_upper = upper;
    m_lower = lower;
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

  public void set(long upper, long lower) {
    setUpperBound(upper);
    setLowerBound(lower);
  }
}
