/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.persistence.sleepycat;

import com.sleepycat.je.Cursor;
import com.sleepycat.je.CursorConfig;
import com.sleepycat.je.Database;
import com.sleepycat.je.DatabaseEntry;
import com.sleepycat.je.LockMode;
import com.sleepycat.je.OperationStatus;
import com.tc.io.serializer.DSOSerializerPolicy;
import com.tc.io.serializer.TCObjectInputStream;
import com.tc.io.serializer.TCObjectOutputStream;
import com.tc.io.serializer.api.BasicSerializer;
import com.tc.logging.TCLogger;
import com.tc.object.ObjectID;
import com.tc.objectserver.core.api.ManagedObjectState;
import com.tc.objectserver.managedobject.PersistableObjectState;
import com.tc.objectserver.persistence.api.PersistenceTransaction;
import com.tc.objectserver.persistence.api.PersistenceTransactionProvider;
import com.tc.objectserver.persistence.api.PersistentCollectionsUtil;
import com.tc.objectserver.persistence.sleepycat.SleepycatPersistor.SleepycatPersistorBase;
import com.tc.properties.TCPropertiesConsts;
import com.tc.properties.TCPropertiesImpl;
import com.tc.util.Assert;
import com.tc.util.Conversion;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInput;
import java.util.SortedSet;

public class SleepycatCollectionsPersistor extends SleepycatPersistorBase {

  private static final int                 DELETE_BATCH_SIZE = TCPropertiesImpl
                                                                 .getProperties()
                                                                 .getInt(
                                                                         TCPropertiesConsts.L2_OBJECTMANAGER_DELETEBATCHSIZE,
                                                                         5000);
  private final Database                   database;
  private final BasicSerializer            serializer;
  private final ByteArrayOutputStream      bao;
  private final SleepycatCollectionFactory collectionFactory;
  private final TCObjectOutputStream       oo;

  public SleepycatCollectionsPersistor(TCLogger logger, Database mapsDatabase,
                                       SleepycatCollectionFactory sleepycatCollectionFactory) {
    this.database = mapsDatabase;
    this.collectionFactory = sleepycatCollectionFactory;
    DSOSerializerPolicy policy = new DSOSerializerPolicy();
    this.serializer = new BasicSerializer(policy);
    this.bao = new ByteArrayOutputStream(1024);
    this.oo = new TCObjectOutputStream(bao);
  }

  public int saveCollections(PersistenceTransaction tx, ManagedObjectState state) throws IOException,
      TCDatabaseException {
    PersistableObjectState persistabeState = (PersistableObjectState) state;
    PersistableCollection collection = persistabeState.getPersistentCollection();
    return collection.commit(this, tx, database);
  }

  public synchronized byte[] serialize(long id, Object o) throws IOException {
    oo.writeLong(id);
    serializer.serializeTo(o, oo);
    oo.flush();
    byte b[] = bao.toByteArray();
    bao.reset();
    return b;
  }

  public synchronized byte[] serialize(Object o) throws IOException {
    serializer.serializeTo(o, oo);
    oo.flush();
    byte b[] = bao.toByteArray();
    bao.reset();
    return b;
  }

  public void loadCollectionsToManagedState(PersistenceTransaction tx, ObjectID id, ManagedObjectState state)
      throws IOException, ClassNotFoundException, TCDatabaseException {
    Assert.assertTrue(PersistentCollectionsUtil.isPersistableCollectionType(state.getType()));

    PersistableObjectState persistableState = (PersistableObjectState) state;
    Assert.assertNull(persistableState.getPersistentCollection());
    PersistableCollection collection = PersistentCollectionsUtil.createPersistableCollection(id, collectionFactory,
                                                                                             state.getType());
    collection.load(this, tx, database);
    persistableState.setPersistentCollection(collection);
  }

  public Object deserialize(int start, byte[] data) throws IOException, ClassNotFoundException {
    if (start >= data.length) return null;
    ByteArrayInputStream bai = new ByteArrayInputStream(data, start, data.length - start);
    ObjectInput ois = new TCObjectInputStream(bai);
    return serializer.deserializeFrom(ois);
  }

  public Object deserialize(byte[] data) throws IOException, ClassNotFoundException {
    return deserialize(0, data);
  }

  /**
   * This method is slightly dubious in that it assumes that the ObjectID is the first 8 bytes of the Key in the entire
   * collections database.(which is true, but that logic is spread elsewhere)
   * @throws TCDatabaseException 
   * 
   */
  public int deleteAllCollections(PersistenceTransactionProvider ptp, SortedSet<ObjectID> mapIds) throws TCDatabaseException {
    // These are the possible ways for isolation
    // CursorConfig.DEFAULT : Default configuration used if null is passed to methods that create a cursor.
    // CursorConfig.READ_COMMITTED : This ensures the stability of the current data item read by the cursor but permits
    // data read by this cursor to be modified or deleted prior to the commit of the transaction.
    // CursorConfig.READ_UNCOMMITTED : A convenience instance to configure read operations performed by the cursor to
    // return modified but not yet committed data.
    // During our testing we found that READ_UNCOMMITTED does not raise any problem and gives a performance enhancement
    // over READ_COMMITTED. Since we never read the map which has been marked for deletion by the DGC the deadlocks are
    // avoided
    PersistenceTransaction tx = ptp.newTransaction();
    int totalEntriesDeleted = 0;
    int mapEntriesDeleted = 0;

    Cursor c = database.openCursor(pt2nt(tx), CursorConfig.READ_UNCOMMITTED);
    try {
      for (ObjectID id : mapIds) {
        byte idb[] = Conversion.long2Bytes(id.toLong());
        DatabaseEntry key = new DatabaseEntry();
        key.setData(idb);
        DatabaseEntry value = new DatabaseEntry();
        value.setPartial(0, 0, true);
        try {
          if (c.getSearchKeyRange(key, value, LockMode.DEFAULT) == OperationStatus.SUCCESS) {
            do {
              if (partialMatch(idb, key.getData())) {
                c.delete();
                mapEntriesDeleted++;
                totalEntriesDeleted++;
                if (mapEntriesDeleted >= DELETE_BATCH_SIZE) {
                  c.close();
                  tx.commit();
                  mapEntriesDeleted = 0;
                  tx = ptp.newTransaction();
                  c = database.openCursor(pt2nt(tx), CursorConfig.READ_UNCOMMITTED);
                  if (c.getSearchKeyRange(key, value, LockMode.DEFAULT) == OperationStatus.SUCCESS) {
                    if (partialMatch(idb, key.getData())) {
                      c.delete();
                      mapEntriesDeleted++;
                      totalEntriesDeleted++;
                    } else {
                      break;
                    }
                  }
                }
              } else {
                break;
              }
            } while (c.getNext(key, value, LockMode.DEFAULT) == OperationStatus.SUCCESS);
          }
        } catch (Exception t) {
          throw new TCDatabaseException(t.getMessage());
        }
      }
    } catch (Exception e) {
      throw new TCDatabaseException(e.getMessage());
    } finally {
      c.close();
      if (mapEntriesDeleted > 0) {
        tx.commit();
      }
    }
    return totalEntriesDeleted;
  }

  private boolean partialMatch(byte[] idbytes, byte[] key) {
    if (key.length < idbytes.length) return false;
    for (int i = 0; i < idbytes.length; i++) {
      if (idbytes[i] != key[i]) return false;
    }
    return true;
  }

}
