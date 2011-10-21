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

import java.util.SortedSet;

public class TestGarbageCollectionManager implements GarbageCollectionManager {

  public void deleteObjects(SortedSet<ObjectID> objects) {
    // do nothing
  }

  public ObjectIDSet nextObjectsToDelete() {
    return TCCollections.EMPTY_OBJECT_ID_SET;
  }

  public void scheduleInlineGarbageCollectionIfNecessary() {
    // do nothing
  }

  public void scheduleGarbageCollection(GCType type, long delay) {
    // do nothing
  }

  public void doGarbageCollection(GCType type) {
    // do nothing
  }

  public void scheduleGarbageCollection(GCType type) {
    // do nothing
  }

  public void initializeContext(ConfigurationContext context) {
    // do nothing
  }

  public void scheduleInlineCleanupIfNecessary() {
    // do nothing
  }

  public void l2StateChanged(StateChangedEvent sce) {
    // do nothing
  }
}
