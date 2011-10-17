/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.dgc.api;

import com.tc.io.TCByteBufferInput;
import com.tc.io.TCByteBufferOutput;
import com.tc.io.TCSerializable;
import com.tc.objectserver.core.impl.GarbageCollectionID;

import java.io.IOException;

public class GarbageCollectionInfo implements TCSerializable {
  public static enum Type {
    NULL_GC, FULL_GC, YOUNG_GC, INLINE_CLEANUP, INLINE_GC
  }

  protected static final long               NOT_INITIALIZED       = -1L;
  protected static final long               NULL_INITIALIZED      = -2;
  private GarbageCollectionID               gcID                  = GarbageCollectionID.NULL_ID;
  private long                              startTime             = NOT_INITIALIZED;
  private long                              beginObjectCount      = NOT_INITIALIZED;
  private long                              markStageTime         = NOT_INITIALIZED;
  private long                              pauseStageTime        = NOT_INITIALIZED;
  private long                              deleteStageTime       = NOT_INITIALIZED;
  private long                              elapsedTime           = NOT_INITIALIZED;
  private long                              endObjectCount        = NOT_INITIALIZED;
  private long                              totalMarkCycleTime    = NOT_INITIALIZED;
  private long                              candidateGarbageCount = NOT_INITIALIZED;
  private long                              actualGarbageCount    = NOT_INITIALIZED;
  private long                              preRescueCount        = NOT_INITIALIZED;
  private long                              rescue1Count          = NOT_INITIALIZED;
  private long                              rescue1Time           = NOT_INITIALIZED;
  private long                              rescue2Time           = NOT_INITIALIZED;
  private Type                              type                  = Type.NULL_GC;

  public static final GarbageCollectionInfo NULL_INFO             = new GarbageCollectionInfo(
                                                                                              new GarbageCollectionID(
                                                                                                                      NULL_INITIALIZED,
                                                                                                                      "NULL INITIALIZED"),
                                                                                              Type.NULL_GC);

  public GarbageCollectionInfo() {
    // for serialization
  }

  public GarbageCollectionInfo(GarbageCollectionID id, Type type) {
    this.gcID = id;
    this.type = type;
  }

  public boolean isInlineCleanup() {
    return type == Type.INLINE_CLEANUP;
  }

  public boolean isInlineDGC() {
    return type == Type.INLINE_GC;
  }

  public void setCandidateGarbageCount(long candidateGarbageCount) {
    this.candidateGarbageCount = candidateGarbageCount;
  }

