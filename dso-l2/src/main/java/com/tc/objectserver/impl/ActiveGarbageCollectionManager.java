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
import com.tc.objectserver.api.ObjectManager;
import com.tc.objectserver.context.GarbageCollectContext;
import com.tc.objectserver.context.InlineGCContext;
import com.tc.objectserver.core.api.ServerConfigurationContext;
import com.tc.objectserver.dgc.api.GarbageCollector;
import com.tc.objectserver.dgc.api.GarbageCollector.GCType;
import com.tc.objectserver.tx.ServerTransactionManager;
import com.tc.objectserver.tx.TxnsInSystemCompletionListener;
import com.tc.properties.TCPropertiesConsts;
import com.tc.properties.TCPropertiesImpl;
import com.tc.util.ObjectIDSet;
import com.tc.util.TCCollections;

import java.util.Set;
import java.util.SortedSet;

import static java.util.concurrent.TimeUnit.NANOSECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;

public class ActiveGarbageCollectionManager implements GarbageCollectionManager {
  private static final TCLogger          logger                 = TCLogging.getLogger(GarbageCollectionManager.class);
  private static final long              INLINE_GC_INTERVAL     = SECONDS
                                                                    .toNanos(TCPropertiesImpl
                                                                        .getProperties()
                                                                        .getLong(TCPropertiesConsts.L2_OBJECTMANAGER_DGC_INLINE_INTERVAL_SECONDS,
                                                                            10));
  private static final long              MAX_INLINE_GC_OBJECTS  = TCPropertiesImpl
                                                                    .getProperties()
                                                                    .getLong(TCPropertiesConsts.L2_OBJECTMANAGER_DGC_INLINE_MAX_OBJECTS,
                                                                        10000);
  private static final boolean           INLINE_DGC_ENABLED     = TCPropertiesImpl.getProperties().getBoolean(TCPropertiesConsts.L2_OBJECTMANAGER_DGC_INLINE_ENABLED, true);
  private static final long              DELETE_LOG_INTERVAL    = SECONDS.toNanos(60);
  public static final InlineGCContext    INLINE_GC_CONTEXT      = new InlineGCContext();
  private ObjectIDSet                    objectsToDelete        = new ObjectIDSet();
  private long                           lastInlineGCTime       = System.nanoTime();
  private long                           lastDeleteLogTime      = System.nanoTime();
  private long                           deletedObjectCount     = 0;
  private final Sink                     garbageCollectSink;
  private final boolean                  restartable;

  private ServerTransactionManager       transactionManager;
  private ObjectManager                  objectManager;
  private GarbageCollector               garbageCollector;

  public ActiveGarbageCollectionManager(final Sink garbageCollectSink, final boolean restartable) {
    this.garbageCollectSink = garbageCollectSink;
    this.restartable = restartable;
  }

  @Override
  public void deleteObjects(SortedSet<ObjectID> objects, final Set<ObjectID> checkouts) {
    Set<ObjectID> remaining = objectManager.tryDeleteObjects(objects, checkouts);
    if (!remaining.isEmpty()) {
      synchronized (this) {
        objectsToDelete.addAll(remaining);
        scheduleInlineGarbageCollectionIfNecessary();
      }
    }
  }

  @Override
  public void missingObjectsToDelete(final Set<ObjectID> objects) {
    if (!objects.isEmpty()) {
      logger.warn("Missing " + objects.size() + " objects on inline delete.");
    }
  }

  @Override
  public synchronized ObjectIDSet nextObjectsToDelete() {
    if (objectsToDelete.isEmpty()) { return TCCollections.EMPTY_OBJECT_ID_SET; }
    ObjectIDSet deleteNow = objectsToDelete;
    objectsToDelete = new ObjectIDSet();
    deletedObjectCount += deleteNow.size();
    long deleteInterval = System.nanoTime() - lastDeleteLogTime;
    if (deleteInterval >= DELETE_LOG_INTERVAL) {
      logger.info("Inline DGC removed " + deletedObjectCount + " objects in the last " + NANOSECONDS.toSeconds(deleteInterval) + " seconds.");
      lastDeleteLogTime = System.nanoTime();
      deletedObjectCount = 0;
    }
    return deleteNow;
  }

  @Override
  public synchronized void scheduleInlineGarbageCollectionIfNecessary() {
    if (!objectsToDelete.isEmpty() && System.nanoTime() - lastInlineGCTime > INLINE_GC_INTERVAL
        || objectsToDelete.size() > MAX_INLINE_GC_OBJECTS) {
      if (garbageCollectSink.addLossy(INLINE_GC_CONTEXT)) {
        lastInlineGCTime = System.nanoTime();
      }
    }
  }

  @Override
  public void scheduleGarbageCollection(final GCType type, final long delay) {
    transactionManager.callBackOnResentTxnsInSystemCompletion(new TxnsInSystemCompletionListener() {
      @Override
      public void onCompletion() {
        garbageCollectSink.add(new GarbageCollectContext(type, delay));
      }
    });
  }

  @Override
  public void doGarbageCollection(GCType type) {
    GarbageCollectContext gcc = new GarbageCollectContext(type);
    scheduleGarbageCollection(type);
    gcc.waitForCompletion();
  }

  @Override
  public void scheduleGarbageCollection(final GCType type) {
    transactionManager.callBackOnResentTxnsInSystemCompletion(new TxnsInSystemCompletionListener() {
      @Override
      public void onCompletion() {
        garbageCollectSink.add(new GarbageCollectContext(type));
      }
    });
  }

  @Override
  public void initializeContext(ConfigurationContext context) {
    ServerConfigurationContext scc = (ServerConfigurationContext) context;
    transactionManager = scc.getTransactionManager();
    garbageCollector = scc.getObjectManager().getGarbageCollector();
    objectManager = scc.getObjectManager();
  }

  @Override
  public void scheduleInlineCleanupIfNecessary() {
    if (INLINE_DGC_ENABLED && !garbageCollector.isPeriodicEnabled() && restartable) {
      // This delay is here as a failsafe in case there's some aspect of startup we missed. This can be increased in
      // order to not collide with other stuff in that case.
      final long delay = 1000 * TCPropertiesImpl.getProperties()
          .getLong(TCPropertiesConsts.L2_OBJECTMANAGER_DGC_INLINE_CLEANUP_DELAY_SECONDS, 0);
      scheduleGarbageCollection(GCType.INLINE_CLEANUP_GC, delay);
    }
  }

  @Override
  public void l2StateChanged(StateChangedEvent sce) {
    // Do nothing
  }
}
