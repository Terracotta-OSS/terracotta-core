/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.core.api;

import java.util.List;
import java.util.SortedSet;

public interface GarbageCollectionInfo {

  public int getRescue1Count();

  public void setRescue1Count(int count);

  public int getPreRescueCount();

  public void setPreRescueCount(int count);

  public int getIteration();

  public void markYoungGen();

  public void markFullGen();

  public boolean isYoungGen();

  public void setStartTime(long time);

  public long getStartTime();

  public void setBeginObjectCount(int count);

  public int getBeginObjectCount();

  public void setMarkStageTime(long time);

  public long getMarkStageTime();

  public void setPausedStageTime(long time);

  public long getPausedStageTime();

  public void setDeleteStageTime(long time);

  public long getDeleteStageTime();

  public void setElapsedTime(long time);

  public long getElapsedTime();

  public void setCandidateGarbageCount(int count);

  public int getCandidateGarbageCount();

  public int getActualGarbageCount();

  public void setDeleted(SortedSet deleted);

  public SortedSet getDeleted();

  public List getRescueTimes();

  public void setRescueTimes(List rescueTimes);

  public Object getObject();

  public void setObject(Object aGCStats);
}
