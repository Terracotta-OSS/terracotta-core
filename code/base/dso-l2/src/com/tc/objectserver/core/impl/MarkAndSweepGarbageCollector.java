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
import com.tc.objectserver.impl.ObjectManagerConfig;
import com.tc.objectserver.l1.api.ClientStateManager;
import com.tc.objectserver.managedobject.ManagedObjectChangeListener;
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
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicInteger;

/**
 */
public class MarkAndSweepGarbageCollector implements GarbageCollector {

  private static final TCLogger                    logger                      = TCLogging
                                                                                   .getLogger(MarkAndSweepGarbageCollector.class);

  private static final ChangeCollector             NULL_CHANGE_COLLECTOR       = new ChangeCollector() {
                                                                                 public void changed(
                                                                                                     ObjectID changedObject,
                                                                                                     ObjectID oldReference,
                                                                                                     ObjectID newReference) {
                                                                                   return;
                                                                                 }

                                                                                 public Set addNewReferencesTo(Set set) {
                                                                                   return Collections.EMPTY_SET;
                                                                                 }
                                                                               };

  private static final YoungGenChangeCollector     NULL_YOUNG_CHANGE_COLLECTOR = new YoungGenChangeCollector() {

                                                                                 public void notifyObjectCreated(
                                                                                                                 ObjectID id) {
                                                                                   return;
                                                                                 }

                                                                                 public void notifyObjectsEvicted(
                                                                                                                  Collection evicted) {
                                                                                   return;
                                                                                 }

                                                                                 public Set addYoungGenCandidateObjectIDsTo(
                                                                                                                            Set set) {
                                                                                   return Collections.EMPTY_SET;
                                                                                 }

                                                                                 public void notifyObjectInitalized(
                                                                                                                    ObjectID id) {
                                                                                   return;
                                                                                 }

                                                                                 public Set getRememberedSet() {
                                                                                   return Collections.EMPTY_SET;
                                                                                 }

                                                                                 public void removeGarbage(SortedSet ids) {
                                                                                   return;
                                                                                 }
                                                                               };

  private static final Filter                      NULL_FILTER                 = new Filter() {
                                                                                 public boolean shouldVisit(
                                                                                                            ObjectID referencedObject) {
                                                                                   return true;
                                                                                 }
                                                                               };
  private static final LifeCycleState              NULL_LIFECYCLE_STATE        = new NullLifeCycleState();

  private static final State                       GC_DISABLED                 = new State("GC_DISABLED");
  private static final State                       GC_RUNNING                  = new State("GC_RUNNING");
  private static final State                       GC_SLEEP                    = new State("GC_SLEEP");
  private static final State                       GC_PAUSING                  = new State("GC_PAUSING");
  private static final State                       GC_PAUSED                   = new State("GC_PAUSED");
  private static final State                       GC_DELETE                   = new State("GC_DELETE");

  private final AtomicInteger                      gcIterationCounter          = new AtomicInteger(0);
  private final ObjectManager                      objectManager;
  private final ClientStateManager                 stateManager;
  private final GarbageCollectionInfoPublisherImpl gcPublisher                 = new GarbageCollectionInfoPublisherImpl();
  private final ObjectManagerConfig                objectManagerConfig;

  private State                                    state                       = GC_SLEEP;
  private volatile ChangeCollector                 referenceCollector          = NULL_CHANGE_COLLECTOR;
  private volatile YoungGenChangeCollector         youngGenReferenceCollector  = NULL_YOUNG_CHANGE_COLLECTOR;
  private volatile LifeCycleState                  gcState                     = new NullLifeCycleState();
  private volatile boolean                         started                     = false;

  public MarkAndSweepGarbageCollector(ObjectManager objectManager, ClientStateManager stateManager,
                                      ObjectManagerConfig objectManagerConfig) {
    this.objectManagerConfig = objectManagerConfig;
    this.objectManager = objectManager;
    this.stateManager = stateManager;
    addListener(new GCLoggerEventPublisher(logger, objectManagerConfig.verboseGC()));
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

    Filter rescueFilter = new SelectiveFilter(gcResults);
    ObjectIDSet rv = collect(rescueFilter, rescueIds, gcResults, gcState);
    rescueTimes.add(new Long(System.currentTimeMillis() - start));
    return rv;
  }

