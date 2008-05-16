/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.persistence.sleepycat;

import com.sleepycat.bind.serial.ClassCatalog;
import com.sleepycat.je.Cursor;
import com.sleepycat.je.CursorConfig;
import com.sleepycat.je.Database;
import com.sleepycat.je.DatabaseEntry;
import com.sleepycat.je.DatabaseException;
import com.sleepycat.je.LockMode;
import com.sleepycat.je.OperationStatus;
import com.tc.exception.TCRuntimeException;
import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.object.ObjectID;
import com.tc.objectserver.core.api.ManagedObject;
import com.tc.objectserver.core.api.ManagedObjectState;
import com.tc.objectserver.managedobject.MapManagedObjectState;
import com.tc.objectserver.persistence.api.ManagedObjectPersistor;
import com.tc.objectserver.persistence.api.PersistenceTransaction;
import com.tc.objectserver.persistence.api.PersistenceTransactionProvider;
import com.tc.objectserver.persistence.api.PersistentCollectionsUtil;
import com.tc.objectserver.persistence.sleepycat.SleepycatPersistor.SleepycatPersistorBase;
import com.tc.properties.TCPropertiesImpl;
import com.tc.properties.TCPropertiesConsts;
import com.tc.text.PrettyPrintable;
import com.tc.text.PrettyPrinter;
import com.tc.util.Assert;
import com.tc.util.Conversion;
import com.tc.util.SyncObjectIdSet;
import com.tc.util.SyncObjectIdSetImpl;
import com.tc.util.concurrent.ThreadUtil;
import com.tc.util.sequence.MutableSequence;

