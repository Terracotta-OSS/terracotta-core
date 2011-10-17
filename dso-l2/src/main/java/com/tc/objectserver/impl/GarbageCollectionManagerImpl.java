/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.impl;

import com.tc.async.api.Sink;
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

  public void startActiveMode() {
    delegate = activeGCManager;
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

  private static class PassiveGarbageCollectionManager implements GarbageCollectionManager {
    public void startActiveMode() {
      //
    }

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
      throw new AssertionError("DGC should not be scheduled on a passive.");
    }

    public void doGarbageCollection(GCType type) {
      throw new AssertionError("DGC should not be run from a passive.");
    }

    public void scheduleGarbageCollection(GCType type) {
      throw new AssertionError("DGC should not be scheduled from a passive.");
    }
  }
}
