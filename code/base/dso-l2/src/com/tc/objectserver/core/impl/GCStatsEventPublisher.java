/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.core.impl;

import com.tc.objectserver.api.GCStats;
import com.tc.objectserver.api.GCStatsEventListener;
import com.tc.objectserver.impl.GCStatsImpl;
import com.tc.stats.LossyStack;

import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class GCStatsEventPublisher extends GarbageCollectorEventListenerAdapter {

  private List             gcStatsEventListeners = new CopyOnWriteArrayList();

  private final LossyStack gcHistory             = new LossyStack(1500);

  public void addListener(GCStatsEventListener listener) {
    gcStatsEventListeners.add(listener);
  }

  public GCStats[] getGarbageCollectorStats() {
    return (GCStats[]) gcHistory.toArray(new GCStats[gcHistory.depth()]);
  }

  public void garbageCollectorStart(GarbageCollectionInfo info) {
    GCStatsImpl gcStats = getGCStats(info);
    fireGCStatsEvent(gcStats);
  }

  public void garbageCollectorMark(GarbageCollectionInfo info) {
    GCStatsImpl gcStats = getGCStats(info);
    gcStats.setBeginObjectCount(info.getBeginObjectCount());
    gcStats.setMarkState();
    fireGCStatsEvent(gcStats);
  }

  public void garbageCollectorPausing(GarbageCollectionInfo info) {
    GCStatsImpl gcStats = getGCStats(info);
    gcStats.setMarkStageTime(info.getMarkStageTime());
    gcStats.setPauseState();
    fireGCStatsEvent(gcStats);

  }

  public void garbageCollectorMarkComplete(GarbageCollectionInfo info) {
    GCStatsImpl gcStats = getGCStats(info);
    gcStats.setCandidateGarbageCount(info.getCandidateGarbageCount());
    gcStats.setActualGarbageCount(info.getActualGarbageCount());
    gcStats.setPausedStageTime(info.getPausedStageTime());
    gcStats.setMarkCompleteState();
    fireGCStatsEvent(gcStats);

  }

  public void garbageCollectorDelete(GarbageCollectionInfo info) {
    GCStatsImpl gcStats = getGCStats(info);
    gcStats.setDeleteState();
    fireGCStatsEvent(gcStats);
  }

  public void garbageCollectorCompleted(GarbageCollectionInfo info) {
    GCStatsImpl gcStats = getGCStats(info);
    gcStats.setDeleteStageTime(info.getDeleteStageTime());
    gcStats.setElapsedTime(info.getElapsedTime());
    gcStats.setCompleteState();
    push(gcStats);
    fireGCStatsEvent(gcStats);
  }

  private GCStatsImpl getGCStats(GarbageCollectionInfo info) {
    GCStatsImpl gcStats = null;
    if ((gcStats = (GCStatsImpl) info.getObject()) == null) {
      gcStats = new GCStatsImpl(info.getIteration(), info.isFullGC(), info.getStartTime());
      info.setObject(gcStats);
    }
    return gcStats;
  }

  private void push(Object obj) {
    gcHistory.push(obj);
  }

  public void fireGCStatsEvent(GCStats gcStats) {
    for (Iterator iter = gcStatsEventListeners.iterator(); iter.hasNext();) {
      GCStatsEventListener listener = (GCStatsEventListener) iter.next();
      listener.update(gcStats);
    }
  }

}
