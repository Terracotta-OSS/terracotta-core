/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.impl;

import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.objectserver.api.GCStats;

import java.io.Serializable;

public class GCStatsImpl implements GCStats, Serializable {
  private static final long     serialVersionUID      = -4177683133067698672L;
  private static final TCLogger logger                = TCLogging.getLogger(GCStatsImpl.class);
  private static final long     NOT_INITIALIZED       = -1L;

  private final int             number;
  private long                  startTime             = NOT_INITIALIZED;
  private long                  elapsedTime           = NOT_INITIALIZED;
  private long                  beginObjectCount      = NOT_INITIALIZED;
  private long                  candidateGarbageCount = NOT_INITIALIZED;
  private long                  actualGarbageCount    = NOT_INITIALIZED;
  private long                  pausedTime            = NOT_INITIALIZED;
  private long                  deleteTime            = NOT_INITIALIZED;

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

  public synchronized long getPausedTime() {
    if (this.pausedTime == NOT_INITIALIZED) {
      errorNotInitialized();
    }
    return this.pausedTime;
  }

  public synchronized long getDeleteTime() {
    if (this.deleteTime == NOT_INITIALIZED) {
      errorNotInitialized();
    }
    return this.deleteTime;
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

  public synchronized void setPausedTime(long time) {
    if (time < 0L) {
      logger.warn("System timer moved backward, setting GC PausedTime to 0");
      time = 0;
    }
    this.pausedTime = time;
  }

  public synchronized void setDeleteTime(long time) {
    if (time < 0L) {
      logger.warn("System timer moved backward, setting GC DeleteTime to 0");
      time = 0;
    }
    this.deleteTime = time;
  }

  public synchronized void setElapsedTime(long time) {
    if (time < 0L) {
      logger.warn("System timer moved backward, setting GC ElapsedTime to 0");
      time = 0;
    }
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
    return "GCStats[" + getIteration() + "] : startTime = " + getStartTime() + "; elapsedTime = " + getElapsedTime()
           + "; pausedTime = " + getPausedTime() + "; deleteTime = " + getDeleteTime()
           + "; beginObjectCount = " + getBeginObjectCount() + "; candidateGarbageCount = "
           + getCandidateGarbageCount() + "; actualGarbageCount = " + getActualGarbageCount();
  }
}
