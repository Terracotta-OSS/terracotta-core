/* 
 * The contents of this file are subject to the Terracotta Public License Version
 * 2.0 (the "License"); You may not use this file except in compliance with the
 * License. You may obtain a copy of the License at 
 *
 *      http://terracotta.org/legal/terracotta-public-license.
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 * the specific language governing rights and limitations under the License.
 *
 * The Covered Software is Terracotta Platform.
 *
 * The Initial Developer of the Covered Software is 
 *      Terracotta, Inc., a Software AG company
 */
package com.tc.objectserver.dgc.impl;

import com.tc.objectserver.api.GCStats;
import com.tc.objectserver.api.GCStatsEventListener;
import com.tc.objectserver.core.impl.GarbageCollectionID;
import com.tc.objectserver.dgc.api.GCStatsImpl;
import com.tc.objectserver.dgc.api.GarbageCollectionInfo;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.CopyOnWriteArrayList;

public class GCStatsEventPublisher extends GarbageCollectorEventListenerAdapter {

  private final List<GCStatsEventListener>               gcStatsEventListeners = new CopyOnWriteArrayList<GCStatsEventListener>();

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
    if (info.isInlineDGC()) { return; }
    GCStatsImpl gcStats = getGCStats(info);
    push(info.getGarbageCollectionID(), gcStats);
    fireGCStatsEvent(gcStats);
  }

  @Override
  public void garbageCollectorMark(GarbageCollectionInfo info) {
    if (info.isInlineDGC()) { return; }
    GCStatsImpl gcStats = getGCStats(info);
    gcStats.setMarkState();
    fireGCStatsEvent(gcStats);
  }

  @Override
  public void garbageCollectorPausing(GarbageCollectionInfo info) {
    if (info.isInlineDGC()) { return; }
    GCStatsImpl gcStats = getGCStats(info);
    gcStats.setPauseState();
    fireGCStatsEvent(gcStats);

  }

  @Override
  public void garbageCollectorMarkComplete(GarbageCollectionInfo info) {
    if (info.isInlineDGC()) { return; }
    GCStatsImpl gcStats = getGCStats(info);
    gcStats.setMarkCompleteState();
    gcStats.setActualGarbageCount(info.getActualGarbageCount());
    fireGCStatsEvent(gcStats);
  }

  @Override
  public void garbageCollectorCompleted(GarbageCollectionInfo info) {
    if (info.isInlineDGC()) { return; }
    GCStatsImpl gcStats = getGCStats(info);
    gcStats.setEndObjectCount(info.getEndObjectCount());
    gcStats.setCompleteState();
    fireGCStatsEvent(gcStats);
  }

  @Override
  public void garbageCollectorCanceled(GarbageCollectionInfo info) {
    if (info.isInlineDGC()) { return; }
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
    for (GCStatsEventListener listener : gcStatsEventListeners) {
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
      return size() >= size;
    }
  }

}
