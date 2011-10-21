/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.impl;

import com.tc.async.api.ConfigurationContext;
import com.tc.async.api.Sink;
import com.tc.l2.context.StateChangedEvent;
import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.object.ObjectID;
import com.tc.objectserver.api.GarbageCollectionManager;
import com.tc.objectserver.dgc.api.GarbageCollector.GCType;
import com.tc.objectserver.l1.impl.ClientObjectReferenceSet;
import com.tc.util.ObjectIDSet;

import java.util.SortedSet;

public class GarbageCollectionManagerImpl implements GarbageCollectionManager {
  private final GarbageCollectionManager    activeGCManager;
  private volatile GarbageCollectionManager delegate = new PassiveGarbageCollectionManager();

  public GarbageCollectionManagerImpl(final Sink garbageCollectSink,
                                      final ClientObjectReferenceSet clientObjectReferenceSet) {
    activeGCManager = new ActiveGarbageCollectionManager(garbageCollectSink, clientObjectReferenceSet);
  }

  public void l2StateChanged(StateChangedEvent sce) {
    if (sce.movedToActive()) {
      delegate = activeGCManager;
    }
  }

  public void deleteObjects(SortedSet<ObjectID> objects) {
    delegate.deleteObjects(objects);
  }

  public ObjectIDSet nextObjectsToDelete() {
    return delegate.nextObjectsToDelete();
  }

  public void scheduleInlineGarbageCollectionIfNecessary() {
    delegate.scheduleInlineGarbageCollectionIfNecessary();
  }

  public void scheduleGarbageCollection(GCType type, long delay) {
    delegate.scheduleGarbageCollection(type, delay);
  }

  public void doGarbageCollection(GCType type) {
    delegate.doGarbageCollection(type);
  }

  public void scheduleGarbageCollection(GCType type) {
    delegate.scheduleGarbageCollection(type);
  }

  public void initializeContext(ConfigurationContext context) {
    // the passive version doesn't need initialization
    activeGCManager.initializeContext(context);
  }

  public void scheduleInlineCleanupIfNecessary() {
    delegate.scheduleInlineCleanupIfNecessary();
  }

  private static class PassiveGarbageCollectionManager implements GarbageCollectionManager {
    private static final TCLogger logger = TCLogging.getLogger(PassiveGarbageCollectionManager.class);

    public void deleteObjects(SortedSet<ObjectID> objects) {
      // Passive doesn't do inline dgc.
    }

    public ObjectIDSet nextObjectsToDelete() {
      throw new AssertionError("Inline-dgc should not be running on a passive.");
    }

    public void scheduleInlineGarbageCollectionIfNecessary() {
      throw new AssertionError("Inline-dgc should not be scheduled on a passive.");
    }

    public void scheduleGarbageCollection(GCType type, long delay) {
      logger.info("In passive mode, not scheduling DGC.");
    }

    public void doGarbageCollection(GCType type) {
      logger.info("In passive mode, not running DGC.");
    }

    public void scheduleGarbageCollection(GCType type) {
      logger.info("In passive mode, not scheduling DGC.");
    }

    public void initializeContext(ConfigurationContext context) {
      //
    }

    public void scheduleInlineCleanupIfNecessary() {
      logger.info("In passive mode, not scheduling inline cleanup.");
    }

    public void l2StateChanged(StateChangedEvent sce) {
      // do nothing.
    }
  }
}
