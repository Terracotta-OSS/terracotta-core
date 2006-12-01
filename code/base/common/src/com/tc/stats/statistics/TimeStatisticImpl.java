/*
 * Copyright (c) 2003-2006 Terracotta, Inc. All rights reserved.
 */
package com.tc.stats.statistics;

public class TimeStatisticImpl extends StatisticImpl {
  private long m_count;

  public TimeStatisticImpl() {
    this(0L);
  }

  public TimeStatisticImpl(long count) {
    m_count = count;
  }

  public void setCount(long count) {
    m_count = count;
  }

  public long getCount() {
    return m_count;
  }

}
