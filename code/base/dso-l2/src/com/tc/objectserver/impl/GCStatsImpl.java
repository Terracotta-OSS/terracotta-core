/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.objectserver.impl;

import java.io.Serializable;

import com.tc.objectserver.api.GCStats;

public class GCStatsImpl implements GCStats, Serializable {
  private static final long NOT_INITIALIZED       = -1L;

  private final int         number;
  private long              startTime             = NOT_INITIALIZED;
  private long              elapsedTime           = NOT_INITIALIZED;
  private long              beginObjectCount      = NOT_INITIALIZED;
  private long              candidateGarbageCount = NOT_INITIALIZED;
  private long              actualGarbageCount    = NOT_INITIALIZED;

  public GCStatsImpl(int number) {
    this.number = number;
  }

  public int getIteration() {
    return this.number;
  }

  public synchronized long getStartTime() {
    if (this.startTime == NOT_INITIALIZED) {
      errorNotInitialized();
    }
    return this.startTime;
  }

  public synchronized long getElapsedTime() {
    if (this.elapsedTime == NOT_INITIALIZED) {
      errorNotInitialized();
    }
    return this.elapsedTime;
  }

  public synchronized long getBeginObjectCount() {
    if (this.beginObjectCount == NOT_INITIALIZED) {
      errorNotInitialized();
    }
    return this.beginObjectCount;
  }

  public synchronized long getCandidateGarbageCount() {
    if (this.candidateGarbageCount == NOT_INITIALIZED) {
      errorNotInitialized();
    }
    return this.candidateGarbageCount;
  }

  public synchronized long getActualGarbageCount() {
    if (this.actualGarbageCount == NOT_INITIALIZED) {
      errorNotInitialized();
    }
    return this.actualGarbageCount;
  }

  public synchronized void setActualGarbageCount(long count) {
    validate(count);
    this.actualGarbageCount = count;
  }

  public synchronized void setBeginObjectCount(long count) {
    validate(count);
    this.beginObjectCount = count;
  }

  public synchronized void setCandidateGarbageCount(long count) {
    validate(count);
    this.candidateGarbageCount = count;
  }

  public synchronized void setElapsedTime(long time) {
    validate(time);
    this.elapsedTime = time;
  }

  public synchronized void setStartTime(long time) {
    validate(time);
    this.startTime = time;
  }

  private void validate(long value) {
    if (value < 0L) { throw new IllegalArgumentException("Value must be greater than or equal to zero"); }
  }

  private void errorNotInitialized() {
    throw new IllegalStateException("Value not initialized");
  }

  public String toString() {
    return "iteration="+getIteration()+
      "; startTime="+getStartTime()+
      "; elapsedTime="+getElapsedTime()+
      "; beginObjectCount="+getBeginObjectCount()+
      "; candidateGarbageCount="+getCandidateGarbageCount()+
      "; actualGarbageCount="+getActualGarbageCount();
  }
}
