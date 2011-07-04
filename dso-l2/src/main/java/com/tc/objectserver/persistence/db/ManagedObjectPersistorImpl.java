/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.persistence.db;

import com.tc.exception.TCRuntimeException;
import com.tc.logging.TCLogger;
import com.tc.object.ObjectID;
import com.tc.objectserver.core.api.ManagedObject;
import com.tc.objectserver.core.api.ManagedObjectState;
import com.tc.objectserver.mgmt.ObjectStatsRecorder;
import com.tc.objectserver.persistence.api.ManagedObjectPersistor;
import com.tc.objectserver.persistence.api.PersistentCollectionsUtil;
import com.tc.objectserver.persistence.db.DBPersistorImpl.DBPersistorBase;
import com.tc.objectserver.storage.api.DBEnvironment;
import com.tc.objectserver.storage.api.PersistenceTransaction;
import com.tc.objectserver.storage.api.PersistenceTransactionProvider;
import com.tc.objectserver.storage.api.TCDatabaseReturnConstants.Status;
import com.tc.objectserver.storage.api.TCLongToBytesDatabase;
import com.tc.objectserver.storage.api.TCRootDatabase;
import com.tc.properties.TCPropertiesConsts;
import com.tc.properties.TCPropertiesImpl;
import com.tc.text.PrettyPrintable;
import com.tc.text.PrettyPrinter;
import com.tc.util.Assert;
import com.tc.util.ObjectIDSet;
import com.tc.util.SyncObjectIdSet;
import com.tc.util.SyncObjectIdSetImpl;
import com.tc.util.sequence.MutableSequence;

import java.io.IOException;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