  public void setActualGarbageCount(long actualGarbageCount) {
    this.actualGarbageCount = actualGarbageCount;
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

  // TODO: see if we can remove this.
  public int getIteration() {
    return (int) this.gcID.toLong();
  }

  public boolean isFullGC() {
    return type == Type.FULL_GC;
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

  public void setEndObjectCount(long count) {
    this.endObjectCount = count;
  }

  public long getEndObjectCount() {
    return this.endObjectCount;
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

  public long getRescue1Time() {
    return rescue1Time;
  }

  public void setRescue1Time(long rescue1Time) {
    this.rescue1Time = rescue1Time;
  }

  public long getRescue2Time() {
    return rescue2Time;
  }

  public void setRescue2Time(long rescue2Time) {
    this.rescue2Time = rescue2Time;
  }

  public GarbageCollectionID getGarbageCollectionID() {
    return gcID;
  }

  @Override
  public String toString() {
    StringBuilder gcInfo = new StringBuilder();
    gcInfo.append("GarbageCollectionInfo [ Iteration = ");
    gcInfo.append(this.gcID.toLong());
    gcInfo.append(" ] = " + " type  = " + type);

    if (this.startTime != NOT_INITIALIZED) {
      gcInfo.append(" startTime = " + this.startTime);
    }
    if (this.beginObjectCount != NOT_INITIALIZED) {
      gcInfo.append(" begin object count = " + this.beginObjectCount);
    }
    if (this.endObjectCount != NOT_INITIALIZED) {
      gcInfo.append(" end object count = " + this.endObjectCount);
    }
    if (this.markStageTime != NOT_INITIALIZED) {
      gcInfo.append(" markStageTime = " + this.markStageTime);
    }
    if (this.pauseStageTime != NOT_INITIALIZED) {
      gcInfo.append(" pauseStageTime = " + this.pauseStageTime);
    }
    if (this.deleteStageTime != NOT_INITIALIZED) {
      gcInfo.append(" deleteStageTime = " + this.deleteStageTime);
    }
    if (this.elapsedTime != NOT_INITIALIZED) {
      gcInfo.append(" elapsedTime = " + this.elapsedTime);
    }
    if (this.totalMarkCycleTime != NOT_INITIALIZED) {
      gcInfo.append(" totalMarkCycleTime = " + this.totalMarkCycleTime);
    }
    if (this.candidateGarbageCount != NOT_INITIALIZED) {
      gcInfo.append(" candiate garabage  count = " + this.candidateGarbageCount);
    }
    if (this.actualGarbageCount != NOT_INITIALIZED) {
      gcInfo.append(" actual garbage count = " + this.actualGarbageCount);
    }
    if (this.preRescueCount != NOT_INITIALIZED) {
      gcInfo.append(" pre rescue count = " + this.preRescueCount);
    }
    if (this.rescue1Time != NOT_INITIALIZED) {
      gcInfo.append(" rescue1Time = " + this.rescue1Time);
    }
    if (this.rescue1Count != NOT_INITIALIZED) {
      gcInfo.append(" rescue 1 Count = " + this.rescue1Count);
    }
    if (this.rescue2Time != NOT_INITIALIZED) {
      gcInfo.append(" rescue2Time = " + this.rescue2Time);
    }
    return gcInfo.toString();
  }

  public Object deserializeFrom(TCByteBufferInput serialInput) throws IOException {

    long iterationCount = serialInput.readLong();
    String uuidString = serialInput.readString();
    this.gcID = new GarbageCollectionID(iterationCount, uuidString);
    this.startTime = serialInput.readLong();
    this.beginObjectCount = serialInput.readLong();
    this.endObjectCount = serialInput.readLong();
    this.markStageTime = serialInput.readLong();
    this.pauseStageTime = serialInput.readLong();
    this.deleteStageTime = serialInput.readLong();
    this.elapsedTime = serialInput.readLong();
    this.totalMarkCycleTime = serialInput.readLong();
    this.candidateGarbageCount = serialInput.readLong();
    this.actualGarbageCount = serialInput.readLong();
    this.preRescueCount = serialInput.readLong();
    this.rescue1Count = serialInput.readLong();
    this.rescue1Time = serialInput.readLong();
    this.rescue2Time = serialInput.readLong();
    this.type = Type.valueOf(serialInput.readString());
    return this;
  }

  public void serializeTo(TCByteBufferOutput serialOutput) {
    serialOutput.writeLong(this.gcID.toLong());
    serialOutput.writeString(this.gcID.getUUID());
    serialOutput.writeLong(this.startTime);
    serialOutput.writeLong(this.beginObjectCount);
    serialOutput.writeLong(this.endObjectCount);
    serialOutput.writeLong(this.markStageTime);
    serialOutput.writeLong(this.pauseStageTime);
    serialOutput.writeLong(this.deleteStageTime);
    serialOutput.writeLong(this.elapsedTime);
    serialOutput.writeLong(this.totalMarkCycleTime);
    serialOutput.writeLong(this.candidateGarbageCount);
    serialOutput.writeLong(this.actualGarbageCount);
    serialOutput.writeLong(this.preRescueCount);
    serialOutput.writeLong(this.rescue1Count);
    serialOutput.writeLong(this.rescue1Time);
    serialOutput.writeLong(this.rescue2Time);
    serialOutput.writeString(this.type.toString());
  }
}