/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.objectserver.core.api;

import com.tc.objectserver.impl.GCStatsImpl;
import com.tc.util.ObjectIDSet;

import java.util.List;
import java.util.SortedSet;

public interface GarbageCollectionInfo {
  
  public boolean isYoungGen();
 
  public int getIteration();
  
  public long getStartTime();
  
  public long getBeginObjectCount();
  
  public long getMarkStageTime();
  
  public long getPausedStageTime();
  
  public void setDeleteStageTime(long time);
  
  public long getDeleteStageTime();
  
  public void setElapsedTime(long time);
  
  public long getElapsedTime();
  
  public long getCandidateGarbageCount();
  
  public long getActualGarbageCount();
  
  public SortedSet getDeleted();
  
  public ObjectIDSet getGcResults();
  
  public ObjectIDSet getManagedIDs();
  
  public List getRescueTimes();
  
  public GCStatsImpl getObject();
  
  public void setObject(GCStatsImpl gcStats);
}
