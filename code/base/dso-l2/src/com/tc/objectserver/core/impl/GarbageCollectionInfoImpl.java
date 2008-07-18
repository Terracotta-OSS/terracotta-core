/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.core.impl;

import com.tc.objectserver.core.api.GarbageCollectionInfo;

import java.util.List;
import java.util.SortedSet;

public class GarbageCollectionInfoImpl implements GarbageCollectionInfo {

  private int       iteration;

  private boolean   youngGen;

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

  public GarbageCollectionInfoImpl(int iteration) {
    this.iteration = iteration;
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

  public void markYoungGen() {
    youngGen = true;
  }

  public void markFullGen() {
    youngGen = false;
  }

  public boolean isYoungGen() {
    return youngGen ? true : false;
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

}