  /**
   * For state transition diagram look here. http://intranet.terracotta.lan/xwiki/bin/view/Main/DGC+Lifecycle
   */
  public void gc() {
    doGC(true);
  }

  private void doGC(boolean fullGC) {
    while (!requestGCStart()) {
      logger.info((fullGC ? "Full" : "YoungGen")
                  + " GC: It is either disabled or is already running. Waiting for 1 min before checking again ...");
      ThreadUtil.reallySleep(60000);
    }

    int gcIteration = gcIterationCounter.incrementAndGet();
    GarbageCollectionInfo gcInfo = new GarbageCollectionInfo(gcIteration, fullGC);
    long startMillis = System.currentTimeMillis();
    gcInfo.setStartTime(startMillis);
    gcPublisher.fireGCStartEvent(gcInfo);

    // NOTE:It is important to set this reference collector before getting the roots ID and all object ids
    this.referenceCollector = new NewReferenceCollector();

    final ObjectIDSet candidateIDs = getGCCandidates(fullGC);
    final Set rootIDs = getRootObjectIDs(fullGC, candidateIDs);

    gcInfo.setBeginObjectCount(candidateIDs.size());
    gcPublisher.fireGCMarkEvent(gcInfo);

    if (gcState.isStopRequested()) { return; }

    Filter filter = (fullGC ? NULL_FILTER : new SelectiveFilter(candidateIDs));
    ObjectIDSet gcResults = collect(filter, rootIDs, candidateIDs, gcState);
    gcInfo.setPreRescueCount(gcResults.size());
    gcPublisher.fireGCMarkResultsEvent(gcInfo);

    if (gcState.isStopRequested()) { return; }

    List rescueTimes = new ArrayList();

    gcResults = rescue(gcResults, rescueTimes);
    gcInfo.setRescue1Count(gcResults.size());
    gcInfo.setMarkStageTime(System.currentTimeMillis() - startMillis);
    gcPublisher.fireGCRescue1CompleteEvent(gcInfo);

    if(gcResults.isEmpty()) {
      // No garbage, short circuit GC cycle, don't pass objectMgr etc. 
      this.referenceCollector = NULL_CHANGE_COLLECTOR;
      notifyGCComplete();
      shortCircuitGCComplete(gcInfo, rescueTimes);
      return;
    }
    
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

    this.referenceCollector = NULL_CHANGE_COLLECTOR;

    long deleteStartMillis = System.currentTimeMillis();
    gcInfo.setPausedStageTime(deleteStartMillis - pauseStartMillis);
    gcPublisher.fireGCMarkCompleteEvent(gcInfo);

    // Delete Garbage
    deleteGarbage(new GCResultContext(gcIteration, toDelete, gcInfo, gcPublisher));

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

  private Set getRootObjectIDs(boolean fullGC, Set candidateIDs) {
    if (fullGC) {
      return objectManager.getRootIDs();
    } else {
      return getYoungGenRootIDs(candidateIDs);
    }
  }

  private ObjectIDSet getGCCandidates(boolean fullGC) {
    if (fullGC) {
      return objectManager.getAllObjectIDs();
    } else {
      return (ObjectIDSet) youngGenReferenceCollector.addYoungGenCandidateObjectIDsTo(new ObjectIDSet());
    }
  }

  public void gcYoung() {
    doGC(false);
  }

  private Set getYoungGenRootIDs(Set candidateIDs) {
    Set idsInMemory = objectManager.getObjectIDsInCache();
    idsInMemory.removeAll(candidateIDs);
    Set roots = objectManager.getRootIDs();
    Set youngGenRoots = youngGenReferenceCollector.getRememberedSet();
    youngGenRoots.addAll(roots);
    youngGenRoots.addAll(idsInMemory);
    return youngGenRoots;
  }

  public boolean deleteGarbage(GCResultContext gcResult) {
    if (requestGCDeleteStart()) {
      youngGenReferenceCollector.removeGarbage(gcResult.getGCedObjectIDs());
      // NOTE:: Important to do this state transition b4 notifying ObjectMgr to avoid hanging lookups.
      notifyGCComplete();
      objectManager.notifyGCComplete(gcResult);
      return true;
    }
    return false;
  }

  public void changed(ObjectID changedObject, ObjectID oldReference, ObjectID newReference) {
    referenceCollector.changed(changedObject, oldReference, newReference);
  }

  public void notifyObjectCreated(ObjectID id) {
    youngGenReferenceCollector.notifyObjectCreated(id);
  }

  public void notifyNewObjectInitalized(ObjectID id) {
    youngGenReferenceCollector.notifyObjectInitalized(id);
  }

  public void notifyObjectsEvicted(Collection evicted) {
    youngGenReferenceCollector.notifyObjectsEvicted(evicted);
  }

  public ObjectIDSet collect(Filter filter, Collection rootIds, ObjectIDSet managedObjectIds) {
    return collect(filter, rootIds, managedObjectIds, NULL_LIFECYCLE_STATE);
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
        // TODO:: come back for young gen to see if we can totally avoid faulting
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

    public Set addNewReferencesTo(Set set) {
      long start = System.currentTimeMillis();
      synchronized (newReferences) {
        set.addAll(newReferences);
      }
      profile_addNewReferencesTo(start);
      return set;
    }

    private void profile_addNewReferencesTo(long start) {
      if (logger.isDebugEnabled()) {
        logger.debug("addNewReferencesTo: " + (System.currentTimeMillis() - start) + " ms.");
      }
    }
  }

  private static final class YoungGenChangeCollectorImpl implements YoungGenChangeCollector {

    private static final State UNINITALIZED      = new State("UNINITIALIZED");
    private static final State INITALIZED        = new State("INITIALIZED");

    private final Map          youngGenObjectIDs = new HashMap();
    private final Set          rememberedSet     = new ObjectIDSet();

    public synchronized Set addYoungGenCandidateObjectIDsTo(Set set) {
      for (Iterator i = youngGenObjectIDs.entrySet().iterator(); i.hasNext();) {
        Entry e = (Entry) i.next();
        if (e.getValue() == INITALIZED) {
          set.add(e.getKey());
        }
      }
      return set;
    }

    public synchronized Set getRememberedSet() {
      return new ObjectIDSet(rememberedSet);
    }

    public synchronized void notifyObjectCreated(ObjectID id) {
      Object oldState = youngGenObjectIDs.put(id, UNINITALIZED);
      if (oldState != null) { throw new AssertionError(id + " is already present in " + oldState); }
    }

    public synchronized void notifyObjectInitalized(ObjectID id) {
      Object oldState = youngGenObjectIDs.put(id, INITALIZED);
      if (oldState != UNINITALIZED) { throw new AssertionError(id + " is not in " + UNINITALIZED + " but in "
                                                               + oldState); }
    }

    public synchronized void notifyObjectsEvicted(Collection evicted) {
      for (Iterator i = evicted.iterator(); i.hasNext();) {
        ManagedObject mo = (ManagedObject) i.next();
        ObjectID id = mo.getID();
        removeReferencesTo(id);
        Set references = mo.getObjectReferences();
        references.retainAll(youngGenObjectIDs.keySet());
        rememberedSet.addAll(references);
      }
    }

    private void removeReferencesTo(ObjectID id) {
      youngGenObjectIDs.remove(id);
      rememberedSet.remove(id);
    }

    public synchronized void removeGarbage(SortedSet ids) {
      for (Iterator i = ids.iterator(); i.hasNext();) {
        ObjectID oid = (ObjectID) i.next();
        removeReferencesTo(oid);
      }
    }

  }

  private interface ChangeCollector extends ManagedObjectChangeListener {
    public Set addNewReferencesTo(Set set);
  }

  private interface YoungGenChangeCollector {
    public void notifyObjectsEvicted(Collection evicted);

    public void removeGarbage(SortedSet ids);

    public Set getRememberedSet();

    public void notifyObjectCreated(ObjectID id);

    public void notifyObjectInitalized(ObjectID id);

    public Set addYoungGenCandidateObjectIDsTo(Set set);
  }

  private final static class SelectiveFilter implements Filter {
    private final Set keys;

    public SelectiveFilter(Set keys) {
      this.keys = keys;
    }

    public boolean shouldVisit(ObjectID referencedObject) {
      return keys.contains(referencedObject);
    }
  }

  public void addNewReferencesTo(Set rescueIds) {
    referenceCollector.addNewReferencesTo(rescueIds);
  }

  public void start() {
    if (objectManagerConfig.isYoungGenDGCEnabled()) {
      this.youngGenReferenceCollector = new YoungGenChangeCollectorImpl();
    }
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
}
