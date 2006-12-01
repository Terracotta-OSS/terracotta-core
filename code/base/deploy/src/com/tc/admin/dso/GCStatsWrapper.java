/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.admin.dso;

import java.util.Date;

import com.tc.objectserver.api.GCStats;

public class GCStatsWrapper implements GCStats {
  private GCStats m_gcStats;
  private Date    m_startDate;

  GCStatsWrapper(GCStats gcStats) {
    m_gcStats   = gcStats;
    m_startDate = new Date(gcStats.getStartTime());
  }

  public int getIteration() {
    return m_gcStats.getIteration();
  }

  public long getStartTime() {
    return m_gcStats.getStartTime();
  }

  public Date getStartDate() {
    return m_startDate;
  }

  public long getElapsedTime() {
    return m_gcStats.getElapsedTime();
  }

  public long getBeginObjectCount() {
    return m_gcStats.getBeginObjectCount();
  }

  public long getCandidateGarbageCount() {
    return m_gcStats.getCandidateGarbageCount();
  }

  public long getActualGarbageCount() {
    return m_gcStats.getActualGarbageCount();
  }
}
