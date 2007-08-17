/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.persistence.impl;

import com.tc.exception.TCRuntimeException;
import com.tc.logging.TCLogger;
import com.tc.memorydatastore.client.MemoryDataStoreClient;
import com.tc.memorydatastore.message.TCByteArrayKeyValuePair;
import com.tc.object.ObjectID;
import com.tc.objectserver.core.api.ManagedObject;
import com.tc.objectserver.core.api.ManagedObjectState;
import com.tc.objectserver.managedobject.MapManagedObjectState;
import com.tc.objectserver.persistence.api.ManagedObjectPersistor;
import com.tc.objectserver.persistence.api.PersistenceTransaction;
import com.tc.text.PrettyPrinter;
import com.tc.util.Assert;
import com.tc.util.Conversion;
import com.tc.util.ObjectIDSet2;
import com.tc.util.SyncObjectIdSet;
import com.tc.util.SyncObjectIdSetImpl;
import com.tc.util.sequence.MutableSequence;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

public final class MemoryStoreManagedObjectPersistor implements ManagedObjectPersistor {
  private final MemoryDataStoreClient          objectDB;
  private final MutableSequence                objectIDSequence;
  private final MemoryDataStoreClient          rootDB;
  private long                                 saveCount;
  private final TCLogger                       logger;
  private final MemoryStoreCollectionsPersistor   collectionsPersistor;

  public MemoryStoreManagedObjectPersistor(TCLogger logger, 
                                        MemoryDataStoreClient objectDB,
                                        MutableSequence objectIDSequence,
                                        MemoryDataStoreClient rootDB, 
                                        MemoryStoreCollectionsPersistor collectionsPersistor) {
    this.logger = logger;
    this.objectDB = objectDB;
    this.objectIDSequence = objectIDSequence;
    this.rootDB = rootDB;
    this.collectionsPersistor = collectionsPersistor;
  }

  public long nextObjectIDBatch(int batchSize) {
    return objectIDSequence.nextBatch(batchSize);
  }

  public void setNextAvailableObjectID(long startID) {
    objectIDSequence.setNext(startID);
  }

  public void addRoot(PersistenceTransaction tx, String name, ObjectID id) {
    validateID(id);
    this.rootDB.put(name.getBytes(), objectIDToData(id));
  }

  public ObjectID loadRootID(String name) {
    if (name == null) throw new AssertionError("Attempt to retrieve a null root name");
    byte[] value = this.rootDB.get(name.getBytes());
    if (value == null) return ObjectID.NULL_ID;
    ObjectID rv = dataToObjectID(value);
    if (rv == null) return ObjectID.NULL_ID;
    return rv;
  }

  public Set loadRoots() {
    Set rv = new HashSet();
    Collection txns = rootDB.getAll();
    for (Iterator i = txns.iterator(); i.hasNext();) {
      TCByteArrayKeyValuePair pair = (TCByteArrayKeyValuePair) i.next();
      rv.add(dataToObjectID(pair.getValue()));
    }
    return rv;
  }

  public SyncObjectIdSet getAllObjectIDs() {
    SyncObjectIdSet rv = new SyncObjectIdSetImpl();
    rv.startPopulating();
    Thread t = new Thread(new ObjectIdReader(rv), "ObjectIdReaderThread");
    t.setDaemon(true);
    t.start();
    return rv;
  }

  public Set loadRootNames() {
    Set rv = new HashSet();
    Collection txns = rootDB.getAll();
    for (Iterator i = txns.iterator(); i.hasNext();) {
      TCByteArrayKeyValuePair pair = (TCByteArrayKeyValuePair) i.next();
      rv.add(pair.getKey().toString());
    }
    return rv;
  }

  public Map loadRootNamesToIDs() {
    Map rv = new HashMap();
    Collection txns = rootDB.getAll();
    for (Iterator i = txns.iterator(); i.hasNext();) {
      TCByteArrayKeyValuePair pair = (TCByteArrayKeyValuePair) i.next();
      rv.put(pair.getKey().toString(), dataToObjectID(pair.getValue()));
    }
    return rv;
  }

  public ManagedObject loadObjectByID(ObjectID id) {
    validateID(id);
    try {
      byte[] value = this.objectDB.get(objectIDToData(id));
      ManagedObject mo = dataToManagedObject(value);
      loadCollection(mo);
      return mo;
    } catch (Exception e) {
      throw new TCRuntimeException(e);
    }
  }

  private void loadCollection(ManagedObject mo) throws IOException, ClassNotFoundException {
    ManagedObjectState state = mo.getManagedObjectState();
    if (state.getType() == ManagedObjectState.MAP_TYPE || state.getType() == ManagedObjectState.PARTIAL_MAP_TYPE) {
      MapManagedObjectState mapState = (MapManagedObjectState) state;
      Assert.assertNull(mapState.getMap());
      mapState.setMap(collectionsPersistor.loadMap(mo.getID()));
    }
  }

