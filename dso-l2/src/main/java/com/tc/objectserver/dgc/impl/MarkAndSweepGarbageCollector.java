/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.dgc.impl;

import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.object.ObjectID;
import com.tc.objectserver.api.ObjectManager;
import com.tc.objectserver.context.DGCResultContext;
import com.tc.objectserver.core.api.Filter;
import com.tc.objectserver.dgc.api.GarbageCollectionInfo;
import com.tc.objectserver.dgc.api.GarbageCollectionInfoPublisher;
import com.tc.objectserver.dgc.api.GarbageCollectorEventListener;
import com.tc.objectserver.impl.ObjectManagerConfig;
import com.tc.objectserver.l1.api.ClientStateManager;
import com.tc.util.ObjectIDSet;
import com.tc.util.concurrent.LifeCycleState;
import com.tc.util.sequence.DGCSequenceProvider;

import java.util.Collection;
import java.util.Set;

/**
 */
public class MarkAndSweepGarbageCollector extends AbstractGarbageCollector {

  static final TCLogger                        logger                     = TCLogging
                                                                              .getLogger(MarkAndSweepGarbageCollector.class);

  private final GarbageCollectionInfoPublisher gcPublisher;
  private final ClientStateManager             stateManager;
  private final ObjectManager                  objectManager;
  private final DGCSequenceProvider            dgcSequenceProvider;

  private volatile ChangeCollector             referenceCollector         = ChangeCollector.NULL_CHANGE_COLLECTOR;
  protected volatile boolean                   started                    = false;
  protected volatile LifeCycleState            gcState                    = NULL_LIFECYCLE_STATE;

  public MarkAndSweepGarbageCollector(final ObjectManagerConfig objectManagerConfig, final ObjectManager objectMgr,
                                      final ClientStateManager stateManager,
                                      final GarbageCollectionInfoPublisher gcPublisher,
                                      final DGCSequenceProvider dgcSequenceProvider) {
    this.objectManager = objectMgr;
    this.stateManager = stateManager;
    this.gcPublisher = gcPublisher;
    this.dgcSequenceProvider = dgcSequenceProvider;
    addListener(new GCLoggerEventPublisher(new GCLogger(logger, objectManagerConfig.verboseGC())));
  }

  @Override
  public void doGC(final GCType type) {
    GCHook hook = null;
    switch (type) {
      case FULL_GC:
        hook = new FullGCHook(this, this.objectManager, this.stateManager, false);
        break;
      case INLINE_CLEANUP_GC:
        hook = new FullGCHook(this, this.objectManager, this.stateManager, true);
        break;
    }
    final MarkAndSweepGCAlgorithm gcAlgo = new MarkAndSweepGCAlgorithm(this, hook, this.gcPublisher, this.gcState,
                                                                       this.dgcSequenceProvider.getNextId());
    gcAlgo.doGC();
  }

  @Override
  public void deleteGarbage(final DGCResultContext dgcResultContext) {
    objectManager.notifyGCComplete(dgcResultContext);
    notifyGCComplete();

    GarbageCollectionInfo gcInfo = dgcResultContext.getGCInfo();
    gcInfo.setEndObjectCount(objectManager.getLiveObjectCount());
    gcInfo.setActualGarbageCount(dgcResultContext.getGarbageIDs().size());
    final long elapsedTime = System.currentTimeMillis() - gcInfo.getStartTime();
    gcInfo.setElapsedTime(elapsedTime);
    gcPublisher.fireGCCompletedEvent(gcInfo);
  }

  public void startMonitoringReferenceChanges() {
    this.referenceCollector = new NewReferenceCollector();
  }

  public void stopMonitoringReferenceChanges() {
    this.referenceCollector = ChangeCollector.NULL_CHANGE_COLLECTOR;
  }

  @Override
  public void changed(final ObjectID changedObject, final ObjectID oldReference, final ObjectID newReference) {
    this.referenceCollector.changed(changedObject, oldReference, newReference);
  }

  public void addNewReferencesTo(final Set rescueIds) {
    this.referenceCollector.addNewReferencesTo(rescueIds);
  }

  /**
   * Used for Tests.
   */
  ObjectIDSet collect(final GCHook hook, final Filter filter, final Collection rootIds,
                      final ObjectIDSet managedObjectIds) {
    return collect(hook, filter, rootIds, managedObjectIds, NULL_LIFECYCLE_STATE);
  }

  /**
   * Used for Tests.
   */
  ObjectIDSet collect(final GCHook hook, final Filter traverser, final Collection roots,
                      final ObjectIDSet managedObjectIds, final LifeCycleState lstate) {
    final MarkAndSweepGCAlgorithm gcAlgo = new MarkAndSweepGCAlgorithm(this, hook, this.gcPublisher, this.gcState,
                                                                       this.dgcSequenceProvider.getNextId());
    return gcAlgo.collect(traverser, roots, managedObjectIds, lstate);
  }

  @Override
  public void start() {
    this.started = true;
    this.gcState.start();
  }

  @Override
  public void stop() {
    this.started = false;
    int count = 0;
    while (!this.gcState.stopAndWait(5000) && (count < 6)) {
      count++;
      logger.warn("DGC Thread did not stop");
    }
  }

  @Override
  public boolean isStarted() {
    return this.started;
  }

  @Override
  public void setState(final LifeCycleState st) {
    this.gcState = st;
  }

  @Override
  public void addListener(final GarbageCollectorEventListener listener) {
    this.gcPublisher.addListener(listener);
  }
}
