/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.dgc.api;

import java.util.List;
import java.util.SortedSet;

public class GarbageCollectionInfo {

  protected static final long NOT_INITIALIZED       = -1L;

  private int                 iteration;

  private boolean             fullGC;

  private long                startTime             = NOT_INITIALIZED;

  private long                beginObjectCount      = NOT_INITIALIZED;

  private long                markStageTime         = NOT_INITIALIZED;

  private long                pauseStageTime        = NOT_INITIALIZED;

  private long                deleteStageTime       = NOT_INITIALIZED;

  private long                elapsedTime           = NOT_INITIALIZED;

  private long                totalMarkCycleTime    = NOT_INITIALIZED;

  private long                candidateGarbageCount = NOT_INITIALIZED;

  private long                actualGarbageCount    = NOT_INITIALIZED;

  private long                preRescueCount        = NOT_INITIALIZED;

  private long                rescue1Count          = NOT_INITIALIZED;

  private Object              stats                 = null;

  private SortedSet           toDelete              = null;

  private List                rescueTimes           = null;

  public GarbageCollectionInfo(int iteration, boolean fullGC) {
    this.iteration = iteration;
    this.fullGC = fullGC;
  }

  public long getRescue1Count() {
    return this.rescue1Count;
  }

  public void setRescue1Count(long count) {
    this.rescue1Count = count;
  }

  public long getPreRescueCount() {
    return this.preRescueCount;
  }

  public void setPreRescueCount(long count) {
    this.preRescueCount = count;
  }

  public int getIteration() {
    return this.iteration;
  }

  public boolean isFullGC() {
    return fullGC;
  }

  public void setStartTime(long time) {
    this.startTime = time;
  }

  public long getStartTime() {
    return this.startTime;
  }

  public void setBeginObjectCount(long count) {
    this.beginObjectCount = count;
  }

  public long getBeginObjectCount() {
    return this.beginObjectCount;
  }

  public void setMarkStageTime(long time) {
    this.markStageTime = time;
  }

  public long getMarkStageTime() {
    return this.markStageTime;
  }

  public void setPausedStageTime(long time) {
    this.pauseStageTime = time;
  }

  public long getPausedStageTime() {
    return this.pauseStageTime;
  }

  public void setDeleteStageTime(long time) {
    this.deleteStageTime = time;
  }

  public long getDeleteStageTime() {
    return this.deleteStageTime;
  }

  public void setElapsedTime(long time) {
    this.elapsedTime = time;
  }

  public long getElapsedTime() {
    return this.elapsedTime;
  }

  public void setTotalMarkCycleTime(long time) {
    this.totalMarkCycleTime = time;
  }

  public long getTotalMarkCycleTime() {
    return this.totalMarkCycleTime;
  }

  public void setCandidateGarbageCount(int count) {
    this.candidateGarbageCount = count;
  }

  public long getCandidateGarbageCount() {
    return this.candidateGarbageCount;
  }

  public long getActualGarbageCount() {
    return this.actualGarbageCount;
  }

  public void setDeleted(SortedSet deleted) {
    this.toDelete = deleted;
    this.actualGarbageCount = deleted.size();
  }

  public SortedSet getDeleted() {
    return this.toDelete;
  }

  public List getRescueTimes() {
    return rescueTimes;
  }

  public void setRescueTimes(List rescueTimes) {
    this.rescueTimes = rescueTimes;
  }

  public Object getObject() {
    return stats;
  }

  public void setObject(Object aGCStats) {
    this.stats = aGCStats;
  }

  public String toString() {
    return "GarbageCollectionInfo [ Iteration = " + iteration + " ] = " + " type  = "
           + (fullGC ? " young, " : " full, ") + " startTime = " + startTime + " begin object count = "
           + beginObjectCount + " markStageTime = " + markStageTime + " pauseStageTime = " + pauseStageTime
           + " deleteStageTime = " + deleteStageTime + " elapsedTime = " + elapsedTime + " totalMarkCycletime  = "
           + totalMarkCycleTime + " candiate garabage  count = " + candidateGarbageCount + " actual garbage count  = "
           + actualGarbageCount + " pre rescue count = " + preRescueCount + " rescue 1 count = " + rescue1Count
           + " Garbage  = " + (toDelete == null ? "Not Set " : toDelete.size());
  }
}