  public void saveObject(PersistenceTransaction persistenceTransaction, ManagedObject managedObject) {
    Assert.assertNotNull(managedObject);
    validateID(managedObject.getID());
    try {
      basicSaveObject(managedObject);
    } catch (IOException e) {
      throw new TCRuntimeException(e);
    } 
  }

  private boolean basicSaveObject(ManagedObject managedObject) throws IOException {
    if (!managedObject.isDirty()) return true;
    this.objectDB.put(objectIDToData(managedObject.getID()), managedObjectToData(managedObject));
    basicSaveCollection(managedObject);
    managedObject.setIsDirty(false);
    saveCount++;
    if (saveCount == 1 || saveCount % (100 * 1000) == 0) {
      logger.debug("saveCount: " + saveCount);
    }
    return true;
  }

  private void basicSaveCollection(ManagedObject managedObject) throws IOException {
    ManagedObjectState state = managedObject.getManagedObjectState();
    if (state.getType() == ManagedObjectState.MAP_TYPE || state.getType() == ManagedObjectState.PARTIAL_MAP_TYPE) {
      MapManagedObjectState mapState = (MapManagedObjectState) state;
      MemoryStorePersistableMap map = (MemoryStorePersistableMap) mapState.getMap();
      collectionsPersistor.saveMap(map);
    }
  }

  public void saveAllObjects(PersistenceTransaction persistenceTransaction, Collection managedObjects) {
    long t0 = System.currentTimeMillis();
    if (managedObjects.isEmpty()) return;
    Object failureContext = null;
    try {
      for (Iterator i = managedObjects.iterator(); i.hasNext();) {
        final ManagedObject managedObject = (ManagedObject) i.next();

        final boolean status = basicSaveObject(managedObject);

        if (!status) {
          failureContext = new Object() {
            public String toString() {
              return "Unable to save ManagedObject: " + managedObject + "; status: " + status;
            }
          };
          break;
        }
      }
    } catch (IOException e) {
      throw new TCRuntimeException(e);
    }

    if (failureContext != null) throw new TCRuntimeException(failureContext.toString());

    long delta = System.currentTimeMillis() - t0;
    saveAllElapsed += delta;
    saveAllCount++;
    saveAllObjectCount += managedObjects.size();
    if (saveAllCount % (100 * 1000) == 0) {
      double avg = ((double) saveAllObjectCount / (double) saveAllElapsed) * 1000;
      logger.debug("save time: " + delta + ", " + managedObjects.size() + " objects; avg: " + avg + "/sec");
    }
  }

  private long saveAllCount       = 0;
  private long saveAllObjectCount = 0;
  private long saveAllElapsed     = 0;

  private void deleteObjectByID(PersistenceTransaction tx, ObjectID id) {
    validateID(id);
    byte[] key = objectIDToData(id);
    if (objectDB.get(key) != null) {
      objectDB.remove(objectIDToData(id));
    } else {
      collectionsPersistor.deleteCollection(id);
    }
  }

  public void deleteAllObjectsByID(PersistenceTransaction tx, Collection objectIDs) {
    for (Iterator i = objectIDs.iterator(); i.hasNext();) {
      deleteObjectByID(tx, (ObjectID) i.next());
    }
  }

  /*********************************************************************************************************************
   * Private stuff
   */

  private void validateID(ObjectID id) {
    Assert.assertNotNull(id);
    Assert.eval(!ObjectID.NULL_ID.equals(id));
  }

  private byte[] objectIDToData(ObjectID objectID) {
    return (Conversion.long2Bytes(objectID.toLong()));
  }


  private byte[] managedObjectToData(ManagedObject mo) throws IOException {
    ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
    new ObjectOutputStream(byteStream).writeObject(mo);
    return (byteStream.toByteArray());
  }

  private ObjectID dataToObjectID(byte[] entry) {
    return new ObjectID(Conversion.bytes2Long(entry));
  }

  private ManagedObject dataToManagedObject(byte[] value) throws IOException, ClassNotFoundException {
    ByteArrayInputStream byteStream = new ByteArrayInputStream(value);
    ObjectInputStream objStream = new ObjectInputStream(byteStream);
    return ((ManagedObject) objStream.readObject());
  }

  public void prettyPrint(PrettyPrinter out) {
    out.println(this.getClass().getName());
    out = out.duplicateAndIndent();
    out.println("db: " + objectDB);
  }

  class ObjectIdReader implements Runnable {
    private final SyncObjectIdSet set;

    public ObjectIdReader(SyncObjectIdSet set) {
      this.set = set;
    }

    public void run() {
      ObjectIDSet2 tmp = new ObjectIDSet2(objectDB.getAll());
      set.stopPopulating(tmp);
    }

   }
}
