/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.core.impl;

import java.util.List;
import java.util.SortedSet;

public class GarbageCollectionInfo {

  private int       iteration;

  private boolean   fullGC;

  private long      startTime;

  private int       beginObjectCount;

  private long      markStageTime;

  private long      pauseStageTime;

  private long      deleteStageTime;

  private long      elapsedTime;

  private int       candidateGarbageCount;

  private int       actualGarbageCount;

  private Object    stats;

  private SortedSet toDelete;

  private List      rescueTimes;

  private int       preRescueCount;

  private int       rescue1Count;

  public GarbageCollectionInfo(int iteration, boolean fullGC) {
    this.iteration = iteration;
    this.fullGC = fullGC;
  }

  public int getRescue1Count() {
    return this.rescue1Count;
  }

  public void setRescue1Count(int count) {
    this.rescue1Count = count;
  }

  public int getPreRescueCount() {
    return this.preRescueCount;
  }

  public void setPreRescueCount(int count) {
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

  public void setBeginObjectCount(int count) {
    this.beginObjectCount = count;
  }

  public int getBeginObjectCount() {
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

  public void setCandidateGarbageCount(int count) {
    this.candidateGarbageCount = count;
  }

  public int getCandidateGarbageCount() {
    return this.candidateGarbageCount;
  }

  public int getActualGarbageCount() {
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
           + " deleteStageTime = " + deleteStageTime + " elapsedTime = " + elapsedTime
           + " candiate garabage  count = " + candidateGarbageCount + " actual garbage count  = " + actualGarbageCount
           + " pre rescue count = " + preRescueCount + " rescue 1 count = " + rescue1Count + " Garbage  = "
           + (toDelete == null ? "Not Set " : toDelete.size());
  }
}