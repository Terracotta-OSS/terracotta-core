/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.impl;

import com.tc.async.api.Sink;
import com.tc.object.ObjectID;
import com.tc.objectserver.api.GarbageCollectionManager;
import com.tc.objectserver.context.GarbageCollectContext;
import com.tc.objectserver.context.InlineGCContext;
import com.tc.objectserver.dgc.api.GarbageCollector.GCType;
import com.tc.util.ObjectIDSet;

import java.util.SortedSet;

public class GarbageCollectionManagerImpl implements GarbageCollectionManager {
  private static final InlineGCContext INLINE_GC_CONTEXT = new InlineGCContext();
  private SortedSet<ObjectID>          objectsToDelete   = new ObjectIDSet();
  private final Sink                   garbageCollectSink;

  public GarbageCollectionManagerImpl(final Sink garbageCollectSink) {
    this.garbageCollectSink = garbageCollectSink;
  }

  public synchronized void deleteObjects(SortedSet<ObjectID> objects) {
    if (!objects.isEmpty()) {
      objectsToDelete.addAll(objects);
      scheduleInlineGarbageCollectionIfNecessary();
    }
  }

  public synchronized SortedSet<ObjectID> nextObjectsToDelete() {
    SortedSet<ObjectID> temp = objectsToDelete;
    objectsToDelete = new ObjectIDSet();
    return temp;
  }

  public synchronized void scheduleInlineGarbageCollectionIfNecessary() {
    if (!objectsToDelete.isEmpty()) {
      garbageCollectSink.addLossy(INLINE_GC_CONTEXT);
    }
  }

  public void scheduleGarbageCollection(GCType type, long delay) {
    garbageCollectSink.add(new GarbageCollectContext(type, delay));
  }

  public void doGarbageCollection(GCType type) {
    GarbageCollectContext gcc = new GarbageCollectContext(type);
    garbageCollectSink.add(gcc);
    gcc.waitForCompletion();
  }

  public void scheduleGarbageCollection(GCType type) {
    garbageCollectSink.add(new GarbageCollectContext(type));
  }

}
