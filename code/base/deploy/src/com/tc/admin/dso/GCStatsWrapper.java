/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.admin.dso;

import java.util.Date;

import com.tc.objectserver.api.GCStats;

public class GCStatsWrapper implements GCStats {
  private GCStats m_gcStats;
  private Date    m_startDate;

  GCStatsWrapper(GCStats gcStats) {

    set(gcStats);
  }

  void set(GCStats gcStats) {
    m_gcStats = gcStats;
    m_startDate = new Date(gcStats.getStartTime());
  }
  
  public int getIteration() {
    return m_gcStats.getIteration();
  }

  public String getType() {
    return m_gcStats.getType();
  }

  public String getStatus() {
    return m_gcStats.getStatus();
  }

  public long getStartTime() {
    return m_gcStats.getStartTime();
  }

  public Date getStartDate() {
    return new Date(m_startDate.getTime());
  }

  public long getElapsedTime() {
    return m_gcStats.getElapsedTime();
  }

  public long getBeginObjectCount() {
    return m_gcStats.getBeginObjectCount();
  }

  public long getPausedStageTime() {
    return m_gcStats.getPausedStageTime();
  }

  public long getMarkStageTime() {
    return m_gcStats.getMarkStageTime();
  }

  public long getActualGarbageCount() {
    return m_gcStats.getActualGarbageCount();
  }

  public long getDeleteStageTime() {
    return m_gcStats.getDeleteStageTime();
  }
  
  // No longer used
  public long getCandidateGarbageCount() {
    return m_gcStats.getCandidateGarbageCount();
  }
}
