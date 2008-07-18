/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.handler;

import com.tc.async.api.AbstractEventHandler;
import com.tc.async.api.EventContext;
import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.object.ObjectID;
import com.tc.objectserver.context.GCResultContext;
import com.tc.objectserver.core.api.GarbageCollectionInfo;
import com.tc.objectserver.core.api.GarbageCollectionInfoPublisher;
import com.tc.objectserver.persistence.api.ManagedObjectPersistor;
import com.tc.objectserver.persistence.api.PersistenceTransaction;
import com.tc.objectserver.persistence.api.PersistenceTransactionProvider;

import java.util.Iterator;
import java.util.SortedSet;
import java.util.TreeSet;

public class GarbageDisposeHandler extends AbstractEventHandler {

  private final static TCLogger                logger           = TCLogging.getLogger(GarbageDisposeHandler.class);

  private static final long                    REMOVE_THRESHOLD = 300;

  private final ManagedObjectPersistor         managedObjectPersistor;
  private final PersistenceTransactionProvider persistenceTransactionProvider;
  private final int                            deleteBatchSize;

  public GarbageDisposeHandler(ManagedObjectPersistor managedObjectPersistor,
                               PersistenceTransactionProvider persistenceTransactionProvider, int deleteBatchSize) {
    this.managedObjectPersistor = managedObjectPersistor;
    this.persistenceTransactionProvider = persistenceTransactionProvider;
    this.deleteBatchSize = deleteBatchSize;
  }

  public void handleEvent(EventContext context) {
    GCResultContext gcResult = (GCResultContext) context;
    GarbageCollectionInfo gcInfo = gcResult.getGCInfo();
    GarbageCollectionInfoPublisher gcPublisher = gcResult.getGCPublisher();
    
    logger.info("GC Delete Start : " + gcResult);
    
    gcPublisher.fireGCDeleteEvent(gcInfo);
    long start = System.currentTimeMillis();
    SortedSet sortedGarbage = gcResult.getGCedObjectIDs();

    if (sortedGarbage.size() <= deleteBatchSize) {
      removeFromStore(sortedGarbage);
    } else {
      SortedSet<ObjectID> split = new TreeSet<ObjectID>();
      for (Iterator<ObjectID> i = sortedGarbage.iterator(); i.hasNext();) {
        split.add(i.next());
        if (split.size() >= deleteBatchSize) {
          removeFromStore(split);
          split = new TreeSet<ObjectID>();
        }
      }
      if (split.size() > 0) {
        removeFromStore(split);
      }
    }
    long elapsed = System.currentTimeMillis() - start;
    logger.info("GC Delete Complete : " + gcResult + " Removed " + sortedGarbage.size() + " objects in " + elapsed
                + " ms.");
    gcInfo.setDeleteStageTime(elapsed); 
    long endMillis = System.currentTimeMillis();
    gcInfo.setElapsedTime(endMillis - gcInfo.getStartTime());

    gcPublisher.fireGCCompletedEvent(gcInfo);
   
  }

  private void removeFromStore(SortedSet<ObjectID> sortedGarbage) {
    long start = System.currentTimeMillis();

    PersistenceTransaction tx = persistenceTransactionProvider.newTransaction();
    managedObjectPersistor.deleteAllObjectsByID(tx, sortedGarbage);
    tx.commit();

    long elapsed = System.currentTimeMillis() - start;
    if (elapsed > REMOVE_THRESHOLD) {
      logger.info("Removed " + sortedGarbage.size() + " objects in " + elapsed + " ms.");
    }
  }
}
