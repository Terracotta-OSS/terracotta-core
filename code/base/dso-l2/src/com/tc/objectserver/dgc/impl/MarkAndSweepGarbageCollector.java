/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.dgc.impl;

import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.object.ObjectID;
import com.tc.objectserver.api.ObjectManager;
import com.tc.objectserver.context.GCResultContext;
import com.tc.objectserver.core.api.Filter;
import com.tc.objectserver.core.api.ManagedObject;
import com.tc.objectserver.dgc.api.GarbageCollectionInfo;
import com.tc.objectserver.dgc.api.GarbageCollector;
import com.tc.objectserver.dgc.api.GarbageCollectorEventListener;
import com.tc.objectserver.impl.ObjectManagerConfig;
import com.tc.objectserver.l1.api.ClientStateManager;
import com.tc.objectserver.managedobject.ManagedObjectChangeListener;
import com.tc.text.PrettyPrinter;
import com.tc.util.ObjectIDSet;
import com.tc.util.State;
import com.tc.util.concurrent.LifeCycleState;
import com.tc.util.concurrent.NullLifeCycleState;
import com.tc.util.concurrent.StoppableThread;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicInteger;

/**
 */
public class MarkAndSweepGarbageCollector implements GarbageCollector {

  static final TCLogger                    logger                      = TCLogging
                                                                                   .getLogger(MarkAndSweepGarbageCollector.class);

