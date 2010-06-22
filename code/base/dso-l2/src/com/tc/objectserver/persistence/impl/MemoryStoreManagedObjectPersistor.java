/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.persistence.impl;

import com.tc.exception.ImplementMe;
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
import com.tc.objectserver.persistence.api.PersistentCollectionsUtil;
import com.tc.text.PrettyPrinter;
import com.tc.util.Assert;
import com.tc.util.Conversion;
import com.tc.util.NullSyncObjectIdSet;
import com.tc.util.ObjectIDSet;
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
import java.util.SortedSet;

/**
 * Looks like this class is not maintained well over the years. It should either be cleaned up or Killed. @see
 * https://jira.terracotta.org/jira/browse/DEV-4347
 */
public final class MemoryStoreManagedObjectPersistor implements ManagedObjectPersistor {
  private final MemoryDataStoreClient           objectDB;
  private final MutableSequence                 objectIDSequence;
  private final MemoryDataStoreClient           rootDB;
  private long                                  saveCount;
  private final TCLogger                        logger;
  private final MemoryStoreCollectionsPersistor collectionsPersistor;
  private final SyncObjectIdSet                 extantObjectIDs;

  public MemoryStoreManagedObjectPersistor(final TCLogger logger, final MemoryDataStoreClient objectDB,
                                           final MutableSequence objectIDSequence, final MemoryDataStoreClient rootDB,
                                           final MemoryStoreCollectionsPersistor collectionsPersistor) {
    this.logger = logger;
    this.objectDB = objectDB;
    this.objectIDSequence = objectIDSequence;
    this.rootDB = rootDB;
    this.collectionsPersistor = collectionsPersistor;

    this.extantObjectIDs = getAllObjectIDs();
  }

  public int getObjectCount() {
    return this.extantObjectIDs.size();
  }

  public boolean addNewObject(final ManagedObject managed) {
    return this.extantObjectIDs.add(managed.getID());
  }

  public boolean containsObject(final ObjectID id) {
    return this.extantObjectIDs.contains(id);
  }

  public void removeAllObjectIDs(final SortedSet<ObjectID> ids) {
    this.extantObjectIDs.removeAll(ids);
  }

  public ObjectIDSet snapshotObjectIDs() {
    return this.extantObjectIDs.snapshot();
  }

  public ObjectIDSet snapshotEvictableObjectIDs() {
    throw new ImplementMe();
  }

  public long nextObjectIDBatch(final int batchSize) {
    return this.objectIDSequence.nextBatch(batchSize);
  }

  public long currentObjectIDValue() {
    return this.objectIDSequence.current();
  }

  public void setNextAvailableObjectID(final long startID) {
    this.objectIDSequence.setNext(startID);
  }

  public void addRoot(final PersistenceTransaction tx, final String name, final ObjectID id) {
    validateID(id);
    this.rootDB.put(name.getBytes(), objectIDToData(id));
  }

  public ObjectID loadRootID(final String name) {
    if (name == null) { throw new AssertionError("Attempt to retrieve a null root name"); }
    final byte[] value = this.rootDB.get(name.getBytes());
    if (value == null) { return ObjectID.NULL_ID; }
    final ObjectID rv = dataToObjectID(value);
    if (rv == null) { return ObjectID.NULL_ID; }
    return rv;
  }

  public Set loadRoots() {
    final Set rv = new HashSet();
    final Collection txns = this.rootDB.getAll();
    for (final Iterator i = txns.iterator(); i.hasNext();) {
      final TCByteArrayKeyValuePair pair = (TCByteArrayKeyValuePair) i.next();
      rv.add(dataToObjectID(pair.getValue()));
    }
    return rv;
  }

  public SyncObjectIdSet getAllObjectIDs() {
    final SyncObjectIdSet rv = new SyncObjectIdSetImpl();
    rv.startPopulating();
    final Thread t = new Thread(new ObjectIdReader(rv), "ObjectIdReaderThread");
    t.setDaemon(true);
    t.start();
    return rv;
  }

  public SyncObjectIdSet getAllMapsObjectIDs() {
    return new NullSyncObjectIdSet();
  }

  public Set loadRootNames() {
    final Set rv = new HashSet();
    final Collection txns = this.rootDB.getAll();
    for (final Iterator i = txns.iterator(); i.hasNext();) {
      final TCByteArrayKeyValuePair pair = (TCByteArrayKeyValuePair) i.next();
      rv.add(pair.getKey().toString());
    }
    return rv;
  }

  public Map loadRootNamesToIDs() {
    final Map rv = new HashMap();
    final Collection txns = this.rootDB.getAll();
    for (final Iterator i = txns.iterator(); i.hasNext();) {
      final TCByteArrayKeyValuePair pair = (TCByteArrayKeyValuePair) i.next();
      rv.put(pair.getKey().toString(), dataToObjectID(pair.getValue()));
    }
    return rv;
  }

  public ManagedObject loadObjectByID(final ObjectID id) {
    validateID(id);
    try {
      final byte[] value = this.objectDB.get(objectIDToData(id));
      final ManagedObject mo = dataToManagedObject(value);
      loadCollection(mo);
      return mo;
    } catch (final Exception e) {
      throw new TCRuntimeException(e);
    }
  }

