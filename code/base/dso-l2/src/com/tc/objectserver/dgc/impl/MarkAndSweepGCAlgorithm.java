/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.objectserver.dgc.impl;

import com.tc.object.ObjectID;
import com.tc.objectserver.context.GCResultContext;
import com.tc.objectserver.core.api.Filter;
import com.tc.objectserver.dgc.api.GarbageCollectionInfo;
import com.tc.objectserver.dgc.api.GarbageCollectionInfoPublisher;
import com.tc.objectserver.dgc.api.GarbageCollector;
import com.tc.util.ObjectIDSet;
import com.tc.util.TCCollections;
import com.tc.util.concurrent.LifeCycleState;
import com.tc.util.concurrent.ThreadUtil;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;

final class MarkAndSweepGCAlgorithm {

  private final GCHook                         gcHook;
  private final int                            gcIteration;
  private final GarbageCollector               collector;
  private final GarbageCollectionInfoPublisher gcPublisher;
  private final LifeCycleState                 gcState;

  public MarkAndSweepGCAlgorithm(GarbageCollector collector, GCHook gcHook,
                                 GarbageCollectionInfoPublisher gcPublisher, LifeCycleState gcState, int gcIteration) {
    this.collector = collector;
    this.gcHook = gcHook;
    this.gcPublisher = gcPublisher;
    this.gcState = gcState;
    this.gcIteration = gcIteration;
  }

  void doGC() {
    while (!collector.requestGCStart()) {
      MarkAndSweepGarbageCollector.logger.info(gcHook.getDescription()
                  + " GC: It is either disabled or is already running. Waiting for 1 min before checking again ...");
      ThreadUtil.reallySleep(60000);
    }

    GarbageCollectionInfo gcInfo = gcHook.getGCInfo(gcIteration);
    long startMillis = System.currentTimeMillis();
    gcInfo.setStartTime(startMillis);
    gcPublisher.fireGCStartEvent(gcInfo);

    // NOTE:It is important to set this reference collector before getting the roots ID and all object ids
    gcHook.startMonitoringReferenceChanges();

    final ObjectIDSet candidateIDs = gcHook.getGCCandidates();
    final Set rootIDs = gcHook.getRootObjectIDs(candidateIDs);

    gcInfo.setBeginObjectCount(candidateIDs.size());
    gcPublisher.fireGCMarkEvent(gcInfo);

    if (gcState.isStopRequested()) { return; }

    ObjectIDSet gcResults = collect(gcHook.getCollectCycleFilter(candidateIDs), rootIDs, candidateIDs, gcState);
    gcInfo.setPreRescueCount(gcResults.size());
    gcPublisher.fireGCMarkResultsEvent(gcInfo);

    if (gcState.isStopRequested()) { return; }

    List rescueTimes = new ArrayList();

    gcResults = rescue(gcResults, rescueTimes);
    gcInfo.setRescue1Count(gcResults.size());
    gcInfo.setMarkStageTime(System.currentTimeMillis() - startMillis);
    gcPublisher.fireGCRescue1CompleteEvent(gcInfo);

    if (gcResults.isEmpty()) {
      // No garbage, short circuit GC cycle, don't pass objectMgr etc.
      gcHook.stopMonitoringReferenceChanges();
      collector.notifyGCComplete();
      shortCircuitGCComplete(gcInfo, rescueTimes);
      return;
    }

    gcPublisher.fireGCPausingEvent(gcInfo);
    collector.requestGCPause();

    if (gcState.isStopRequested()) { return; }

    gcHook.waitUntilReadyToGC();

    if (gcState.isStopRequested()) { return; }

    long pauseStartMillis = System.currentTimeMillis();
    gcPublisher.fireGCPausedEvent(gcInfo);

    gcInfo.setCandidateGarbageCount(gcResults.size());
    gcPublisher.fireGCRescue2StartEvent(gcInfo);
    SortedSet toDelete = Collections.unmodifiableSortedSet(rescue(new ObjectIDSet(gcResults), rescueTimes));
    gcInfo.setRescueTimes(rescueTimes);
    gcInfo.setDeleted(toDelete);

    if (gcState.isStopRequested()) { return; }

    gcHook.stopMonitoringReferenceChanges();

    long deleteStartMillis = System.currentTimeMillis();
    gcInfo.setPausedStageTime(deleteStartMillis - pauseStartMillis);
    gcPublisher.fireGCMarkCompleteEvent(gcInfo);

    // Delete Garbage
    collector.deleteGarbage(new GCResultContext(gcIteration, toDelete, gcInfo, gcPublisher));

    long endMillis = System.currentTimeMillis();
    gcInfo.setTotalMarkCycleTime(endMillis - gcInfo.getStartTime());
    gcPublisher.fireGCCycleCompletedEvent(gcInfo);
  }

