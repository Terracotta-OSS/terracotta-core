/* 
 * The contents of this file are subject to the Terracotta Public License Version
 * 2.0 (the "License"); You may not use this file except in compliance with the
 * License. You may obtain a copy of the License at 
 *
 *      http://terracotta.org/legal/terracotta-public-license.
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 * the specific language governing rights and limitations under the License.
 *
 * The Covered Software is Terracotta Platform.
 *
 * The Initial Developer of the Covered Software is 
 *      Terracotta, Inc., a Software AG company
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
import com.tc.objectserver.api.ObjectManager;
import com.tc.objectserver.api.Transaction;
import com.tc.objectserver.context.GarbageCollectContext;
import com.tc.objectserver.context.InlineGCContext;
import com.tc.objectserver.core.api.ServerConfigurationContext;
import com.tc.objectserver.dgc.api.GarbageCollector.GCType;
import com.tc.objectserver.persistence.InlineGCPersistor;
import com.tc.objectserver.persistence.PersistenceTransactionProvider;
import com.tc.objectserver.persistence.Persistor;
import com.tc.objectserver.tx.ServerTransactionManager;
import com.tc.objectserver.tx.TxnsInSystemCompletionListener;
import com.tc.properties.TCPropertiesConsts;
import com.tc.properties.TCPropertiesImpl;

import java.util.Set;
import java.util.SortedSet;
import java.util.concurrent.atomic.AtomicBoolean;

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
  private static final long              DELETE_LOG_INTERVAL    = SECONDS.toNanos(60);
  private static final InlineGCContext   INLINE_GC_CONTEXT      = new InlineGCContext();

  private volatile long                  lastInlineGCTime       = System.nanoTime();
  private long                           lastDeleteLogTime      = System.nanoTime();
  private final AtomicBoolean            startedInlineDGC       = new AtomicBoolean();
  private long                           deletedObjectCount     = 0;
  private final Sink                     garbageCollectSink;
  private final InlineGCPersistor        inlineGCPersistor;
  private final PersistenceTransactionProvider persistenceTransactionProvider;

  private ServerTransactionManager       transactionManager;
  private ObjectManager                  objectManager;
  private StateManager stateManager;

  public ActiveGarbageCollectionManager(final Sink garbageCollectSink, final Persistor persistor) {
    this.garbageCollectSink = garbageCollectSink;
    this.inlineGCPersistor = persistor.getInlineGCPersistor();
    this.persistenceTransactionProvider = persistor.getPersistenceTransactionProvider();
  }

  @Override
  public void deleteObjects(SortedSet<ObjectID> objects, final Set<ObjectID> checkouts) {
    Set<ObjectID> remaining = objectManager.tryDeleteObjects(objects, checkouts);
    if (!remaining.isEmpty()) {
      inlineGCPersistor.addObjectIDs(remaining);
      scheduleInlineGarbageCollectionIfNecessary();
    }
  }

  @Override
  public void inlineCleanup() {
    try {
      Set<ObjectID> toDelete = inlineGCPersistor.allObjectIDs();
      Transaction transaction = persistenceTransactionProvider.newTransaction();
      try {
        Set<ObjectID> missing = objectManager.deleteObjects(toDelete);
        if (!missing.isEmpty()) {
          if (isPassiveUnitialized()) {
            // Retry missing objects if we're still passive uninitialized because they might not yet be
            // synced over.
            toDelete.removeAll(missing);
          } else {
            logger.warn("Tried to delete missing objects " + missing);
          }
        }
        // Object deletion and removing from the inline GC persistor must happen in the same transaction
        // so nothing leaks.
        inlineGCPersistor.removeObjectIDs(toDelete);
      } finally {
        transaction.commit();
      }
      logInlineDGC(toDelete.size());
    } finally {
      startedInlineDGC.set(false);
    }
    // Reschedule in case there are more objects to delete now.
    scheduleInlineGarbageCollectionIfNecessary();
  }

  private void logInlineDGC(int size) {
    deletedObjectCount += size;
    long deleteInterval = System.nanoTime() - lastDeleteLogTime;
    if (deleteInterval >= DELETE_LOG_INTERVAL) {
      logger.info("Inline DGC removed " + deletedObjectCount + " objects in the last " + NANOSECONDS.toSeconds(deleteInterval) + " seconds.");
      lastDeleteLogTime = System.nanoTime();
      deletedObjectCount = 0;
    }
  }

  @Override
  public void scheduleInlineGarbageCollectionIfNecessary() {
    if (inlineGCPersistor.size() > 0 && System.nanoTime() - lastInlineGCTime > INLINE_GC_INTERVAL
        || inlineGCPersistor.size() > MAX_INLINE_GC_OBJECTS) {
      if (startedInlineDGC.compareAndSet(false, true)) {
        garbageCollectSink.add(INLINE_GC_CONTEXT);
        lastInlineGCTime = System.nanoTime();
      }
    }
  }

  @Override
  public void scheduleGarbageCollection(final GCType type, final long delay) {
    if (!stateManager.isActiveCoordinator()) {
      logger.info("Not in active mode, not scheduling DGC.");
      return;
    }
    transactionManager.callBackOnResentTxnsInSystemCompletion(new TxnsInSystemCompletionListener() {
      @Override
      public void onCompletion() {
        garbageCollectSink.add(new GarbageCollectContext(type, delay));
      }
    });
  }

  @Override
  public void doGarbageCollection(GCType type) {
    if (!stateManager.isActiveCoordinator()) {
      logger.info("Not in active mode, not scheduling DGC.");
      return;
    }
    GarbageCollectContext gcc = new GarbageCollectContext(type);
    scheduleGarbageCollection(type);
    gcc.waitForCompletion();
  }

  @Override
  public void scheduleGarbageCollection(final GCType type) {
    if (!stateManager.isActiveCoordinator()) {
      logger.info("Not in active mode, not scheduling DGC.");
      return;
    }
    transactionManager.callBackOnResentTxnsInSystemCompletion(new TxnsInSystemCompletionListener() {
      @Override
      public void onCompletion() {
        garbageCollectSink.add(new GarbageCollectContext(type));
      }
    });
  }

  private boolean isPassiveUnitialized() {
    return stateManager.getCurrentState().equals(StateManager.PASSIVE_UNINITIALIZED);
  }
  @Override
  public void initializeContext(ConfigurationContext context) {
    ServerConfigurationContext scc = (ServerConfigurationContext) context;
    transactionManager = scc.getTransactionManager();
    objectManager = scc.getObjectManager();
    stateManager = scc.getL2Coordinator().getStateManager();
  }

  @Override
  public void l2StateChanged(StateChangedEvent sce) {
    // Schedule a cleanup on becoming active or becoming passive standby
    // The only case that really matters for becoming active is if we're
    // becoming active after recovering from disk. It doesn't really hurt
    // to do an extra cleanup here if going from passive->active though
    // since in that case there will probably be nothing to do.
    // On becoming passive standby we do a cleanup because stuff could be missed
    // since it's not yet synced over.
    if (StateManager.ACTIVE_COORDINATOR.equals(sce.getCurrentState()) ||
        StateManager.PASSIVE_STANDBY.equals(sce.getCurrentState())) {
      logger.info("Doing an inline DGC cleanup.");
      scheduleInlineGarbageCollectionIfNecessary();
    }
  }
}