  private void loadCollection(final ManagedObject mo) {
    final ManagedObjectState state = mo.getManagedObjectState();
    if (PersistentCollectionsUtil.isPersistableCollectionType(state.getType())) {
      final MapManagedObjectState mapState = (MapManagedObjectState) state;
      Assert.assertNull(mapState.getMap());
      mapState.setMap(this.collectionsPersistor.loadMap(mo.getID()));
    }
  }

  public void saveObject(final PersistenceTransaction persistenceTransaction, final ManagedObject managedObject) {
    Assert.assertNotNull(managedObject);
    validateID(managedObject.getID());
    try {
      basicSaveObject(managedObject);
    } catch (final IOException e) {
      throw new TCRuntimeException(e);
    }
  }

  private boolean basicSaveObject(final ManagedObject managedObject) throws IOException {
    if (!managedObject.isDirty()) { return true; }
    this.objectDB.put(objectIDToData(managedObject.getID()), managedObjectToData(managedObject));
    basicSaveCollection(managedObject);
    managedObject.setIsDirty(false);
    this.saveCount++;
    if (this.saveCount == 1 || this.saveCount % (100 * 1000) == 0) {
      this.logger.debug("saveCount: " + this.saveCount);
    }
    return true;
  }

  private void basicSaveCollection(final ManagedObject managedObject) {
    final ManagedObjectState state = managedObject.getManagedObjectState();
    if (PersistentCollectionsUtil.isPersistableCollectionType(state.getType())) {
      final MapManagedObjectState mapState = (MapManagedObjectState) state;
      final MemoryStorePersistableMap map = (MemoryStorePersistableMap) mapState.getMap();
      this.collectionsPersistor.saveMap(map);
    }
  }

  public void saveAllObjects(final PersistenceTransaction persistenceTransaction, final Collection managedObjects) {
    final long t0 = System.currentTimeMillis();
    if (managedObjects.isEmpty()) { return; }
    Object failureContext = null;
    try {
      for (final Iterator i = managedObjects.iterator(); i.hasNext();) {
        final ManagedObject managedObject = (ManagedObject) i.next();

        final boolean status = basicSaveObject(managedObject);

        if (!status) {
          failureContext = new Object() {
            @Override
            public String toString() {
              return "Unable to save ManagedObject: " + managedObject + "; status: " + status;
            }
          };
          break;
        }
      }
    } catch (final IOException e) {
      throw new TCRuntimeException(e);
    }

    if (failureContext != null) { throw new TCRuntimeException(failureContext.toString()); }

    final long delta = System.currentTimeMillis() - t0;
    this.saveAllElapsed += delta;
    this.saveAllCount++;
    this.saveAllObjectCount += managedObjects.size();
    if (this.saveAllCount % (100 * 1000) == 0) {
      final double avg = ((double) this.saveAllObjectCount / (double) this.saveAllElapsed) * 1000;
      this.logger.debug("save time: " + delta + ", " + managedObjects.size() + " objects; avg: " + avg + "/sec");
    }
  }

  private long saveAllCount       = 0;
  private long saveAllObjectCount = 0;
  private long saveAllElapsed     = 0;

  private void deleteObjectByID(final PersistenceTransaction tx, final ObjectID id) {
    validateID(id);
    final byte[] key = objectIDToData(id);
    if (this.objectDB.get(key) != null) {
      this.objectDB.remove(objectIDToData(id));
    } else {
      this.collectionsPersistor.deleteCollection(id);
    }
  }

  public void deleteAllObjectsByID(final PersistenceTransaction tx, final SortedSet<ObjectID> objectIDs) {
    for (final Object element : objectIDs) {
      deleteObjectByID(tx, (ObjectID) element);
    }
  }

  /*********************************************************************************************************************
   * Private stuff
   */

  private void validateID(final ObjectID id) {
    Assert.assertNotNull(id);
    Assert.eval(!ObjectID.NULL_ID.equals(id));
  }

  private byte[] objectIDToData(final ObjectID objectID) {
    return (Conversion.long2Bytes(objectID.toLong()));
  }

  private byte[] managedObjectToData(final ManagedObject mo) throws IOException {
    final ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
    new ObjectOutputStream(byteStream).writeObject(mo);
    return (byteStream.toByteArray());
  }

  private ObjectID dataToObjectID(final byte[] entry) {
    return new ObjectID(Conversion.bytes2Long(entry));
  }

  private ManagedObject dataToManagedObject(final byte[] value) throws IOException, ClassNotFoundException {
    final ByteArrayInputStream byteStream = new ByteArrayInputStream(value);
    final ObjectInputStream objStream = new ObjectInputStream(byteStream);
    return ((ManagedObject) objStream.readObject());
  }

  public void prettyPrint(PrettyPrinter out) {
    out.println(this.getClass().getName());
    out = out.duplicateAndIndent();
    out.println("db: " + this.objectDB);
    out.indent().print("extantObjectIDs: ").visit(this.extantObjectIDs).println();
  }

  class ObjectIdReader implements Runnable {
    private final SyncObjectIdSet set;

    public ObjectIdReader(final SyncObjectIdSet set) {
      this.set = set;
    }

    public void run() {
      final ObjectIDSet tmp = new ObjectIDSet(MemoryStoreManagedObjectPersistor.this.objectDB.getAll());
      this.set.stopPopulating(tmp);
    }
  }

  public boolean addMapTypeObject(final ObjectID id) {
    return false;
  }

  public boolean containsMapType(final ObjectID id) {
    return false;
  }

  public void removeAllMapTypeObject(final Collection ids) {
    return;
  }

}
