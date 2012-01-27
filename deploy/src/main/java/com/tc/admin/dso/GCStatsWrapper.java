/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.admin.dso;

import org.apache.commons.lang.builder.EqualsBuilder;

import com.tc.objectserver.api.GCStats;

import java.util.Date;

public class GCStatsWrapper implements GCStats, Comparable {
  private GCStats gcStats;
  private Date    startDate;

  GCStatsWrapper(GCStats gcStats) {
    set(gcStats);
  }

  void set(GCStats gcStats) {
    this.gcStats = gcStats;
    startDate = new Date(gcStats.getStartTime());
  }

  public int getIteration() {
    return gcStats.getIteration();
  }

  public String getType() {
    return gcStats.getType();
  }

  public String getStatus() {
    return gcStats.getStatus();
  }

  public long getStartTime() {
    return gcStats.getStartTime();
  }

  public Date getStartDate() {
    return new Date(startDate.getTime());
  }

  public long getElapsedTime() {
    return gcStats.getElapsedTime();
  }

  public long getBeginObjectCount() {
    return gcStats.getBeginObjectCount();
  }

  public long getEndObjectCount() {
    return gcStats.getEndObjectCount();
  }

  public long getPausedStageTime() {
    return gcStats.getPausedStageTime();
  }

  public long getMarkStageTime() {
    return gcStats.getMarkStageTime();
  }

  public long getActualGarbageCount() {
    return gcStats.getActualGarbageCount();
  }

  public long getDeleteStageTime() {
    return gcStats.getDeleteStageTime();
  }

  // No longer used
  public long getCandidateGarbageCount() {
    return gcStats.getCandidateGarbageCount();
  }

  public int compareTo(Object o) {
    if (!GCStatsWrapper.class.isAssignableFrom(o.getClass())) { throw new ClassCastException("Not a GCStatsWrapper"); }
    Integer iter = Integer.valueOf(getIteration());
    Integer otherIter = Integer.valueOf(((GCStatsWrapper) o).getIteration());
    return otherIter.compareTo(iter);
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == null) { return false; }
    if (obj == this) { return true; }
    if (obj.getClass() != getClass()) { return false; }
    return new EqualsBuilder().appendSuper(super.equals(obj)).isEquals();
  }

  @Override
  public int hashCode() {
    return gcStats.hashCode();
  }
}
