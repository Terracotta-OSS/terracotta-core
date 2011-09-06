/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.stats.statistics;

import java.io.Serializable;

public class StatisticImpl implements Serializable {
  private long   m_lastSampleTime;

  public StatisticImpl() {
    m_lastSampleTime = 0;
  }

  public StatisticImpl(long lastSampleTime) {
    m_lastSampleTime = lastSampleTime;
  }

  public void setLastSampleTime(long lastSampleTime) {
    m_lastSampleTime = lastSampleTime;
  }

  public long getLastSampleTime() {
    return m_lastSampleTime;
  }

  private static final long serialVersionUID = 42;
}
