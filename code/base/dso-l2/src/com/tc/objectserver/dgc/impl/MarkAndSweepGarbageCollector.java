/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.dgc.impl;

import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.object.ObjectID;
import com.tc.objectserver.context.GCResultContext;
import com.tc.objectserver.core.api.Filter;
import com.tc.objectserver.core.api.ManagedObject;
import com.tc.objectserver.dgc.api.GarbageCollector;
import com.tc.objectserver.dgc.api.GarbageCollectorEventListener;
import com.tc.objectserver.impl.ObjectManagerConfig;
import com.tc.objectserver.managedobject.ManagedObjectChangeListener;
import com.tc.text.PrettyPrinter;
import com.tc.util.Assert;
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

  static final TCLogger                            logger                      = TCLogging
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

                                                                                 public void startMonitoringChanges() {
                                                                                   return;
                                                                                 }

                                                                                 public void stopMonitoringChanges() {
                                                                                   return;
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
  private final GarbageCollectionInfoPublisherImpl gcPublisher                 = new GarbageCollectionInfoPublisherImpl();
  private final ObjectManagerConfig                objectManagerConfig;

  private State                                    state                       = GC_SLEEP;
  private volatile ChangeCollector                 referenceCollector          = NULL_CHANGE_COLLECTOR;
  private volatile YoungGenChangeCollector         youngGenReferenceCollector  = NULL_YOUNG_CHANGE_COLLECTOR;
  private volatile LifeCycleState                  gcState                     = new NullLifeCycleState();
  private volatile boolean                         started                     = false;
  private GCHook                                   gcHook;

  public MarkAndSweepGarbageCollector(ObjectManagerConfig objectManagerConfig) {
    this.objectManagerConfig = objectManagerConfig;
    addListener(new GCLoggerEventPublisher(logger, objectManagerConfig.verboseGC()));
  }

  public void doGC(GCHook hook) {
    this.gcHook = hook;
    MarkAndSweepGCAlgorithm gcAlgo = new MarkAndSweepGCAlgorithm(this, hook, this.gcPublisher, this.gcState,
                                                                 this.gcIterationCounter.incrementAndGet());
    gcAlgo.doGC();
  }

  public boolean deleteGarbage(GCResultContext gcResult) {
    if (requestGCDeleteStart()) {
      this.youngGenReferenceCollector.removeGarbage(gcResult.getGCedObjectIDs());
      this.gcHook.notifyGCComplete(gcResult);
      notifyGCComplete();
      return true;
    }
    return false;
  }

  public void startMonitoringReferenceChanges() {
    this.referenceCollector = new NewReferenceCollector();
    this.youngGenReferenceCollector.startMonitoringChanges();
  }

  public void stopMonitoringReferenceChanges() {
    this.referenceCollector = NULL_CHANGE_COLLECTOR;
    this.youngGenReferenceCollector.stopMonitoringChanges();
  }

  public void changed(ObjectID changedObject, ObjectID oldReference, ObjectID newReference) {
    this.referenceCollector.changed(changedObject, oldReference, newReference);
  }

  public void notifyObjectCreated(ObjectID id) {
    this.youngGenReferenceCollector.notifyObjectCreated(id);
  }

  public void notifyNewObjectInitalized(ObjectID id) {
    this.youngGenReferenceCollector.notifyObjectInitalized(id);
  }

  public void notifyObjectsEvicted(Collection evicted) {
    this.youngGenReferenceCollector.notifyObjectsEvicted(evicted);
  }

  public void addNewReferencesTo(Set rescueIds) {
    this.referenceCollector.addNewReferencesTo(rescueIds);
  }

  /**
   * Used for Tests.
   */
  ObjectIDSet collect(GCHook hook, Filter filter, Collection rootIds, ObjectIDSet managedObjectIds) {
    return collect(hook, filter, rootIds, managedObjectIds, NULL_LIFECYCLE_STATE);
  }

  /**
   * Used for Tests.
   */
  ObjectIDSet collect(GCHook hook, Filter traverser, Collection roots, ObjectIDSet managedObjectIds,
                      LifeCycleState lstate) {
    MarkAndSweepGCAlgorithm gcAlgo = new MarkAndSweepGCAlgorithm(this, hook, this.gcPublisher, this.gcState,
                                                                 this.gcIterationCounter.incrementAndGet());
    return gcAlgo.collect(traverser, roots, managedObjectIds, lstate);
  }

  public void start() {
    if (this.objectManagerConfig.isYoungGenDGCEnabled()) {
      this.youngGenReferenceCollector = new YoungGenChangeCollectorImpl();
    }
    this.started = true;
    this.gcState.start();
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
    this.gcPublisher.addListener(listener);
  }

  public synchronized boolean requestGCStart() {
    if (this.started && this.state == GC_SLEEP) {
      this.state = GC_RUNNING;
      return true;
    }
    // Can't start GC
    return false;
  }

  public synchronized void enableGC() {
    if (GC_DISABLED == this.state) {
      this.state = GC_SLEEP;
    } else {
      logger.warn("GC is already enabled : " + this.state);
    }
  }

  public synchronized boolean disableGC() {
    if (GC_SLEEP == this.state) {
      this.state = GC_DISABLED;
      return true;
    }
    // GC is already running, can't be disabled
    return false;
  }

  public synchronized void notifyReadyToGC() {
    if (this.state == GC_PAUSING) {
      this.state = GC_PAUSED;
    }
  }

  public synchronized void notifyGCComplete() {
    this.state = GC_SLEEP;
  }

  /**
   * In Active server, state transitions from GC_PAUSED to GC_DELETE and in the passive server, state transitions from
   * GC_SLEEP to GC_DELETE.
   */
  private synchronized boolean requestGCDeleteStart() {
    if (this.state == GC_SLEEP || this.state == GC_PAUSED) {
      this.state = GC_DELETE;
      return true;
    }
    return false;
  }

  public synchronized void requestGCPause() {
    this.state = GC_PAUSING;
  }

  public synchronized boolean isPausingOrPaused() {
    return GC_PAUSED == this.state || GC_PAUSING == this.state;
  }

  public synchronized boolean isPaused() {
    return this.state == GC_PAUSED;
  }

  public synchronized boolean isDisabled() {
    return GC_DISABLED == this.state;
  }

  public synchronized PrettyPrinter prettyPrint(PrettyPrinter out) {
    return out.print(getClass().getName()).print("[").print(this.state).print("]");
  }

  public ChangeCollector getChangeCollector() {
    return this.referenceCollector;
  }

  public YoungGenChangeCollector getYoungGenChangeCollector() {
    return this.youngGenReferenceCollector;
  }

  private static class NewReferenceCollector implements ChangeCollector {

    Set newReferences = new ObjectIDSet();

    public void changed(ObjectID changedObject, ObjectID oldReference, ObjectID newReference) {
      synchronized (this.newReferences) {
        this.newReferences.add(newReference);
      }
    }

    public Set addNewReferencesTo(Set set) {
      long start = System.currentTimeMillis();
      synchronized (this.newReferences) {
        set.addAll(this.newReferences);
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

    /* Used in the youngGenObjectIDs Map */
    private static final State UNINITALIZED         = new State("UNINITIALIZED");
    private static final State INITALIZED           = new State("INITIALIZED");

    /* Used for the object state */
    private static final State MONITOR_CHANGES      = new State("MONITOR-CHANGES");
    private static final State DONT_MONITOR_CHANGES = new State("DONT-MONITOR-CHANGES");

    private final Map          youngGenObjectIDs    = new HashMap();
    private final Set          rememberedSet        = new ObjectIDSet();

    private State              state                = DONT_MONITOR_CHANGES;

    public synchronized Set addYoungGenCandidateObjectIDsTo(Set set) {
      for (Iterator i = this.youngGenObjectIDs.entrySet().iterator(); i.hasNext();) {
        Entry e = (Entry) i.next();
        if (e.getValue() == INITALIZED) {
          set.add(e.getKey());
        }
      }
      return set;
    }

    public synchronized Set getRememberedSet() {
      return new ObjectIDSet(this.rememberedSet);
    }

    public synchronized void notifyObjectCreated(ObjectID id) {
      Object oldState = this.youngGenObjectIDs.put(id, UNINITALIZED);
      if (oldState != null) { throw new AssertionError(id + " is already present in " + oldState); }
    }

    public synchronized void notifyObjectInitalized(ObjectID id) {
      Object oldState = this.youngGenObjectIDs.put(id, INITALIZED);
      if (oldState != UNINITALIZED) { throw new AssertionError(id + " is not in " + UNINITALIZED + " but in "
                                                               + oldState); }
    }

    public synchronized void notifyObjectsEvicted(Collection evicted) {
      for (Iterator i = evicted.iterator(); i.hasNext();) {
        ManagedObject mo = (ManagedObject) i.next();
        ObjectID id = mo.getID();
        removeReferencesTo(id);
        Set references = mo.getObjectReferences();
        references.retainAll(this.youngGenObjectIDs.keySet());
        this.rememberedSet.addAll(references);
      }
    }

    private void removeReferencesTo(ObjectID id) {
      this.youngGenObjectIDs.remove(id);
      if (this.state == DONT_MONITOR_CHANGES) {
        /**
         * XXX:: We don't want to remove inward reference to Young Gen Objects that are just faulted out of cache
         * (becoming OldGen object) while the DGC is running. If we did it will lead to GCing valid reachable objects
         * since YoungGen only looks us the objects in memory.
         * <p>
         * This seems counter-intuitive to not remove inward pointers when in MONITOR_CHANGES state, but if you think of
         * removing inward references as forgetting the fact that a reference existed, then not removing the reference
         * is Monitoring the changes.
         */
        this.rememberedSet.remove(id);
      }
    }

    public synchronized void removeGarbage(SortedSet ids) {
      for (Iterator i = ids.iterator(); i.hasNext();) {
        ObjectID oid = (ObjectID) i.next();
        removeReferencesTo(oid);
      }
    }

    public synchronized void startMonitoringChanges() {
      Assert.assertTrue(this.state == DONT_MONITOR_CHANGES);
      this.state = MONITOR_CHANGES;
    }

    public synchronized void stopMonitoringChanges() {
      Assert.assertTrue(this.state == MONITOR_CHANGES);
      this.state = DONT_MONITOR_CHANGES;
      // reset remembered set to the latest set of Young Gen IDs.
      this.rememberedSet.retainAll(this.youngGenObjectIDs.keySet());
    }

  }

  private interface ChangeCollector extends ManagedObjectChangeListener {
    public Set addNewReferencesTo(Set set);
  }

}
