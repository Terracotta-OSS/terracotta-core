/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.api;

import com.tc.async.api.PostInit;
import com.tc.l2.state.StateChangeListener;
import com.tc.object.ObjectID;
import com.tc.objectserver.dgc.api.GarbageCollector.GCType;

import java.util.Set;
import java.util.SortedSet;

public interface GarbageCollectionManager extends PostInit, StateChangeListener {

  void deleteObjects(SortedSet<ObjectID> objects, final Set<ObjectID> checkouts);

  void inlineCleanup();

  void scheduleInlineGarbageCollectionIfNecessary();

  /**
   * Schedule a garbage collect to run asynchronously.
   */
  void scheduleGarbageCollection(GCType type, long delay);

  /**
   * Run a garbage collect synchronously.
   */
  void doGarbageCollection(GCType type);

  void scheduleGarbageCollection(GCType type);
}