  private static final ChangeCollector             NULL_CHANGE_COLLECTOR       = new ChangeCollector() {
                                                                                 public void changed(
                                                                                                     ObjectID changedObject,
                                                                                                     ObjectID oldReference,
                                                                                                     ObjectID newReference) {
                                                                                   return;
                                                                                 }

                                                                                 public Set addNewReferencesTo(Set set) {
                                                                                   return set;
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
                                                                                   return set;
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

  /**
   * For state transition diagram look here. http://intranet.terracotta.lan/xwiki/bin/view/Main/DGC+Lifecycle
   */
  public void gc() {
    doGC(new FullGCHook(this, this.objectManager, this.stateManager));
  }

  public void gcYoung() {
    doGC(new YoungGCHook(this, this.objectManager, this.stateManager, this.youngGenReferenceCollector));
  }

  private void doGC(GCHook gcHook) {
    MarkAndSweepGCAlgorithm gcAlgo = new MarkAndSweepGCAlgorithm(this, gcHook, gcPublisher, gcState, gcIterationCounter
        .incrementAndGet());
    gcAlgo.doGC();
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

  private void startMonitoringReferenceChanges() {
    this.referenceCollector = new NewReferenceCollector();
  }

  private void stopMonitoringReferenceChanges() {
    this.referenceCollector = NULL_CHANGE_COLLECTOR;
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

  public void addNewReferencesTo(Set rescueIds) {
    referenceCollector.addNewReferencesTo(rescueIds);
  }

  /**
   * Used for Tests. TODO:: Re-factor tests and remove this method
   */
  public ObjectIDSet collect(Filter filter, Collection rootIds, ObjectIDSet managedObjectIds) {
    return collect(filter, rootIds, managedObjectIds, NULL_LIFECYCLE_STATE);
  }

  /**
   * Used for Tests. TODO:: Re-factor tests and remove this method
   */
  public ObjectIDSet collect(Filter traverser, Collection roots, ObjectIDSet managedObjectIds, LifeCycleState lstate) {
    MarkAndSweepGCAlgorithm gcAlgo = new MarkAndSweepGCAlgorithm(this,
                                                                 new FullGCHook(this, objectManager, stateManager),
                                                                 gcPublisher, gcState, gcIterationCounter
                                                                     .incrementAndGet());
    return gcAlgo.collect(traverser, roots, managedObjectIds, lstate);
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

  public synchronized boolean requestGCStart() {
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

  private static abstract class AbstractGCHook implements GCHook {
    protected final MarkAndSweepGarbageCollector collector;
    protected final ObjectManager                objectManager;
    protected final ClientStateManager           stateManager;

    protected AbstractGCHook(MarkAndSweepGarbageCollector collector, ObjectManager objectManager,
                             ClientStateManager stateManager) {
      this.collector = collector;
      this.objectManager = objectManager;
      this.stateManager = stateManager;
    }

    public void startMonitoringReferenceChanges() {
      collector.startMonitoringReferenceChanges();
    }

    public void stopMonitoringReferenceChanges() {
      collector.stopMonitoringReferenceChanges();
    }

    public void waitUntilReadyToGC() {
      objectManager.waitUntilReadyToGC();
    }
  }

  private final static class FullGCHook extends AbstractGCHook {

    public FullGCHook(MarkAndSweepGarbageCollector collector, ObjectManager objectManager,
                      ClientStateManager stateManager) {
      super(collector, objectManager, stateManager);
    }

    public ObjectIDSet getGCCandidates() {
      return objectManager.getAllObjectIDs();
    }

    public String getDescription() {
      return "Full";
    }

    public GarbageCollectionInfo getGCInfo(int gcIteration) {
      return new GarbageCollectionInfo(gcIteration, true);
    }

    public Set getRootObjectIDs(ObjectIDSet candidateIDs) {
      return objectManager.getRootIDs();
    }

    public Filter getCollectCycleFilter(Set candidateIDs) {
      return NULL_FILTER;
    }

    public Set getObjectReferencesFrom(ObjectID id) {
      ManagedObject obj = objectManager.getObjectByIDOrNull(id);
      if (obj == null) {
        logger.warn("Looked up a new Object before its initialized, skipping : " + id);
        return Collections.EMPTY_SET;
      }
      Set references = obj.getObjectReferences();
      objectManager.releaseReadOnly(obj);
      return references;
    }

    public Set getRescueIDs() {
      Set rescueIds = new ObjectIDSet();
      stateManager.addAllReferencedIdsTo(rescueIds);
      int stateManagerIds = rescueIds.size();

      collector.addNewReferencesTo(rescueIds);
      int referenceCollectorIds = rescueIds.size() - stateManagerIds;

      logger.debug("rescueIds: " + rescueIds.size() + ", stateManagerIds: " + stateManagerIds
                   + ", additional referenceCollectorIds: " + referenceCollectorIds);

      return rescueIds;
    }
  }

  private final static class YoungGCHook extends AbstractGCHook {

    private final YoungGenChangeCollector youngGenReferenceCollector;

    public YoungGCHook(MarkAndSweepGarbageCollector collector, ObjectManager objectManager,
                       ClientStateManager stateManager, YoungGenChangeCollector youngGenChangeCollector) {
      super(collector, objectManager, stateManager);
      this.youngGenReferenceCollector = youngGenChangeCollector;
    }

    public String getDescription() {
      return "YoungGen";
    }

    public GarbageCollectionInfo getGCInfo(int gcIteration) {
      return new GarbageCollectionInfo(gcIteration, true);
    }

    public ObjectIDSet getGCCandidates() {
      return (ObjectIDSet) youngGenReferenceCollector.addYoungGenCandidateObjectIDsTo(new ObjectIDSet());
    }

    public Set getRootObjectIDs(ObjectIDSet candidateIDs) {
      Set idsInMemory = objectManager.getObjectIDsInCache();
      idsInMemory.removeAll(candidateIDs);
      Set roots = objectManager.getRootIDs();
      Set youngGenRoots = youngGenReferenceCollector.getRememberedSet();
      youngGenRoots.addAll(roots);
      youngGenRoots.addAll(idsInMemory);
      return youngGenRoots;
    }

    public Filter getCollectCycleFilter(Set candidateIDs) {
      return new SelectiveFilter(candidateIDs);
    }

    // TODO::Come back and optimized for young Generation
    public Set getObjectReferencesFrom(ObjectID id) {
      ManagedObject obj = objectManager.getObjectByIDOrNull(id);
      if (obj == null) {
        logger.warn("Looked up a new Object before its initialized, skipping : " + id);
        return Collections.EMPTY_SET;
      }
      Set references = obj.getObjectReferences();
      objectManager.releaseReadOnly(obj);
      return references;
    }

    // TODO::Come back and optimized for young Generation
    public Set getRescueIDs() {
      Set rescueIds = new ObjectIDSet();
      stateManager.addAllReferencedIdsTo(rescueIds);
      int stateManagerIds = rescueIds.size();

      collector.addNewReferencesTo(rescueIds);
      int referenceCollectorIds = rescueIds.size() - stateManagerIds;

      logger.debug("rescueIds: " + rescueIds.size() + ", stateManagerIds: " + stateManagerIds
                   + ", additional referenceCollectorIds: " + referenceCollectorIds);

      return rescueIds;
    }
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
}
