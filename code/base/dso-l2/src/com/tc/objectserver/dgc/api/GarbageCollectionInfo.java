/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.dgc.api;

import com.tc.util.ObjectIDSet;

import java.util.List;

public class GarbageCollectionInfo implements Cloneable {

  protected static final long NOT_INITIALIZED       = -1L;

  private final int           iteration;

  private final boolean       fullGC;

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

  private ObjectIDSet         toDelete              = null;

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
    return this.fullGC;
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

  public void setDeleted(ObjectIDSet deleted) {
    this.toDelete = deleted;
    this.actualGarbageCount = deleted.size();
  }

  public ObjectIDSet getDeleted() {
    return this.toDelete;
  }

  public List getRescueTimes() {
    return this.rescueTimes;
  }

  public void setRescueTimes(List rescueTimes) {
    this.rescueTimes = rescueTimes;
  }

  public Object getObject() {
    return this.stats;
  }

  public void setObject(Object aGCStats) {
    this.stats = aGCStats;
  }

  @Override
  public String toString() {
    return "GarbageCollectionInfo [ Iteration = " + this.iteration + " ] = " + " type  = "
           + (this.fullGC ? " full, " : " young, ") + " startTime = " + this.startTime + " begin object count = "
           + this.beginObjectCount + " markStageTime = " + this.markStageTime + " pauseStageTime = "
           + this.pauseStageTime + " deleteStageTime = " + this.deleteStageTime + " elapsedTime = " + this.elapsedTime
           + " totalMarkCycletime  = " + this.totalMarkCycleTime + " candiate garabage  count = "
           + this.candidateGarbageCount + " actual garbage count  = " + this.actualGarbageCount
           + " pre rescue count = " + this.preRescueCount + " rescue 1 count = " + this.rescue1Count + " Garbage  = "
           + (this.toDelete == null ? "Not Set " : this.toDelete.size());
  }
}