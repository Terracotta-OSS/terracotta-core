/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.impl;

import com.tc.async.api.ConfigurationContext;
import com.tc.async.api.Sink;
import com.tc.l2.context.StateChangedEvent;
import com.tc.l2.state.StateManager;
import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.object.ObjectID;
import com.tc.objectserver.api.GarbageCollectionManager;
import com.tc.objectserver.dgc.api.GarbageCollector.GCType;
import com.tc.util.ObjectIDSet;

import java.util.Collections;
import java.util.Set;
import java.util.SortedSet;

public class GarbageCollectionManagerImpl implements GarbageCollectionManager {
  private final GarbageCollectionManager    activeGCManager;
  private volatile GarbageCollectionManager delegate = new PassiveGarbageCollectionManager();

  public GarbageCollectionManagerImpl(final Sink garbageCollectSink, final boolean restartable) {
    activeGCManager = new ActiveGarbageCollectionManager(garbageCollectSink, restartable);
  }

  @Override
  public void l2StateChanged(StateChangedEvent sce) {
    if (sce.movedToActive()) {
      delegate = activeGCManager;
      if (StateManager.START_STATE.equals(sce.getOldState())) {
        scheduleInlineCleanupIfNecessary();
      }
    } else {
      delegate.l2StateChanged(sce);
    }
  }

  @Override
  public void missingObjectsToDelete(final Set<ObjectID> objects) {
    delegate.missingObjectsToDelete(objects);
  }

  @Override
  public void deleteObjects(SortedSet<ObjectID> objects, final Set<ObjectID> checkouts) {
    delegate.deleteObjects(objects, checkouts);
  }

  @Override
  public ObjectIDSet nextObjectsToDelete() {
    return delegate.nextObjectsToDelete();
  }

  @Override
  public void scheduleInlineGarbageCollectionIfNecessary() {
    delegate.scheduleInlineGarbageCollectionIfNecessary();
  }

  @Override
  public void scheduleGarbageCollection(GCType type, long delay) {
    delegate.scheduleGarbageCollection(type, delay);
  }

  @Override
  public void doGarbageCollection(GCType type) {
    delegate.doGarbageCollection(type);
  }

  @Override
  public void scheduleGarbageCollection(GCType type) {
    delegate.scheduleGarbageCollection(type);
  }

  @Override
  public void initializeContext(ConfigurationContext context) {
    // the passive version doesn't need initialization
    activeGCManager.initializeContext(context);
  }

  @Override
  public void scheduleInlineCleanupIfNecessary() {
    delegate.scheduleInlineCleanupIfNecessary();
  }

  private class PassiveGarbageCollectionManager implements GarbageCollectionManager {
    private volatile boolean acceptMissing = true;
    private final ObjectIDSet missingObjects = new ObjectIDSet();
    private final TCLogger logger = TCLogging.getLogger(PassiveGarbageCollectionManager.class);

    @Override
    public void deleteObjects(SortedSet<ObjectID> objects, final Set<ObjectID> checkouts) {
      activeGCManager.deleteObjects(objects, checkouts);
    }

    @Override
    public void missingObjectsToDelete(final Set<ObjectID> objects) {
      if (acceptMissing) {
        missingObjects.addAll(objects);
      } else {
        activeGCManager.missingObjectsToDelete(objects);
      }
    }

    @Override
    public ObjectIDSet nextObjectsToDelete() {
      return activeGCManager.nextObjectsToDelete();
    }

    @Override
    public void scheduleInlineGarbageCollectionIfNecessary() {
      activeGCManager.scheduleInlineGarbageCollectionIfNecessary();
    }

    @Override
    public void scheduleGarbageCollection(GCType type, long delay) {
      logger.info("In passive mode, not scheduling DGC.");
    }

    @Override
    public void doGarbageCollection(GCType type) {
      logger.info("In passive mode, not running DGC.");
    }

    @Override
    public void scheduleGarbageCollection(GCType type) {
      logger.info("In passive mode, not scheduling DGC.");
    }

    @Override
    public void initializeContext(ConfigurationContext context) {
      //
    }

    @Override
    public void scheduleInlineCleanupIfNecessary() {
      logger.info("In passive mode, not scheduling inline cleanup.");
    }

    @Override
    public void l2StateChanged(StateChangedEvent sce) {
      if (StateManager.PASSIVE_STANDBY.equals(sce.getCurrentState())) {
        acceptMissing = false;
        deleteObjects(missingObjects, Collections.<ObjectID>emptySet());
        missingObjects.clear();
      }
    }
  }
}
