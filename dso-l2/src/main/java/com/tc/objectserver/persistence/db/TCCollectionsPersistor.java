/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.persistence.db;

import com.tc.logging.TCLogger;
import com.tc.object.ObjectID;
import com.tc.objectserver.core.api.ManagedObjectState;
import com.tc.objectserver.managedobject.PersistableObjectState;
import com.tc.objectserver.persistence.api.PersistentCollectionsUtil;
import com.tc.objectserver.persistence.db.DBPersistorImpl.DBPersistorBase;
import com.tc.objectserver.storage.api.PersistenceTransaction;
import com.tc.objectserver.storage.api.PersistenceTransactionProvider;
import com.tc.objectserver.storage.api.TCMapsDatabase;
import com.tc.properties.TCPropertiesConsts;
import com.tc.properties.TCPropertiesImpl;
import com.tc.util.Assert;

import java.io.IOException;
import java.util.SortedSet;

public class TCCollectionsPersistor extends DBPersistorBase {

  private final TCMapsDatabase               database;
  private static final int                   DELETE_BATCH_SIZE = TCPropertiesImpl
                                                                   .getProperties()
                                                                   .getInt(TCPropertiesConsts.L2_OBJECTMANAGER_DELETEBATCHSIZE,
                                                                           5000);

  private final PersistableCollectionFactory collectionFactory;
  private final TCCollectionsSerializer      serializer;

  public TCCollectionsPersistor(final TCLogger logger, final TCMapsDatabase mapsDatabase,
                                final PersistableCollectionFactory sleepycatCollectionFactory,
                                final TCCollectionsSerializer serializer) {
    this.database = mapsDatabase;
    this.collectionFactory = sleepycatCollectionFactory;
    this.serializer = serializer;
  }

  public int saveCollections(final PersistenceTransaction tx, final ManagedObjectState state) throws IOException,
      TCDatabaseException {
    final PersistableObjectState persistabeState = (PersistableObjectState) state;
    final PersistableCollection collection = persistabeState.getPersistentCollection();
    return collection.commit(this.serializer, tx, this.database);
  }

  public void loadCollectionsToManagedState(final PersistenceTransaction tx, final ObjectID id,
                                            final ManagedObjectState state) throws IOException, ClassNotFoundException,
      TCDatabaseException {
    Assert.assertTrue(PersistentCollectionsUtil.isPersistableCollectionType(state.getType()));

    final PersistableObjectState persistableState = (PersistableObjectState) state;
    Assert.assertNull(persistableState.getPersistentCollection());
    final PersistableCollection collection = PersistentCollectionsUtil
        .createPersistableCollection(id, this.collectionFactory, state.getType());
    collection.load(this.serializer, tx, this.database);
    persistableState.setPersistentCollection(collection);
  }

  /**
   * This method is slightly dubious in that it assumes that the ObjectID is the first 8 bytes of the Key in the entire
   * collections database.(which is true, but that logic is spread elsewhere)
   * 
   * @param ptp - PersistenceTransactionProvider
   * @param oids - Object IDs to delete
   * @param extantMapTypeOidSet - a copy of the map OIDs
   * @throws TCDatabaseException
   */
  public long deleteAllCollections(final PersistenceTransactionProvider ptp, final SortedSet<ObjectID> oids,
                                   final SortedSet<ObjectID> extantMapTypeOidSet) throws TCDatabaseException {
    BatchedTransaction batchedTransaction = new BatchedTransactionImpl(ptp, DELETE_BATCH_SIZE);
    batchedTransaction.startBatchedTransaction();
    long changesCount = 0;
    try {
      for (final ObjectID id : oids) {
        if (!extantMapTypeOidSet.contains(id)) {
          // Not a map type
          continue;
        }
        this.database.deleteCollectionBatched(id.toLong(), batchedTransaction);
      }
    } finally {
      changesCount = batchedTransaction.completeBatchedTransaction();
    }
    return changesCount;
  }
}
