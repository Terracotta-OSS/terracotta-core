/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.dgc.impl;

import com.tc.objectserver.api.GCStats;
import com.tc.objectserver.api.GCStatsEventListener;
import com.tc.objectserver.core.impl.GarbageCollectionID;
import com.tc.objectserver.dgc.api.GCStatsImpl;
import com.tc.objectserver.dgc.api.GarbageCollectionInfo;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.CopyOnWriteArrayList;

public class GCStatsEventPublisher extends GarbageCollectorEventListenerAdapter {

  private final List               gcStatsEventListeners = new CopyOnWriteArrayList();

  private final LossyLinkedHashMap gcHistory             = new LossyLinkedHashMap(1500);
  private GCStats                  lastGCStat            = null;

  public void addListener(GCStatsEventListener listener) {
    gcStatsEventListeners.add(listener);
  }

  public GCStats[] getGarbageCollectorStats() {
    return gcHistory.values().toArray(new GCStats[gcHistory.size()]);
  }

  public GCStats getLastGarbageCollectorStats() {
    return this.lastGCStat;
  }

  @Override
  public void garbageCollectorStart(GarbageCollectionInfo info) {
    GCStatsImpl gcStats = getGCStats(info);
    push(info.getGarbageCollectionID(), gcStats);
    fireGCStatsEvent(gcStats);
  }

  @Override
  public void garbageCollectorMark(GarbageCollectionInfo info) {
    GCStatsImpl gcStats = getGCStats(info);
    gcStats.setMarkState();
    fireGCStatsEvent(gcStats);
  }

  @Override
  public void garbageCollectorPausing(GarbageCollectionInfo info) {
    GCStatsImpl gcStats = getGCStats(info);
    gcStats.setPauseState();
    fireGCStatsEvent(gcStats);

  }

  @Override
  public void garbageCollectorMarkComplete(GarbageCollectionInfo info) {
    GCStatsImpl gcStats = getGCStats(info);
    gcStats.setMarkCompleteState();
    gcStats.setActualGarbageCount(info.getActualGarbageCount());
    fireGCStatsEvent(gcStats);
  }

  @Override
  public void garbageCollectorDelete(GarbageCollectionInfo info) {
    GCStatsImpl gcStats = getGCStats(info);
    gcStats.setDeleteState();
    fireGCStatsEvent(gcStats);
  }

  @Override
  public void garbageCollectorCompleted(GarbageCollectionInfo info) {
    GCStatsImpl gcStats = getGCStats(info);
    gcStats.setEndObjectCount(info.getEndObjectCount());
    gcStats.setCompleteState();
    fireGCStatsEvent(gcStats);
  }

  @Override
  public void garbageCollectorCanceled(GarbageCollectionInfo info) {
    GCStatsImpl gcStats = getGCStats(info);
    gcStats.setCanceledState();
    fireGCStatsEvent(gcStats);
  }

  private GCStatsImpl getGCStats(GarbageCollectionInfo info) {
    GCStatsImpl gcStats = null;
    if ((gcStats = gcHistory.get(info.getGarbageCollectionID())) == null) {
      gcStats = new GCStatsImpl(info.getIteration(), info.isFullGC(), info.getStartTime());
      push(info.getGarbageCollectionID(), gcStats);
    }
    gcStats.setActualGarbageCount(info.getActualGarbageCount());
    gcStats.setBeginObjectCount(info.getBeginObjectCount());
    gcStats.setCandidateGarbageCount(info.getCandidateGarbageCount());
    gcStats.setDeleteStageTime(info.getDeleteStageTime());
    gcStats.setElapsedTime(info.getElapsedTime());
    gcStats.setMarkStageTime(info.getMarkStageTime());
    gcStats.setPausedStageTime(info.getPausedStageTime());
    gcStats.setEndObjectCount(info.getEndObjectCount());
    return gcStats;
  }

  private void push(GarbageCollectionID id, GCStatsImpl stats) {
    this.lastGCStat = stats;
    gcHistory.put(id, stats);
  }

  public void fireGCStatsEvent(GCStats gcStats) {
    for (Iterator iter = gcStatsEventListeners.iterator(); iter.hasNext();) {
      GCStatsEventListener listener = (GCStatsEventListener) iter.next();
      listener.update(gcStats);
    }
  }

  private static class LossyLinkedHashMap extends LinkedHashMap<GarbageCollectionID, GCStatsImpl> {

    private final int size;

    public LossyLinkedHashMap(int size) {
      this.size = size;
    }

    @Override
    protected boolean removeEldestEntry(Entry<GarbageCollectionID, GCStatsImpl> eldest) {
      return (size() < size) ? false : true;
    }

  }

}