import java.io.IOException;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public final class ManagedObjectPersistorImpl extends SleepycatPersistorBase implements ManagedObjectPersistor,
    PrettyPrintable {

  private final static TCLogger                statsLogger           = TCLogging.getLogger("com.tc.StatsLogger");

  private static final Comparator              MO_COMPARATOR         = new Comparator() {
                                                                       public int compare(Object o1, Object o2) {
                                                                         long oid1 = ((ManagedObject) o1).getID()
                                                                             .toLong();
                                                                         long oid2 = ((ManagedObject) o2).getID()
                                                                             .toLong();
                                                                         if (oid1 < oid2) {
                                                                           return -1;
                                                                         } else if (oid1 > oid2) {
                                                                           return 1;
                                                                         } else {
                                                                           return 0;
                                                                         }
                                                                       }
                                                                     };

  private static final Object                  MO_PERSISTOR_KEY      = ManagedObjectPersistorImpl.class.getName()
                                                                       + ".saveAllObjects";
  private static final Object                  MO_PERSISTOR_VALUE    = "Complete";

  private static final boolean                 STATS_LOGGING_ENABLED = TCPropertiesImpl
                                                                         .getProperties()
                                                                         .getBoolean(
                                                                                     TCPropertiesConsts.L2_OBJECTMANAGER_PERSISTOR_LOGGING_ENABLED);

  private final Database                       objectDB;
  private final SerializationAdapterFactory    saf;
  private final MutableSequence                objectIDSequence;
  private final Database                       rootDB;
  private final CursorConfig                   rootDBCursorConfig;
  private long                                 saveCount;
  private final TCLogger                       logger;
  private final PersistenceTransactionProvider ptp;
  private final ClassCatalog                   classCatalog;
  private SerializationAdapter                 serializationAdapter;
  private final SleepycatCollectionsPersistor  collectionsPersistor;
  private final ObjectIDManager                objectIDManager;
  private final ConcurrentHashMap              statsRecords          = new ConcurrentHashMap();

  public ManagedObjectPersistorImpl(TCLogger logger, ClassCatalog classCatalog,
                                    SerializationAdapterFactory serializationAdapterFactory, Database objectDB,
                                    Database oidDB, Database oidLogDB, Database oidLogSeqDB,
                                    CursorConfig dBCursorConfig, MutableSequence objectIDSequence, Database rootDB,
                                    CursorConfig rootDBCursorConfig, PersistenceTransactionProvider ptp,
                                    SleepycatCollectionsPersistor collectionsPersistor, boolean paranoid) {
    this.logger = logger;
    this.classCatalog = classCatalog;
    this.saf = serializationAdapterFactory;
    this.objectDB = objectDB;
    this.objectIDSequence = objectIDSequence;
    this.rootDB = rootDB;
    this.rootDBCursorConfig = rootDBCursorConfig;
    this.ptp = ptp;
    this.collectionsPersistor = collectionsPersistor;

    boolean oidFastLoad = TCPropertiesImpl.getProperties().getBoolean(TCPropertiesConsts.L2_OBJECTMANAGER_LOADOBJECTID_FASTLOAD);
    if (!paranoid) {
      this.objectIDManager = new NullObjectIDManager();
    } else if (oidFastLoad) {
      // read objectIDs from compressed DB
      MutableSequence sequence = new SleepycatSequence(this.ptp, logger, 1, 1000, oidLogSeqDB);
      this.objectIDManager = new FastObjectIDManagerImpl(oidDB, oidLogDB, ptp, dBCursorConfig, sequence);
    } else {
      // read objectIDs from object DB
      this.objectIDManager = new PlainObjectIDManagerImpl(objectDB, ptp, dBCursorConfig);
    }

    if (STATS_LOGGING_ENABLED) startStatsPrinter();
  }

  public long nextObjectIDBatch(int batchSize) {
    return objectIDSequence.nextBatch(batchSize);
  }

  public void setNextAvailableObjectID(long startID) {
    objectIDSequence.setNext(startID);
  }

  public void addRoot(PersistenceTransaction tx, String name, ObjectID id) {
    validateID(id);
    OperationStatus status = null;
    try {
      DatabaseEntry key = new DatabaseEntry();
      DatabaseEntry value = new DatabaseEntry();
      setStringData(key, name);
      setObjectIDData(value, id);

      status = this.rootDB.put(pt2nt(tx), key, value);
    } catch (Throwable t) {
      throw new DBException(t);
    }
    if (!OperationStatus.SUCCESS.equals(status)) { throw new DBException("Unable to write root id: " + name + "=" + id
                                                                         + "; status: " + status); }
  }

  public ObjectID loadRootID(String name) {
    if (name == null) throw new AssertionError("Attempt to retrieve a null root name");
    OperationStatus status = null;
    try {
      DatabaseEntry key = new DatabaseEntry();
      DatabaseEntry value = new DatabaseEntry();
      setStringData(key, name);
      PersistenceTransaction tx = ptp.newTransaction();
      status = this.rootDB.get(pt2nt(tx), key, value, LockMode.DEFAULT);
      tx.commit();
      if (OperationStatus.SUCCESS.equals(status)) {
        ObjectID rv = getObjectIDData(value);
        return rv;
      }
    } catch (Throwable t) {
      throw new DBException(t);
    }
    if (OperationStatus.NOTFOUND.equals(status)) return ObjectID.NULL_ID;
    else throw new DBException("Error retrieving root: " + name + "; status: " + status);
  }

  public Set loadRoots() {
    Set rv = new HashSet();
    Cursor cursor = null;
    try {
      DatabaseEntry key = new DatabaseEntry();
      DatabaseEntry value = new DatabaseEntry();
      PersistenceTransaction tx = ptp.newTransaction();
      cursor = rootDB.openCursor(pt2nt(tx), rootDBCursorConfig);
      while (OperationStatus.SUCCESS.equals(cursor.getNext(key, value, LockMode.DEFAULT))) {
        rv.add(getObjectIDData(value));
      }
      cursor.close();
      tx.commit();
    } catch (Throwable t) {
      throw new DBException(t);
    }
    return rv;
  }

  public SyncObjectIdSet getAllObjectIDs() {
    SyncObjectIdSet rv = new SyncObjectIdSetImpl();
    rv.startPopulating();
    Thread t = new Thread(objectIDManager.getObjectIDReader(rv), "ObjectIdReaderThread");
    t.setDaemon(true);
    t.start();
    return rv;
  }

  public Set loadRootNames() {
    Set rv = new HashSet();
    Cursor cursor = null;
    try {
      PersistenceTransaction tx = ptp.newTransaction();
      DatabaseEntry key = new DatabaseEntry();
      DatabaseEntry value = new DatabaseEntry();
      cursor = rootDB.openCursor(pt2nt(tx), rootDBCursorConfig);
      while (OperationStatus.SUCCESS.equals(cursor.getNext(key, value, LockMode.DEFAULT))) {
        rv.add(getStringData(key));
      }
      cursor.close();
      tx.commit();
    } catch (Throwable t) {
      throw new DBException(t);
    }
    return rv;
  }

  public Map loadRootNamesToIDs() {
    Map rv = new HashMap();
    Cursor cursor = null;
    try {
      PersistenceTransaction tx = ptp.newTransaction();
      DatabaseEntry key = new DatabaseEntry();
      DatabaseEntry value = new DatabaseEntry();
      cursor = rootDB.openCursor(pt2nt(tx), rootDBCursorConfig);
      while (OperationStatus.SUCCESS.equals(cursor.getNext(key, value, LockMode.DEFAULT))) {
        rv.put(getStringData(key), getObjectIDData(value));
      }
      cursor.close();
      tx.commit();
    } catch (Throwable t) {
      throw new DBException(t);
    }
    return rv;
  }

  public ManagedObject loadObjectByID(ObjectID id) {
    validateID(id);
    OperationStatus status = null;
    PersistenceTransaction tx = ptp.newTransaction();
    try {
      DatabaseEntry key = new DatabaseEntry();
      DatabaseEntry value = new DatabaseEntry();
      setObjectIDData(key, id);
      status = this.objectDB.get(pt2nt(tx), key, value, LockMode.DEFAULT);
      if (OperationStatus.SUCCESS.equals(status)) {
        ManagedObject mo = getManagedObjectData(value);
        loadCollection(tx, mo);
        tx.commit();
        return mo;
      }
    } catch (Throwable e) {
      abortOnError(tx);
      throw new DBException(e);
    }
    if (OperationStatus.NOTFOUND.equals(status)) return null;
    else throw new DBException("Error retrieving object id: " + id + "; status: " + status);
  }

  private void loadCollection(PersistenceTransaction tx, ManagedObject mo) throws IOException, ClassNotFoundException,
      TCDatabaseException {
    ManagedObjectState state = mo.getManagedObjectState();
    if (PersistentCollectionsUtil.isPersistableCollectionType(state.getType())) {
      MapManagedObjectState mapState = (MapManagedObjectState) state;
      Assert.assertNull(mapState.getMap());
      try {
        mapState.setMap(collectionsPersistor.loadMap(tx, mo.getID()));
      } catch (DatabaseException e) {
        throw new TCDatabaseException(e);
      }
    }
  }

  public void saveObject(PersistenceTransaction persistenceTransaction, ManagedObject managedObject) {
    Assert.assertNotNull(managedObject);
    validateID(managedObject.getID());
    OperationStatus status = null;
    try {
      status = basicSaveObject(persistenceTransaction, managedObject);
      if (OperationStatus.SUCCESS.equals(status) && managedObject.isNew()) {
        status = objectIDManager.put(persistenceTransaction, managedObject.getID());
      }
    } catch (DBException e) {
      throw e;
    } catch (Throwable t) {
      throw new DBException("Trying to save object: " + managedObject, t);
    }

    if (!OperationStatus.SUCCESS.equals(status)) { throw new DBException("Unable to write ManagedObject: "
                                                                         + managedObject + "; status: " + status); }

  }

  private OperationStatus basicSaveObject(PersistenceTransaction tx, ManagedObject managedObject)
      throws TCDatabaseException, IOException {
    if (!managedObject.isDirty()) return OperationStatus.SUCCESS;
    OperationStatus status;
    DatabaseEntry key = new DatabaseEntry();
    DatabaseEntry value = new DatabaseEntry();
    setObjectIDData(key, managedObject.getID());
    setManagedObjectData(value, managedObject);
    int length = value.getSize();
    length += key.getSize();
    try {
      status = this.objectDB.put(pt2nt(tx), key, value);
      if (OperationStatus.SUCCESS.equals(status)) {
        length += basicSaveCollection(tx, managedObject);
        managedObject.setIsDirty(false);
        saveCount++;
        if (saveCount == 1 || saveCount % (100 * 1000) == 0) {
          logger.debug("saveCount: " + saveCount);
        }
      }
      if (STATS_LOGGING_ENABLED) updateStats(managedObject, length);
    } catch (DatabaseException de) {
      throw new TCDatabaseException(de);
    }
    return status;
  }

  private void updateStats(ManagedObject managedObject, int length) {
    String className = managedObject.getManagedObjectState().getClassName();
    record(className, length, managedObject.isNew());
  }

  private void record(String className, int length, boolean isNew) {
    StatsRecord r = (StatsRecord) statsRecords.get(className);
    if (r == null) {
      r = new StatsRecord(className);
      statsRecords.put(className, r);
    }
    r.update(length, isNew);
  }

  private void startStatsPrinter() {
    Thread t = new Thread(new Runnable() {
      public void run() {
        while (true) {
          ThreadUtil.reallySleep(5000);
          statsLogger.info("Commits in last 5 seconds");
          statsLogger.info("===========================");
          StatsRecord total = new StatsRecord("TOTAL");
          for (Iterator i = statsRecords.entrySet().iterator(); i.hasNext();) {
            Map.Entry e = (Entry) i.next();
            StatsRecord r = (StatsRecord) e.getValue();
            r.printDetailsIfNecessary(total);
          }
          total.printDetailsIfNecessary(null);
        }
      }
    }, "ManagedObjects Stats printer");
    t.start();
  }

  private static String createLogString(String className, long written, int count, int newCount) {
    StringBuilder sb = new StringBuilder();
    appendFixedSpaceString(sb, className, 50);
    sb.append(": bytes = ");
    appendFixedSpaceString(sb, String.valueOf(written), 8).append(" count = ");
    appendFixedSpaceString(sb, String.valueOf(count), 5).append(" new = ").append(newCount);
    return sb.toString();
  }

  private static StringBuilder appendFixedSpaceString(StringBuilder sb, String msg, int length) {
    int spaces = Math.max(length - msg.length(), 0);
    sb.append(msg);
    while (spaces-- > 0) {
      sb.append(" ");
    }
    return sb;
  }

  private int basicSaveCollection(PersistenceTransaction tx, ManagedObject managedObject) throws IOException,
      TCDatabaseException {
    ManagedObjectState state = managedObject.getManagedObjectState();
    if (PersistentCollectionsUtil.isPersistableCollectionType(state.getType())) {
      MapManagedObjectState mapState = (MapManagedObjectState) state;
      SleepycatPersistableMap map = (SleepycatPersistableMap) mapState.getMap();
      try {
        return collectionsPersistor.saveMap(tx, map);
      } catch (DatabaseException e) {
        throw new TCDatabaseException(e);
      }
    }
    return 0;
  }

  public void saveAllObjects(PersistenceTransaction persistenceTransaction, Collection managedObjects) {
    long t0 = System.currentTimeMillis();
    if (managedObjects.isEmpty()) return;
    Object failureContext = null;

    // XXX:: We are sorting so that we maintain lock ordering when writing to sleepycat (check
    // SleepycatPersistableMap.basicClear()). This is done under the assumption that this method is not called
    // twice with the same transaction
    Object old = persistenceTransaction.setProperty(MO_PERSISTOR_KEY, MO_PERSISTOR_VALUE);
    Assert.assertNull(old);
    SortedSet sortedList = getSortedManagedObjectsSet(managedObjects);
    SortedSet oidSet = new TreeSet();

    try {
      for (Iterator i = sortedList.iterator(); i.hasNext();) {
        final ManagedObject managedObject = (ManagedObject) i.next();

        final OperationStatus status = basicSaveObject(persistenceTransaction, managedObject);

        if (!OperationStatus.SUCCESS.equals(status)) {
          failureContext = new Object() {
            public String toString() {
              return "Unable to save ManagedObject: " + managedObject + "; status: " + status;
            }
          };
          break;
        }

        // record new object-IDs to be written to persistent store later.
        if (managedObject.isNew()) {
          objectIDManager.prePutAll(oidSet, managedObject.getID());
        }
      }
      if (!OperationStatus.SUCCESS.equals(objectIDManager.putAll(persistenceTransaction, oidSet))) { throw new DBException(
                                                                                                                           "Failed to save Object-IDs"); }
    } catch (Throwable t) {
      throw new DBException(t);
    }

    if (failureContext != null) throw new DBException(failureContext.toString());

    long delta = System.currentTimeMillis() - t0;
    saveAllElapsed += delta;
    saveAllCount++;
    saveAllObjectCount += managedObjects.size();
    if (saveAllCount % (100 * 1000) == 0) {
      double avg = ((double) saveAllObjectCount / (double) saveAllElapsed) * 1000;
      logger.debug("save time: " + delta + ", " + managedObjects.size() + " objects; avg: " + avg + "/sec");
    }
  }

  private SortedSet getSortedManagedObjectsSet(Collection managedObjects) {
    TreeSet sorted = new TreeSet(MO_COMPARATOR);
    sorted.addAll(managedObjects);
    Assert.assertEquals(managedObjects.size(), sorted.size());
    return sorted;
  }

  private long saveAllCount       = 0;
  private long saveAllObjectCount = 0;
  private long saveAllElapsed     = 0;

  private void deleteObjectByID(PersistenceTransaction tx, ObjectID id) {
    validateID(id);
    try {
      DatabaseEntry key = new DatabaseEntry();
      setObjectIDData(key, id);
      OperationStatus status = this.objectDB.delete(pt2nt(tx), key);
      if (!(OperationStatus.NOTFOUND.equals(status) || OperationStatus.SUCCESS.equals(status))) {
        // make the formatter happy
        throw new DBException("Unable to remove ManagedObject for object id: " + id + ", status: " + status);
      } else {
        collectionsPersistor.deleteCollection(tx, id);
      }
    } catch (DatabaseException t) {
      throw new DBException(t);
    }
  }

  /*
   *  This method takes a SortedSet of Object ID to delete for two reasons.
   *  1) to maintain lock ordering - check saveAllObjects
   *  2) for performance reason
   */
  public void deleteAllObjectsByID(PersistenceTransaction tx, SortedSet<ObjectID> sortedOids) {
    for (Iterator i = sortedOids.iterator(); i.hasNext();) {
      ObjectID objectId = (ObjectID) i.next();
      deleteObjectByID(tx, objectId);
    }

    try {
      objectIDManager.deleteAll(tx, sortedOids);
    } catch (TCDatabaseException de) {
      throw new TCRuntimeException(de);
    }
  }

  /**
   * This is only package protected for tests.
   */
  SerializationAdapter getSerializationAdapter() throws IOException {
    // XXX: This lazy initialization comes from how the sleepycat stuff is glued together in the server.
    if (serializationAdapter == null) serializationAdapter = saf.newAdapter(this.classCatalog);
    return serializationAdapter;
  }

  /*********************************************************************************************************************
   * Private stuff
   */

  private void validateID(ObjectID id) {
    Assert.assertNotNull(id);
    Assert.eval(!ObjectID.NULL_ID.equals(id));
  }

  private void setObjectIDData(DatabaseEntry entry, ObjectID objectID) {
    entry.setData(Conversion.long2Bytes(objectID.toLong()));
  }

  private void setStringData(DatabaseEntry entry, String string) throws IOException {
    getSerializationAdapter().serializeString(entry, string);
  }

  private void setManagedObjectData(DatabaseEntry entry, ManagedObject mo) throws IOException {
    getSerializationAdapter().serializeManagedObject(entry, mo);
  }

  private ObjectID getObjectIDData(DatabaseEntry entry) {
    return new ObjectID(Conversion.bytes2Long(entry.getData()));
  }

  private String getStringData(DatabaseEntry entry) throws IOException, ClassNotFoundException {
    return getSerializationAdapter().deserializeString(entry);
  }

  private ManagedObject getManagedObjectData(DatabaseEntry entry) throws IOException, ClassNotFoundException {
    return getSerializationAdapter().deserializeManagedObject(entry);
  }

  public PrettyPrinter prettyPrint(PrettyPrinter out) {
    out.println(this.getClass().getName());
    out = out.duplicateAndIndent();
    out.println("db: " + objectDB);
    return out;
  }

  // for testing purpose only
  ObjectIDManager getOibjectIDManager() {
    return objectIDManager;
  }

  private static final class StatsRecord {
    private final AtomicInteger written    = new AtomicInteger(0);
    private final AtomicInteger count      = new AtomicInteger(0);
    private final AtomicInteger newObjects = new AtomicInteger(0);
    private final String        className;

    public StatsRecord(String className) {
      this.className = className;
    }

    public void update(int length, boolean isNew) {
      written.addAndGet(length);
      count.incrementAndGet();
      if (isNew) newObjects.incrementAndGet();
    }

    public void printDetailsIfNecessary(StatsRecord total) {
      int length = written.getAndSet(0);
      int c = count.getAndSet(0);
      int newCount = newObjects.getAndSet(0);
      if (c != 0) {
        statsLogger.info(createLogString(className, length, c, newCount));
        if (total != null) total.add(length, c, newCount);
      }
    }

    private void add(int length, int c, int newCount) {
      written.addAndGet(length);
      count.addAndGet(c);
      newObjects.addAndGet(newCount);
    }
  }
}
