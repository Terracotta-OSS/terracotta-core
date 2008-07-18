/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.core.api;

import java.util.List;
import java.util.SortedSet;

public interface GarbageCollectionInfo {

  public boolean isYoungGen();

  public int getIteration();

  public long getStartTime();

  public int getBeginObjectCount();

  public long getMarkStageTime();

  public long getPausedStageTime();

  public void setDeleteStageTime(long time);

  public long getDeleteStageTime();

  public void setElapsedTime(long time);

  public long getElapsedTime();

  public int getCandidateGarbageCount();

  public int getActualGarbageCount();

  public SortedSet getDeleted();

  public List getRescueTimes();

  public Object getObject();

  public void setObject(Object stats);

  public int getPreRescueCount();

  public int getRescue1Count();
}
