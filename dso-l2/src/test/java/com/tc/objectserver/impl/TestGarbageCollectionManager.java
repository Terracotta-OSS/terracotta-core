/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.impl;

import com.tc.async.api.ConfigurationContext;
import com.tc.l2.context.StateChangedEvent;
import com.tc.object.ObjectID;
import com.tc.objectserver.api.GarbageCollectionManager;
import com.tc.objectserver.dgc.api.GarbageCollector.GCType;
import com.tc.util.ObjectIDSet;
import com.tc.util.TCCollections;

import java.util.Set;
import java.util.SortedSet;

public class TestGarbageCollectionManager implements GarbageCollectionManager {

  @Override
  public void deleteObjects(SortedSet<ObjectID> objects, final Set<ObjectID> checkouts) {
    // do nothing
  }

  @Override
  public void missingObjectsToDelete(final Set<ObjectID> objects) {
    // do nothing
  }

  @Override
  public ObjectIDSet nextObjectsToDelete() {
    return TCCollections.EMPTY_OBJECT_ID_SET;
  }

  @Override
  public void scheduleInlineGarbageCollectionIfNecessary() {
    // do nothing
  }

  @Override
  public void scheduleGarbageCollection(GCType type, long delay) {
    // do nothing
  }

  @Override
  public void doGarbageCollection(GCType type) {
    // do nothing
  }

  @Override
  public void scheduleGarbageCollection(GCType type) {
    // do nothing
  }

  @Override
  public void initializeContext(ConfigurationContext context) {
    // do nothing
  }

  @Override
  public void scheduleInlineCleanupIfNecessary() {
    // do nothing
  }

  @Override
  public void l2StateChanged(StateChangedEvent sce) {
    // do nothing
  }
}
