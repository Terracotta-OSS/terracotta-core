/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.core.impl;


import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.object.ObjectID;
import com.tc.objectserver.api.ObjectManager;
import com.tc.objectserver.context.GCResultContext;
import com.tc.objectserver.core.api.Filter;
import com.tc.objectserver.core.api.GarbageCollector;
import com.tc.objectserver.core.api.GarbageCollectorEventListener;
import com.tc.objectserver.core.api.ManagedObject;
import com.tc.objectserver.l1.api.ClientStateManager;
import com.tc.objectserver.managedobject.ManagedObjectChangeListener;
import com.tc.text.PrettyPrintable;
import com.tc.text.PrettyPrinter;
import com.tc.util.ObjectIDSet;
import com.tc.util.State;
import com.tc.util.TCCollections;
import com.tc.util.concurrent.LifeCycleState;
import com.tc.util.concurrent.NullLifeCycleState;
import com.tc.util.concurrent.StoppableThread;
import com.tc.util.concurrent.ThreadUtil;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.concurrent.atomic.AtomicInteger;

/**
 */
public class MarkAndSweepGarbageCollector implements GarbageCollector {

  private static final TCLogger                    logger                = TCLogging
                                                                             .getLogger(MarkAndSweepGarbageCollector.class);
  private static final ChangeCollector             NULL_CHANGE_COLLECTOR = new ChangeCollector() {
                                                                           public void changed(ObjectID changedObject,
                                                                                               ObjectID oldReference,
                                                                                               ObjectID newReference) {
                                                                             return;
                                                                           }

                                                                           public void addNewReferencesTo(Set set) {
                                                                             return;
                                                                           }

                                                                           public PrettyPrinter prettyPrint(PrettyPrinter out) {
                                                                             return out
                                                                                 .println("NULL CHANGE COLLECTOR");
                                                                           }
                                                                         };
  private static final Filter                      NULL_FILTER           = new Filter() {
                                                                           public boolean shouldVisit(ObjectID referencedObject) {
                                                                             return true;
                                                                           }
                                                                         };
  private static final LifeCycleState              NULL_LIFECYCLE_STATE  = new NullLifeCycleState();

  private static final State                       GC_DISABLED           = new State("GC_DISABLED");
  private static final State                       GC_RUNNING            = new State("GC_RUNNING");
  private static final State                       GC_SLEEP              = new State("GC_SLEEP");
  private static final State                       GC_PAUSING            = new State("GC_PAUSING");
  private static final State                       GC_PAUSED             = new State("GC_PAUSED");
  private static final State                       GC_DELETE             = new State("GC_DELETE");

  private final GarbageCollectionInfoPublisherImpl gcPublisher           = new GarbageCollectionInfoPublisherImpl();
  private final AtomicInteger                      gcIterationCounter    = new AtomicInteger(0);
  private final ObjectManager                      objectManager;
  private final ClientStateManager                 stateManager;

  private State                                    state                 = GC_SLEEP;
  private LifeCycleState                           lifeCycleState;
  private volatile ChangeCollector                 referenceCollector    = NULL_CHANGE_COLLECTOR;
  private LifeCycleState                           gcState               = new NullLifeCycleState();
  private volatile boolean                         started               = false;

  public MarkAndSweepGarbageCollector(ObjectManager objectManager, ClientStateManager stateManager, boolean verboseGC) {
    this.objectManager = objectManager;
    this.stateManager = stateManager;
    addListener(new GCLoggerEventPublisher(logger, verboseGC));
  }

  private ObjectIDSet rescue(final ObjectIDSet gcResults, final List rescueTimes) {
    long start = System.currentTimeMillis();
    Set rescueIds = new ObjectIDSet();
    stateManager.addAllReferencedIdsTo(rescueIds);
    int stateManagerIds = rescueIds.size();

    addNewReferencesTo(rescueIds);
    int referenceCollectorIds = rescueIds.size() - stateManagerIds;

    logger.debug("rescueIds: " + rescueIds.size() + ", stateManagerIds: " + stateManagerIds
                 + ", additional referenceCollectorIds: " + referenceCollectorIds);

    rescueIds.retainAll(gcResults);

    Filter rescueFilter = new Filter() {
      public boolean shouldVisit(ObjectID referencedObject) {
        return gcResults.contains(referencedObject);
      }
    };

    ObjectIDSet rv = collect(rescueFilter, rescueIds, gcResults, gcState);
    rescueTimes.add(new Long(System.currentTimeMillis() - start));
    return rv;
  }

