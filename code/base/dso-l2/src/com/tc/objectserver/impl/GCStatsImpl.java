/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.impl;

import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.objectserver.api.GCStats;
import com.tc.util.State;

import java.io.Serializable;
import java.text.SimpleDateFormat;

public class GCStatsImpl implements GCStats, Serializable {
  private static final long             serialVersionUID      = -4177683133067698672L;
  private static final TCLogger         logger                = TCLogging.getLogger(GCStatsImpl.class);
  private static final SimpleDateFormat printFormat           = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss z");

  private static final State            GC_START              = new State("START");
  private static final State            GC_MARK               = new State("MARK");
  private static final State            GC_PAUSE              = new State("PAUSE");
  private static final State            GC_MARK_COMPLETE      = new State("MARK_COMPLETE");
  private static final State            GC_DELETE             = new State("DELETE");
  private static final State            GC_COMPLETE           = new State("COMPLETE");

  private static final long             NOT_INITIALIZED       = -1L;
  private static final String           YOUNG_GENERATION      = "Young";
  private static final String           FULL_GENERATION       = "Full";
  private final int                     number;
  private final long                    startTime;
  private long                          elapsedTime           = NOT_INITIALIZED;
  private long                          beginObjectCount      = NOT_INITIALIZED;
  private long                          candidateGarbageCount = NOT_INITIALIZED;
  private long                          actualGarbageCount    = NOT_INITIALIZED;
  private long                          markStageTime         = NOT_INITIALIZED;
  private long                          pausedStageTime       = NOT_INITIALIZED;
  private long                          deleteStageTime       = NOT_INITIALIZED;
  private State                         state                 = GC_START;
  private final boolean                 fullGC;

  public GCStatsImpl(int number, boolean fullGC, long startTime) {
    this.number = number;
    this.fullGC = fullGC;
    validate(startTime);
    this.startTime = startTime;
  }

  public int getIteration() {
    return this.number;
  }

  public synchronized void setMarkState() {
    this.state = GC_MARK;
  }

  public synchronized void setPauseState() {
    this.state = GC_PAUSE;
  }

  public synchronized void setMarkCompleteState() {
    this.state = GC_MARK_COMPLETE;
  }

  public synchronized void setCompleteState() {
    this.state = GC_COMPLETE;
  }

  public synchronized void setDeleteState() {
    this.state = GC_DELETE;
  }

  public synchronized long getStartTime() {
    return this.startTime;
  }

  public synchronized long getElapsedTime() {
    return this.elapsedTime;
  }

  public synchronized long getBeginObjectCount() {
    return this.beginObjectCount;
  }

  public synchronized long getCandidateGarbageCount() {
    return this.candidateGarbageCount;
  }

  public synchronized long getActualGarbageCount() {
    return this.actualGarbageCount;
  }

  public synchronized long getMarkStageTime() {
    return this.markStageTime;
  }

  public synchronized long getPausedStageTime() {
    return this.pausedStageTime;
  }

  public synchronized long getDeleteStageTime() {
    return this.deleteStageTime;
  }

  public synchronized String getStatus() {
    return state.getName();
  }

  public synchronized String getType() {
    return fullGC ?  FULL_GENERATION : YOUNG_GENERATION;
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

  public synchronized void setMarkStageTime(long time) {
    if (time < 0L) {
      logger.warn("System timer moved backward, setting GC MarkStageTime to 0");
      time = 0;
    }
    this.markStageTime = time;
  }

  public synchronized void setPausedStageTime(long time) {
    if (time < 0L) {
      logger.warn("System timer moved backward, setting GC PausedStageTime to 0");
      time = 0;
    }
    this.pausedStageTime = time;
  }

  public synchronized void setDeleteStageTime(long time) {
    if (time < 0L) {
      logger.warn("System timer moved backward, setting GC DeleteStageTime to 0");
      time = 0;
    }
    this.deleteStageTime = time;
  }

  public synchronized void setElapsedTime(long time) {
    if (time < 0L) {
      logger.warn("System timer moved backward, setting GC ElapsedTime to 0");
      time = 0;
    }
    this.elapsedTime = time;
  }

  private void validate(long value) {
    if (value < 0L) { throw new IllegalArgumentException("Value must be greater than or equal to zero"); }
  }

  private String formatAsDate(long date) {
    return printFormat.format(date);
  }

  private String formatTime(long time) {
    if (time == NOT_INITIALIZED) {
      return "N/A";
    } else {
      return time + "ms";
    }
  }

  public String toString() {
    return "GCStats[ iteration: " + getIteration() + "; type: " + getType() + "; status: " + getStatus()
           + " ] : startTime = " + formatAsDate(this.startTime) + "; elapsedTime = " + formatTime(this.elapsedTime)
           + "; markStageTime = " + formatTime(markStageTime) + "; pausedStageTime = "
           + formatTime(this.pausedStageTime) + "; deleteStageTime = " + formatTime(this.deleteStageTime)
           + "; beginObjectCount = " + this.beginObjectCount + "; candidateGarbageCount = "
           + this.candidateGarbageCount + "; actualGarbageCount = " + this.actualGarbageCount;
  }

}