  private void shortCircuitGCComplete(GarbageCollectionInfo gcInfo, List rescueTimes) {
    gcInfo.setCandidateGarbageCount(0);
    gcInfo.setRescueTimes(rescueTimes);
    gcInfo.setDeleted(TCCollections.EMPTY_SORTED_SET);
    gcInfo.setPausedStageTime(0);
    gcInfo.setDeleteStageTime(0);
    long endMillis = System.currentTimeMillis();
    gcInfo.setTotalMarkCycleTime(endMillis - gcInfo.getStartTime());
    gcInfo.setElapsedTime(endMillis - gcInfo.getStartTime());
    gcPublisher.fireGCCycleCompletedEvent(gcInfo);
    gcPublisher.fireGCCompletedEvent(gcInfo);
  }

  public ObjectIDSet collect(Filter filter, Collection rootIds, ObjectIDSet managedObjectIds,
                             LifeCycleState lifeCycleState) {
    long start = System.currentTimeMillis();
    logstart_collect(rootIds, managedObjectIds);

    for (Iterator i = rootIds.iterator(); i.hasNext() && !managedObjectIds.isEmpty();) {
      ObjectID rootId = (ObjectID) i.next();
      managedObjectIds.remove(rootId);
      if (lifeCycleState.isStopRequested()) return TCCollections.EMPTY_OBJECT_ID_SET;
      collectRoot(filter, rootId, managedObjectIds, lifeCycleState);
    }

    profile_collect(start);

    return managedObjectIds;
  }

  private void collectRoot(Filter filter, ObjectID rootId, Set managedObjectIds, LifeCycleState lifeCycleState) {
    Set toBeVisited = new ObjectIDSet();
    toBeVisited.add(rootId);

    while (!toBeVisited.isEmpty() && !managedObjectIds.isEmpty()) {

      for (Iterator i = new ObjectIDSet(toBeVisited).iterator(); i.hasNext() && !managedObjectIds.isEmpty();) {
        ObjectID id = (ObjectID) i.next();
        if (lifeCycleState.isStopRequested()) return;
        Set references = gcHook.getObjectReferencesFrom(id);
        toBeVisited.remove(id);

        for (Iterator r = references.iterator(); r.hasNext();) {
          ObjectID mid = (ObjectID) r.next();
          if (mid == null) {
            // see CDV-765
            MarkAndSweepGarbageCollector.logger.error("null value returned from getObjectReferences() on " + id);
            continue;
          }
          if (mid.isNull() || !managedObjectIds.contains(mid)) continue;
          if (filter.shouldVisit(mid)) toBeVisited.add(mid);
          managedObjectIds.remove(mid);
        }
      }
    }
  }

  private ObjectIDSet rescue(final ObjectIDSet gcResults, final List rescueTimes) {
    long start = System.currentTimeMillis();
    Set rescueIds = gcHook.getRescueIDs();
    rescueIds.retainAll(gcResults);

    Filter rescueFilter = new SelectiveFilter(gcResults);
    ObjectIDSet rv = collect(rescueFilter, rescueIds, gcResults, gcState);
    rescueTimes.add(new Long(System.currentTimeMillis() - start));
    return rv;
  }

  private void logstart_collect(Collection rootIds, Set managedObjectIds) {
    if (MarkAndSweepGarbageCollector.logger.isDebugEnabled()) MarkAndSweepGarbageCollector.logger.debug("collect(): rootIds=" + rootIds.size() + ", managedObjectIds="
                                              + managedObjectIds.size());
  }

  private void profile_collect(long start) {
    if (MarkAndSweepGarbageCollector.logger.isDebugEnabled()) MarkAndSweepGarbageCollector.logger.debug("collect: " + (System.currentTimeMillis() - start) + " ms.");
  }

}