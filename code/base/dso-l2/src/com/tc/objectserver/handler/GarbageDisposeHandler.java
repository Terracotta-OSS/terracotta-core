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
import com.tc.objectserver.persistence.api.ManagedObjectPersistor;
import com.tc.objectserver.persistence.api.PersistenceTransaction;
import com.tc.objectserver.persistence.api.PersistenceTransactionProvider;

import java.util.Iterator;
import java.util.Set;
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
    logger.info("GC DELETE START : " + gcResult);
    long start = System.currentTimeMillis();
    Set garbage = gcResult.getGCedObjectIDs();
    // FIXME::TODO:: Use ObjectIDSet once it implement sorted set or something similar
    SortedSet<ObjectID> sortedGarbage = new TreeSet<ObjectID>(garbage);

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
    logger.info("GC DELETE COMPLETE : " + gcResult + " Removed " + sortedGarbage.size() + " objects in " + elapsed + " ms.");
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