  /**
   * GC_SLEEP --> GC_RUNNING --> GC_PAUSING --> GC_PAUSED --> GC_SWEEP
   */
  public void gc() {

    while (!requestGCStart()) {
      logger.info("GC: Not running gc since its disabled. Waiting for 1 min before checking again ...");
      ThreadUtil.reallySleep(60000);
    }

    int gcIteration = gcIterationCounter.incrementAndGet();
    GarbageCollectionInfoImpl gcInfo = new GarbageCollectionInfoImpl(gcIteration);
    gcInfo.markFullGen();

    long startMillis = System.currentTimeMillis();
    gcInfo.setStartTime(startMillis);
    gcPublisher.fireGCStartEvent(gcInfo);

    // NOTE:It is important to set this reference collector before getting the roots ID and all object ids
    this.referenceCollector = new NewReferenceCollector();

    Set rootIDs = objectManager.getRootIDs();
    ObjectIDSet managedIDs = objectManager.getAllObjectIDs();

    gcInfo.setBeginObjectCount(managedIDs.size());

    if (gcState.isStopRequested()) { return; }

    gcPublisher.fireGCMarkEvent(gcInfo);
    ObjectIDSet gcResults = collect(NULL_FILTER, rootIDs, managedIDs, gcState);
    gcInfo.setPreRescueCount(gcResults.size());
    gcPublisher.fireGCMarkResultsEvent(gcInfo);

    if (gcState.isStopRequested()) { return; }

    List rescueTimes = new ArrayList();

    gcResults = rescue(gcResults, rescueTimes);
    gcInfo.setRescue1Count(gcResults.size());
    gcPublisher.fireGCRescue1CompleteEvent(gcInfo);

    gcInfo.setMarkStageTime(System.currentTimeMillis() - startMillis);

    gcPublisher.fireGCPausingEvent(gcInfo);
    requestGCPause();

    if (gcState.isStopRequested()) { return; }

    objectManager.waitUntilReadyToGC();

    if (gcState.isStopRequested()) { return; }

    long pauseStartMillis = System.currentTimeMillis();

    gcPublisher.fireGCPausedEvent(gcInfo);

    gcInfo.setCandidateGarbageCount(gcResults.size());
    gcPublisher.fireGCRescue2StartEvent(gcInfo);
    SortedSet toDelete = Collections.unmodifiableSortedSet(rescue(new ObjectIDSet(gcResults), rescueTimes));
    gcInfo.setRescueTimes(rescueTimes);
    gcInfo.setDeleted(toDelete);

    if (gcState.isStopRequested()) { return; }

    long deleteStartMillis = System.currentTimeMillis();
    gcInfo.setPausedStageTime(deleteStartMillis - pauseStartMillis);

    gcPublisher.fireGCMarkCompleteEvent(gcInfo);

    this.referenceCollector = NULL_CHANGE_COLLECTOR;

    // Delete Garbage
    GCResultContext gcResultContext = new GCResultContext(gcIteration, toDelete);
    gcResultContext.setGcInfo(gcInfo);
    gcResultContext.setGcPublisher(gcPublisher);
    deleteGarbage(gcResultContext);

    long endMillis = System.currentTimeMillis();
    gcInfo.setElapsedTime(endMillis - gcInfo.getStartTime());
    gcPublisher.fireGCCycleCompletedEvent(gcInfo);

  }

  public boolean deleteGarbage(GCResultContext gcResult) {
    if (requestGCDeleteStart()) {
      objectManager.notifyGCComplete(gcResult);
      notifyGCComplete();
      return true;
    }
    return false;
  }

  public void changed(ObjectID changedObject, ObjectID oldReference, ObjectID newReference) {
    referenceCollector.changed(changedObject, oldReference, newReference);
  }

  public ObjectIDSet collect(Filter filter, Collection rootIds, ObjectIDSet managedObjectIds) {
    return collect(filter, rootIds, managedObjectIds, NULL_LIFECYCLE_STATE);
  }

  public ObjectIDSet collect(Filter filter, Collection rootIds, ObjectIDSet managedObjectIds,
                             LifeCycleState aLifeCycleState) {
    this.lifeCycleState = aLifeCycleState;

    long start = System.currentTimeMillis();
    logstart_collect(rootIds, managedObjectIds);

    for (Iterator i = rootIds.iterator(); i.hasNext();) {
      ObjectID rootId = (ObjectID) i.next();
      managedObjectIds.remove(rootId);
      if (lifeCycleState.isStopRequested()) return TCCollections.EMPTY_OBJECT_ID_SET;
      collectRoot(filter, rootId, managedObjectIds);
    }

    profile_collect(start);

    return managedObjectIds;
  }

