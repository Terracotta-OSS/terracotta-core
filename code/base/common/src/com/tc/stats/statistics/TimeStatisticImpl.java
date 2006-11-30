/*
 * Copyright (c) 2003-2006 Terracotta, Inc. All rights reserved.
 */
package com.tc.stats.statistics;

import javax.management.j2ee.statistics.TimeStatistic;

/**
 * An implementation of javax.management.j2ee.statistics.TimeStatistic.
 *
 * @see javax.management.j2ee.statistics.TimeStatistic
 * @see StatisticImpl
 */

public class TimeStatisticImpl extends StatisticImpl 
  implements TimeStatistic
{
  private long m_count;
  private long m_maxTime;
  private long m_minTime;
  private long m_totalTime;

  public TimeStatisticImpl() {
    super("TimeStatistic", "", "", 0, 0);
  }

  public TimeStatisticImpl(String name, String unit, String description) {
    super(name, unit, description, 0, 0);
  }

  public TimeStatisticImpl(long count, long maxTime, long minTime, long totalTime) {
    m_count     = count;
    m_maxTime   = maxTime;
    m_minTime   = minTime;
    m_totalTime = totalTime;
  }

  public void setCount(long count) {
    m_count = count;
  }

  public long getCount() {
    return m_count;
  }

  public void setMaxTime(long maxTime) {
    m_maxTime = maxTime;
  }

  public long getMaxTime() {
    return m_maxTime;
  }

  public void setMinTime(long minTime) {
    m_minTime = minTime;
  }

  public long getMinTime() {
    return m_minTime;
  }

  public void setTotalTime(long totalTime) {
    m_totalTime = totalTime;
  }

  public long getTotalTime() {
    return m_totalTime;
  }

  public void update(long count, long maxTime, long minTime, long totalTime) {
    setCount(count);
    setMaxTime(maxTime);
    setMinTime(minTime);
    setTotalTime(totalTime);
  }
}