public final class ManagedObjectPersistorImpl extends DBPersistorBase implements ManagedObjectPersistor,
    PrettyPrintable {

  private static final Comparator                 MO_COMPARATOR          = new Comparator() {
                                                                           public int compare(final Object o1,
                                                                                              final Object o2) {
                                                                             final long oid1 = ((ManagedObject) o1)
                                                                                 .getID().toLong();
                                                                             final long oid2 = ((ManagedObject) o2)
                                                                                 .getID().toLong();
                                                                             if (oid1 < oid2) {
                                                                               return -1;
                                                                             } else if (oid1 > oid2) {
                                                                               return 1;
                                                                             } else {
                                                                               return 0;
                                                                             }
                                                                           }
                                                                         };

  private static final int                        DELETE_BATCH_SIZE      = TCPropertiesImpl
                                                                             .getProperties()
                                                                             .getInt(TCPropertiesConsts.L2_OBJECTMANAGER_DELETEBATCHSIZE,
                                                                                     5000);

  private static final long                       REMOVE_THRESHOLD       = 300;

  private static final int                        INTEGER_MAX_80_PERCENT = (int) (Integer.MAX_VALUE * 0.8);

  private final TCLongToBytesDatabase             objectDB;
  private final SerializationAdapterFactory       saf;
  private final MutableSequence                   objectIDSequence;
  private final TCRootDatabase                    rootDB;
  private long                                    saveCount;
  private final TCLogger                          logger;
  private final PersistenceTransactionProvider    ptp;
  private final TCCollectionsPersistor            collectionsPersistor;
  private final ObjectIDManager                   objectIDManager;
  private final SyncObjectIdSet                   extantObjectIDs;
  private final SyncObjectIdSet                   extantMapTypeOidSet;
  private final SyncObjectIdSet                   extantEvictableOidSet;
  private final ObjectStatsRecorder               objectStatsRecorder;

  private final ThreadLocal<SerializationAdapter> threadlocalAdapter;

  public ManagedObjectPersistorImpl(final TCLogger logger,
                                    final SerializationAdapterFactory serializationAdapterFactory,
                                    final DBEnvironment env, final MutableSequence objectIDSequence,
                                    final TCRootDatabase rootDB, final PersistenceTransactionProvider ptp,
                                    final TCCollectionsPersistor collectionsPersistor, final boolean paranoid,
                                    final ObjectStatsRecorder objectStatsRecorder) throws TCDatabaseException {
    this.logger = logger;
    this.saf = serializationAdapterFactory;
    this.objectDB = env.getObjectDatabase();
    this.objectIDSequence = objectIDSequence;
    this.rootDB = rootDB;
    this.ptp = ptp;
    this.collectionsPersistor = collectionsPersistor;

    this.threadlocalAdapter = initializethreadlocalAdapter();

    if (!paranoid) {
      this.objectIDManager = new NullObjectIDManager();
    } else {
      // read objectIDs from compressed DB
      final MutableSequence sequence = env.getSequence(this.ptp, logger, DBSequenceKeys.OID_STORE_LOG_SEQUENCE_DB_NAME,
                                                       1000);
      this.objectIDManager = new FastObjectIDManagerImpl(env, ptp, sequence);
    }

    this.extantObjectIDs = loadAllObjectIDs();
    this.extantMapTypeOidSet = loadAllMapsObjectIDs();
    this.extantEvictableOidSet = loadAllEvictableObjectIDs();

    this.objectStatsRecorder = objectStatsRecorder;
  }

  public int getObjectCount() {
    return this.extantObjectIDs.size();
  }

  public boolean addNewObject(final ManagedObject mo) {
    final ObjectID id = mo.getID();
    final int size = this.extantObjectIDs.addAndGetSize(id);
    boolean result = true;
    if (size < 0) {
      // not added
      return false;
    } else if (size > INTEGER_MAX_80_PERCENT && size % 10000 == 0) {
      this.logger.warn("Total number of objects in the system close to MAX supported : " + size + " MAX : "
                       + Integer.MAX_VALUE);
    }
    final byte type = mo.getManagedObjectState().getType();
    if (PersistentCollectionsUtil.isPersistableCollectionType(type)) {
      result &= addMapTypeObject(id);
    }
    if (PersistentCollectionsUtil.isEvictableMapType(type)) {
      result &= addEvictableTypeObject(id);
    }
    return result;
  }

  public boolean containsObject(final ObjectID id) {
    return this.extantObjectIDs.contains(id);
  }

  public void removeAllObjectIDs(final SortedSet<ObjectID> ids) {
    this.extantObjectIDs.removeAll(ids);
  }

  public ObjectIDSet snapshotMapTypeObjectIDs() {
    return this.extantMapTypeOidSet.snapshot();
  }

  public ObjectIDSet snapshotEvictableObjectIDs() {
    final ObjectIDSet evictables = this.extantEvictableOidSet.snapshot();
    // As deleted objects are not deleted from extantEvictableOidSet inline, we want to only return things that are
    evictables.retainAll(this.extantObjectIDs);
    return evictables;
  }

  public ObjectIDSet snapshotObjectIDs() {
    return this.extantObjectIDs.snapshot();
  }

  private boolean addMapTypeObject(final ObjectID id) {
    return this.extantMapTypeOidSet.add(id);
  }

  private void removeAllFromOtherExtantSets(final Collection ids) {
    this.extantMapTypeOidSet.removeAll(ids);
    this.extantEvictableOidSet.removeAll(ids);
  }

  private boolean addEvictableTypeObject(final ObjectID id) {
    return this.extantEvictableOidSet.add(id);
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
    boolean status = false;
    try {
      byte[] rootNameInBytes = setStringData(name);
      status = this.rootDB.put(rootNameInBytes, id.toLong(), tx) == Status.SUCCESS;
    } catch (final Throwable t) {
      throw new DBException(t);
    }
    if (!status) { throw new DBException("Unable to write root id: " + name + "=" + id + "; status: " + status); }
  }

  public ObjectID loadRootID(final String name) {
    if (name == null) { throw new AssertionError("Attempt to retrieve a null root name"); }
    try {
      byte[] rootNameInByte = setStringData(name);
      final PersistenceTransaction tx = this.ptp.newTransaction();

      ObjectID oid = new ObjectID(this.rootDB.getIdFromName(rootNameInByte, tx));
      tx.commit();
      return oid;
    } catch (final Throwable t) {
      throw new DBException(t);
    }
  }

  public Set loadRoots() {
    PersistenceTransaction tx = this.ptp.newTransaction();
    return rootDB.getRootIds(tx);
  }

  private SyncObjectIdSet loadAllObjectIDs() {
    final SyncObjectIdSet rv = new SyncObjectIdSetImpl();
    rv.startPopulating();
    final Thread t = new Thread(this.objectIDManager.getObjectIDReader(rv), "ObjectIdReaderThread");
    t.setDaemon(true);
    t.start();
    return rv;
  }

  private SyncObjectIdSet loadAllMapsObjectIDs() {
    final SyncObjectIdSet rv = new SyncObjectIdSetImpl();
    rv.startPopulating();
    final Thread t = new Thread(this.objectIDManager.getMapsObjectIDReader(rv), "MapsObjectIdReaderThread");
    t.setDaemon(true);
    t.start();
    return rv;
  }

  private SyncObjectIdSet loadAllEvictableObjectIDs() {
    final SyncObjectIdSet rv = new SyncObjectIdSetImpl();
    rv.startPopulating();
    final Thread t = new Thread(this.objectIDManager.getEvictableObjectIDReader(rv), "EvictableObjectIdReaderThread");
    t.setDaemon(true);
    t.start();
    return rv;
  }

  public Set loadRootNames() {
    final Set rv = new HashSet();
    PersistenceTransaction tx = this.ptp.newTransaction();

    List<byte[]> rootNamesInBytes = rootDB.getRootNames(tx);
    try {
      for (byte[] rootNameInBytes : rootNamesInBytes) {
        rv.add(getStringData(rootNameInBytes));
      }
    } catch (final Throwable t) {
      throw new DBException(t);
    }

    return rv;
  }

  public Map loadRootNamesToIDs() {
    final Map rv = new HashMap();
    try {
      PersistenceTransaction tx = this.ptp.newTransaction();
      Map<byte[], Long> mapFromDB = rootDB.getRootNamesToId(tx);

      for (Entry<byte[], Long> entry : mapFromDB.entrySet()) {
        String rootName = getStringData(entry.getKey());
        ObjectID oid = new ObjectID(entry.getValue());
        rv.put(rootName, oid);
      }
    } catch (final Throwable t) {
      throw new DBException(t);
    }
    return rv;
  }

  public ManagedObject loadObjectByID(final ObjectID id) {
    validateID(id);
    final PersistenceTransaction tx = this.ptp.newTransaction();
    try {
      byte[] value = this.objectDB.get(id.toLong(), tx);
      if (value != null) {
        final ManagedObject mo = getManagedObjectData(value);
        loadCollection(tx, mo);
        tx.commit();
        return mo;
      } else {
        return null;
      }
    } catch (final Throwable e) {
      abortOnError(tx);
      throw new DBException(e);
    }
  }

  private void loadCollection(final PersistenceTransaction tx, final ManagedObject mo) throws TCDatabaseException {
    final ManagedObjectState state = mo.getManagedObjectState();
    if (PersistentCollectionsUtil.isPersistableCollectionType(state.getType())) {
      try {
        this.collectionsPersistor.loadCollectionsToManagedState(tx, mo.getID(), state);
      } catch (final Exception e) {
        throw new TCDatabaseException(e.getMessage());
      }
    }
  }

  public void saveObject(final PersistenceTransaction persistenceTransaction, final ManagedObject managedObject) {
    Assert.assertNotNull(managedObject);
    validateID(managedObject.getID());
    boolean status = false;
    try {
      status = basicSaveObject(persistenceTransaction, managedObject);
      if (status && managedObject.isNew()) {
        status = this.objectIDManager.put(persistenceTransaction, managedObject);
      }
    } catch (final DBException e) {
      throw e;
    } catch (final Throwable t) {
      throw new DBException("Trying to save object: " + managedObject, t);
    }

    if (!status) { throw new DBException("Unable to write ManagedObject: " + managedObject + "; status: " + status); }
  }

  private boolean basicSaveObject(PersistenceTransaction tx, ManagedObject managedObject) throws TCDatabaseException,
      IOException {
    if (!managedObject.isDirty()) { return true; }
    Status status;
    byte[] value = setManagedObjectData(managedObject);
    int length = value.length;
    length += 8;

    try {
      if (managedObject.isNewInDB()) {
        status = this.objectDB.insert(managedObject.getID().toLong(), value, tx);
      } else {
        status = this.objectDB.update(managedObject.getID().toLong(), value, tx);
      }
      if (status == Status.SUCCESS) {
        length += basicSaveCollection(tx, managedObject);
        managedObject.setIsDirty(false);
        this.saveCount++;
        if (this.saveCount == 1 || this.saveCount % (100 * 1000) == 0) {
          this.logger.debug("saveCount: " + this.saveCount);
        }
      }
      if (this.objectStatsRecorder.getCommitDebug()) {
        updateStats(managedObject, length);
      }
    } catch (final Exception de) {
      throw new TCDatabaseException(de.getMessage());
    }
    return status == Status.SUCCESS;
  }

  private void updateStats(final ManagedObject managedObject, final int length) {
    final String className = managedObject.getManagedObjectState().getClassName();
    record(className, length, managedObject.isNew());
  }

  private void record(final String className, final int length, final boolean isNew) {
    this.objectStatsRecorder.updateCommitStats(className, length, isNew); // count, bytes written, new
  }

  private int basicSaveCollection(final PersistenceTransaction tx, final ManagedObject managedObject)
      throws TCDatabaseException {
    final ManagedObjectState state = managedObject.getManagedObjectState();
    if (PersistentCollectionsUtil.isPersistableCollectionType(state.getType())) {
      try {
        return this.collectionsPersistor.saveCollections(tx, state);
      } catch (final Exception e) {
        throw new TCDatabaseException(e.getMessage());
      }
    }
    return 0;
  }

  private long saveAllCount       = 0;
  private long saveAllObjectCount = 0;
  private long saveAllElapsed     = 0;

  public void saveAllObjects(final PersistenceTransaction persistenceTransaction, final Collection managedObjects) {
    final long t0 = System.currentTimeMillis();
    if (managedObjects.isEmpty()) { return; }

    // XXX:: We are sorting so that we maintain lock ordering when writing to sleepycat (check
    // SleepycatPersistableMap.basicClear()). This is done under the assumption that this method is not called
    // twice with the same transaction
    final SortedSet<ManagedObject> sortedManagedObjects = getSortedManagedObjectsSet(managedObjects);

    try {
      for (final Iterator i = sortedManagedObjects.iterator(); i.hasNext();) {
        final ManagedObject managedObject = (ManagedObject) i.next();

        final boolean status = basicSaveObject(persistenceTransaction, managedObject);
        if (!status) { throw new DBException("Unable to save ManagedObject: " + managedObject + "; status: " + status); }

        if (!managedObject.isNew()) {
          // Not interested anymore
          i.remove();
        }
      }
      if (!this.objectIDManager.putAll(persistenceTransaction, sortedManagedObjects)) { throw new DBException(
                                                                                                              "Failed to save Object-IDs"); }
    } catch (final DBException dbe) {
      throw dbe;
    } catch (final Throwable t) {
      throw new DBException(t);
    }

    final long delta = System.currentTimeMillis() - t0;
    this.saveAllElapsed += delta;
    this.saveAllCount++;
    this.saveAllObjectCount += managedObjects.size();
    if (this.saveAllCount % (100 * 1000) == 0) {
      final double avg = ((double) this.saveAllObjectCount / (double) this.saveAllElapsed) * 1000;
      this.logger.debug("save time: " + delta + ", " + managedObjects.size() + " objects; avg: " + avg + "/sec");
    }
  }

  private SortedSet<ManagedObject> getSortedManagedObjectsSet(final Collection managedObjects) {
    final TreeSet<ManagedObject> sorted = new TreeSet<ManagedObject>(MO_COMPARATOR);
    sorted.addAll(managedObjects);
    Assert.assertEquals(managedObjects.size(), sorted.size());
    return sorted;
  }

  private void deleteObjectByID(final PersistenceTransaction tx, final ObjectID id) {
    validateID(id);
    Status status = this.objectDB.delete(id.toLong(), tx);
    if (!(status == Status.SUCCESS || status == Status.NOT_FOUND)) { throw new DBException(
                                                                                           "Unable to remove ManagedObject for object id: "
                                                                                               + id + ", status: "
                                                                                               + status); }
  }

  private void deleteAllMaps(final SortedSet<ObjectID> sortedOids) throws TCDatabaseException {
    if (sortedOids.size() > 0) {
      this.collectionsPersistor.deleteAllCollections(this.ptp, sortedOids, this.extantMapTypeOidSet.snapshot());
    }
  }

  /**
   * This method takes a SortedSet of Object ID to delete for two reasons. 1) to maintain lock ordering - check
   * saveAllObjects 2) for performance reason The way we delete the objects is to delete all the entries of all the maps
   * first and then delete all the entries from the object manager. While deleting the entries from the map we delete it
   * in batches. See DEV-3881 for more details.
   */
  public void deleteAllObjects(final SortedSet<ObjectID> sortedGarbage) {
    try {
      deleteAllMaps(sortedGarbage);
      deleteAllObjectsFromStore(sortedGarbage);
    } catch (final TCDatabaseException e) {
      this.logger.error("Exception trying to delete oids : " + sortedGarbage.size(), e);
      throw new TCRuntimeException(e);
    }
  }

  private void deleteAllObjectsFromStore(final SortedSet<ObjectID> sortedGarbage) {
    final Iterator<ObjectID> iterator = sortedGarbage.iterator();
    while (iterator.hasNext()) {
      final long start = System.currentTimeMillis();
      final PersistenceTransaction tx = this.ptp.newTransaction();
      final SortedSet<ObjectID> split = new TreeSet<ObjectID>();
      for (int i = 0; i < DELETE_BATCH_SIZE && iterator.hasNext(); i++) {
        final ObjectID oid = iterator.next();
        deleteObjectByID(tx, oid);
        split.add(oid);
      }

      try {
        this.objectIDManager.deleteAll(tx, split, this.extantMapTypeOidSet, this.extantEvictableOidSet);
      } catch (final TCDatabaseException de) {
        throw new TCRuntimeException(de);
      }
      tx.commit();

      // NOTE:: Deleting from MapType and Evictable OIDs after we use the info for deleting from collections DB.
      removeAllFromOtherExtantSets(split);
      final long elapsed = System.currentTimeMillis() - start;
      if (elapsed > REMOVE_THRESHOLD) {
        this.logger.info("Removed " + split.size() + " objects in " + elapsed + " ms.");
      }
    }
  }

  /**
   * This is only package protected for tests.
   */
  SerializationAdapter getSerializationAdapter() {
    return this.threadlocalAdapter.get();
  }

  private ThreadLocal<SerializationAdapter> initializethreadlocalAdapter() {
    final ThreadLocal<SerializationAdapter> threadlclAdapter = new ThreadLocal<SerializationAdapter>() {
      @Override
      protected SerializationAdapter initialValue() {
        try {
          return ManagedObjectPersistorImpl.this.saf.newAdapter();
        } catch (final IOException e) {
          throw new RuntimeException(e);
        }
      }
    };
    return threadlclAdapter;
  }

  private void validateID(final ObjectID id) {
    if (id == null || ObjectID.NULL_ID.equals(id)) { throw new AssertionError("Not a valid ObjectID : " + id); }
  }

  private byte[] setStringData(String string) throws IOException {
    return getSerializationAdapter().serializeString(string);
  }

  private byte[] setManagedObjectData(ManagedObject mo) throws IOException {
    return getSerializationAdapter().serializeManagedObject(mo);
  }

  private String getStringData(byte[] entry) throws IOException, ClassNotFoundException {
    return getSerializationAdapter().deserializeString(entry);
  }

  private ManagedObject getManagedObjectData(byte[] entry) throws IOException, ClassNotFoundException {
    return getSerializationAdapter().deserializeManagedObject(entry);
  }

  public PrettyPrinter prettyPrint(PrettyPrinter out) {
    out.println(this.getClass().getName());
    out = out.duplicateAndIndent();
    out.println("db: " + this.objectDB);
    out.indent().print("extantObjectIDs: ").visit(this.extantObjectIDs).println();
    out.indent().print("extantMapTypeOidSet: ").visit(this.extantMapTypeOidSet).println();
    out.indent().print("extantEvictableOidSet: ").visit(this.extantEvictableOidSet).println();
    return out;
  }

  // for testing purpose only
  ObjectIDManager getOibjectIDManager() {
    return this.objectIDManager;
  }
}