  private void collectRoot(Filter filter, ObjectID rootId, Set managedObjectIds) {
    Set toBeVisited = new ObjectIDSet();
    toBeVisited.add(rootId);

    while (!toBeVisited.isEmpty()) {

      for (Iterator i = new ObjectIDSet(toBeVisited).iterator(); i.hasNext();) {
        ObjectID id = (ObjectID) i.next();
        if (lifeCycleState.isStopRequested()) return;
        ManagedObject obj = objectManager.getObjectByIDOrNull(id);
        toBeVisited.remove(id);
        if (obj == null) {
          logger.warn("Looked up a new Object before its initialized, skipping : " + id);
          continue;
        }

        for (Iterator r = obj.getObjectReferences().iterator(); r.hasNext();) {
          ObjectID mid = (ObjectID) r.next();

          if (mid == null) {
            // see CDV-765
            logger.error("null value returned from getObjectReferences() on " + obj);
            continue;
          }

          if (mid.isNull() || !managedObjectIds.contains(mid)) continue;
          if (filter.shouldVisit(mid)) toBeVisited.add(mid);
          managedObjectIds.remove(mid);
        }
        objectManager.releaseReadOnly(obj);
      }
    }
  }

  private synchronized boolean requestGCStart() {
    if (started && state == GC_SLEEP) {
      state = GC_RUNNING;
      return true;
    }
    // Can't start GC
    return false;
  }

  public synchronized void enableGC() {
    if (GC_DISABLED == state) {
      state = GC_SLEEP;
    } else {
      logger.warn("GC is already enabled : " + state);
    }
  }

  public synchronized boolean disableGC() {
    if (GC_SLEEP == state) {
      state = GC_DISABLED;
      return true;
    }
    // GC is already running, can't be disabled
    return false;
  }

  public synchronized void notifyReadyToGC() {
    if (state == GC_PAUSING) {
      state = GC_PAUSED;
    }
  }

  public synchronized void notifyGCComplete() {
    state = GC_SLEEP;
  }

  /**
   * In Active server, state transitions from GC_PAUSED to GC_DELETE and in the passive server, state transitions from
   * GC_SLEEP to GC_DELETE.
   */
  private synchronized boolean requestGCDeleteStart() {
    if (state == GC_SLEEP || state == GC_PAUSED) {
      state = GC_DELETE;
      return true;
    }
    return false;
  }

  public synchronized void requestGCPause() {
    state = GC_PAUSING;
  }

  public synchronized boolean isPausingOrPaused() {
    return GC_PAUSED == state || GC_PAUSING == state;
  }

  public synchronized boolean isPaused() {
    return state == GC_PAUSED;
  }

  public synchronized boolean isDisabled() {
    return GC_DISABLED == state;
  }

  public synchronized PrettyPrinter prettyPrint(PrettyPrinter out) {
    return out.print(getClass().getName()).print("[").print(state).print("]");
  }

  private void logstart_collect(Collection rootIds, Set managedObjectIds) {
    if (logger.isDebugEnabled()) logger.debug("collect(): rootIds=" + rootIds.size() + ", managedObjectIds="
                                              + managedObjectIds.size());
  }

  private void profile_collect(long start) {
    if (logger.isDebugEnabled()) logger.debug("collect: " + (System.currentTimeMillis() - start) + " ms.");
  }

  private static class NewReferenceCollector implements ChangeCollector {

    Set newReferences = new ObjectIDSet();

    public void changed(ObjectID changedObject, ObjectID oldReference, ObjectID newReference) {
      synchronized (newReferences) {
        newReferences.add(newReference);
      }
    }

    public void addNewReferencesTo(Set set) {
      long start = System.currentTimeMillis();
      synchronized (newReferences) {
        set.addAll(newReferences);
      }
      profile_addNewReferencesTo(start);
    }

    private void profile_addNewReferencesTo(long start) {
      if (logger.isDebugEnabled()) {
        logger.debug("addNewReferencesTo: " + (System.currentTimeMillis() - start) + " ms.");
      }
    }

    public PrettyPrinter prettyPrint(PrettyPrinter out) {
      synchronized (newReferences) {
        return out.println("newReferences: ").println(newReferences);
      }
    }
  }

  public void addNewReferencesTo(Set rescueIds) {
    referenceCollector.addNewReferencesTo(rescueIds);
  }

  public void start() {
    this.started = true;
    gcState.start();
  }

  public void stop() {
    this.started = false;
    int count = 0;
    while (!this.gcState.stopAndWait(5000) && (count < 6)) {
      count++;
      logger.warn("GC Thread did not stop");
    }
  }

  public boolean isStarted() {
    return this.started;
  }

  public void setState(StoppableThread st) {
    this.gcState = st;
  }

  public void addListener(GarbageCollectorEventListener listener) {
    gcPublisher.addListener(listener);
  }

  private interface ChangeCollector extends ManagedObjectChangeListener, PrettyPrintable {
    public void addNewReferencesTo(Set set);
  }
}