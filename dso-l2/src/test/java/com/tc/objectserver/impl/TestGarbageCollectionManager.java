/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.impl;

import com.tc.object.ObjectID;
import com.tc.objectserver.api.GarbageCollectionManager;
import com.tc.objectserver.dgc.api.GarbageCollector.GCType;
import com.tc.util.TCCollections;

import java.util.SortedSet;

public class TestGarbageCollectionManager implements GarbageCollectionManager {

  public void deleteObjects(SortedSet<ObjectID> objects) {
    // do nothing
  }

  public SortedSet<ObjectID> nextObjectsToDelete() {
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
}
