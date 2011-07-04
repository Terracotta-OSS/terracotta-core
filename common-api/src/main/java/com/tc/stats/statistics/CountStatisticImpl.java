/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.stats.statistics;

public class CountStatisticImpl extends StatisticImpl implements CountStatistic {
  private long m_count;

  public CountStatisticImpl() {
    this(0L);
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
}
