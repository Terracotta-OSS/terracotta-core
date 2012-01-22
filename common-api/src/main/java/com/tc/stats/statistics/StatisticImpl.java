/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
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
