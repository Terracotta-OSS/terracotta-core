/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.impl;

import static java.util.concurrent.TimeUnit.SECONDS;

import com.tc.async.api.ConfigurationContext;
import com.tc.async.api.Sink;
import com.tc.l2.context.StateChangedEvent;
import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.object.ObjectID;
import com.tc.objectserver.api.GarbageCollectionManager;
import com.tc.objectserver.context.GarbageCollectContext;
import com.tc.objectserver.context.InlineGCContext;
import com.tc.objectserver.core.api.ServerConfigurationContext;
import com.tc.objectserver.dgc.api.GarbageCollector;
import com.tc.objectserver.dgc.api.GarbageCollector.GCType;
import com.tc.objectserver.l1.impl.ClientObjectReferenceSet;
import com.tc.objectserver.l1.impl.ClientObjectReferenceSetChangedListener;
import com.tc.objectserver.tx.ServerTransactionManager;
import com.tc.objectserver.tx.TxnsInSystemCompletionListener;
import com.tc.properties.TCPropertiesConsts;
import com.tc.properties.TCPropertiesImpl;
import com.tc.util.ObjectIDSet;
import com.tc.util.TCCollections;

import java.util.Iterator;
import java.util.SortedSet;

public class ActiveGarbageCollectionManager implements GarbageCollectionManager {
  private static final TCLogger          logger                 = TCLogging.getLogger(GarbageCollectionManager.class);
  private static final int               OBJECT_RETRY_THRESHOLD = 100000;
  private static final long              INLINE_GC_INTERVAL     = SECONDS
                                                                    .toNanos(TCPropertiesImpl
                                                                        .getProperties()
                                                                        .getLong(TCPropertiesConsts.L2_OBJECTMANAGER_DGC_INLINE_INTERVAL_SECONDS,
                                                                                 10));
  private static final long              MAX_INLINE_GC_OBJECTS  = TCPropertiesImpl
                                                                    .getProperties()
                                                                    .getLong(TCPropertiesConsts.L2_OBJECTMANAGER_DGC_INLINE_MAX_OBJECTS,
                                                                             10000);
  public static final InlineGCContext    INLINE_GC_CONTEXT      = new InlineGCContext();
  private ObjectIDSet                    objectsToDelete        = new ObjectIDSet();
  private ObjectIDSet                    objectsToRetry         = new ObjectIDSet();
  private long                           lastInlineGCTime       = System.nanoTime();
  private final Sink                     garbageCollectSink;
  private final ClientObjectReferenceSet clientObjectReferenceSet;

  private ServerTransactionManager       transactionManager;
  private GarbageCollector               garbageCollector;

  public ActiveGarbageCollectionManager(final Sink garbageCollectSink,
                                        final ClientObjectReferenceSet clientObjectReferenceSet) {
    this.garbageCollectSink = garbageCollectSink;
    this.clientObjectReferenceSet = clientObjectReferenceSet;
    clientObjectReferenceSet.addReferenceSetChangeListener(new ClientObjectReferenceSetChangedListener() {
      public void notifyReferenceSetChanged() {
        retryDeletingReferencedObjects();
      }
    });
  }

  public void deleteObjects(SortedSet<ObjectID> objects) {
    if (!objects.isEmpty()) {
      synchronized (this) {
        objectsToDelete.addAll(objects);
        scheduleInlineGarbageCollectionIfNecessary();
      }
    }
  }

  public synchronized ObjectIDSet nextObjectsToDelete() {
    if (objectsToDelete.isEmpty()) { return TCCollections.EMPTY_OBJECT_ID_SET; }
    ObjectIDSet deleteNow = objectsToDelete;
    objectsToDelete = new ObjectIDSet();
    Iterator<ObjectID> oidIterator = deleteNow.iterator();
    int objectsRetried = 0;
    while (oidIterator.hasNext()) {
      ObjectID oid = oidIterator.next();
      if (clientObjectReferenceSet.contains(oid)) {
        objectsRetried++;
        objectsToRetry.add(oid);
        oidIterator.remove();
      }
    }
    if (objectsRetried > OBJECT_RETRY_THRESHOLD) {
      logger.warn("Large number of referenced objects requiring retry (" + objectsRetried + ").");
    }
    return deleteNow;
  }

  public synchronized void scheduleInlineGarbageCollectionIfNecessary() {
    if (!objectsToDelete.isEmpty() && System.nanoTime() - lastInlineGCTime > INLINE_GC_INTERVAL
        || objectsToDelete.size() > MAX_INLINE_GC_OBJECTS) {
      if (garbageCollectSink.addLossy(INLINE_GC_CONTEXT)) {
        lastInlineGCTime = System.nanoTime();
      }

    }
  }

  public void scheduleGarbageCollection(final GCType type, final long delay) {
    transactionManager.callBackOnResentTxnsInSystemCompletion(new TxnsInSystemCompletionListener() {
      public void onCompletion() {
        garbageCollectSink.add(new GarbageCollectContext(type, delay));
      }
    });
  }

  public void doGarbageCollection(GCType type) {
    GarbageCollectContext gcc = new GarbageCollectContext(type);
    scheduleGarbageCollection(type);
    gcc.waitForCompletion();
  }

  public void scheduleGarbageCollection(final GCType type) {
    transactionManager.callBackOnResentTxnsInSystemCompletion(new TxnsInSystemCompletionListener() {
      public void onCompletion() {
        garbageCollectSink.add(new GarbageCollectContext(type));
      }
    });
  }

  private synchronized void retryDeletingReferencedObjects() {
    if (!objectsToRetry.isEmpty()) {
      deleteObjects(objectsToRetry);
      objectsToRetry = new ObjectIDSet();
    }
  }

  public void initializeContext(ConfigurationContext context) {
    ServerConfigurationContext scc = (ServerConfigurationContext) context;
    transactionManager = scc.getTransactionManager();
    garbageCollector = scc.getObjectManager().getGarbageCollector();
  }

  public void scheduleInlineCleanupIfNecessary() {
    if (!garbageCollector.isPeriodicEnabled()
        && TCPropertiesImpl.getProperties().getBoolean(TCPropertiesConsts.L2_OBJECTMANAGER_DGC_INLINE_ENABLED, true)) {
      // This delay is here as a failsafe in case there's some aspect of startup we missed. This can be increased in
      // order to not collide with other stuff in that case.
      final long delay = 1000 * TCPropertiesImpl.getProperties()
          .getLong(TCPropertiesConsts.L2_OBJECTMANAGER_DGC_INLINE_CLEANUP_DELAY_SECONDS, 0);
      scheduleGarbageCollection(GCType.INLINE_CLEANUP_GC, delay);
    }
  }

  public void l2StateChanged(StateChangedEvent sce) {
    // Do nothing
  }
}
