/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.persistence.db;

import com.tc.async.impl.MockSink;
import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.object.ObjectID;
import com.tc.object.dna.api.DNA.DNAType;
import com.tc.object.dna.api.DNACursor;
import com.tc.object.dna.api.DNAWriter;
import com.tc.objectserver.core.api.ManagedObject;
import com.tc.objectserver.core.api.ManagedObjectState;
import com.tc.objectserver.core.impl.TestManagedObject;
import com.tc.objectserver.impl.PersistentManagedObjectStore;
import com.tc.objectserver.managedobject.AbstractManagedObjectState;
import com.tc.objectserver.managedobject.ApplyTransactionInfo;
import com.tc.objectserver.managedobject.ManagedObjectTraverser;
import com.tc.objectserver.mgmt.ManagedObjectFacade;
import com.tc.objectserver.mgmt.ObjectStatsRecorder;
import com.tc.objectserver.persistence.db.FastObjectIDManagerImpl.StoppedFlag;
import com.tc.objectserver.persistence.impl.TestMutableSequence;
import com.tc.objectserver.storage.api.PersistenceTransaction;
import com.tc.objectserver.storage.api.PersistenceTransactionProvider;
import com.tc.objectserver.storage.api.TCMapsDatabase;
import com.tc.objectserver.storage.berkeleydb.BerkeleyDBEnvironment;
import com.tc.objectserver.storage.berkeleydb.BerkeleyDBPersistenceTransactionProvider;
import com.tc.test.TCTestCase;
import com.tc.util.ObjectIDSet;
import com.tc.util.SyncObjectIdSet;
import com.tc.util.SyncObjectIdSetImpl;

import java.io.File;
import java.io.ObjectOutput;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Random;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

public class ManagedObjectPersistorEvictableTest extends TCTestCase {
  private static final TCLogger             logger = TCLogging
                                                       .getTestingLogger(ManagedObjectPersistorEvictableTest.class);

  private ManagedObjectPersistorImpl        managedObjectPersistor;
  private PersistentManagedObjectStore      objectStore;
  private PersistenceTransactionProvider    persistenceTransactionProvider;
  private TestSleepycatCollectionsPersistor testSleepycatCollectionsPersistor;
  private BerkeleyDBEnvironment             env;
  private FastObjectIDManagerImpl           oidManager;

  // Passing across tests
  private static HashSet<ManagedObject>     globalAllObjects;
  private static HashSet<ManagedObject>     globalEvictableObjects;

  public ManagedObjectPersistorEvictableTest() {
    //
  }

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    final boolean paranoid = true;
    this.env = newDBEnvironment(paranoid);
    this.env.open();
    this.persistenceTransactionProvider = new BerkeleyDBPersistenceTransactionProvider(this.env.getEnvironment());
    final PersistableCollectionFactory sleepycatCollectionFactory = new PersistableCollectionFactory(
                                                                                                     new HashMapBackingMapFactory(),
                                                                                                     this.env
                                                                                                         .isParanoidMode());
    this.testSleepycatCollectionsPersistor = new TestSleepycatCollectionsPersistor(logger, this.env.getMapsDatabase(),
                                                                                   sleepycatCollectionFactory,
                                                                                   new TCCollectionsSerializerImpl());
    this.managedObjectPersistor = new ManagedObjectPersistorImpl(logger, new CustomSerializationAdapterFactory(),
                                                                 this.env, new TestMutableSequence(),
                                                                 this.env.getRootDatabase(),
                                                                 this.persistenceTransactionProvider,
                                                                 this.testSleepycatCollectionsPersistor,
                                                                 this.env.isParanoidMode(), new ObjectStatsRecorder());
    this.objectStore = new PersistentManagedObjectStore(this.managedObjectPersistor, new MockSink());
    this.oidManager = (FastObjectIDManagerImpl) this.managedObjectPersistor.getOibjectIDManager();
  }

  @Override
  protected void tearDown() throws Exception {
    this.oidManager.stopCheckpointRunner();
    this.env.close();
    super.tearDown();
  }

  @Override
  protected boolean cleanTempDir() {
    return false;
  }

  private BerkeleyDBEnvironment newDBEnvironment(final boolean paranoid) throws Exception {
    final File dbHome = new File(getTempDirectory(), getClass().getName() + "db");
    dbHome.mkdir();
    assertTrue(dbHome.exists());
    assertTrue(dbHome.isDirectory());
    System.out.println("DB Home: " + dbHome);
    return new BerkeleyDBEnvironment(paranoid, dbHome);
  }

  private HashSet<ManagedObject> createPhyscalObjects(final int num) {
    return createRandomObjects(num, ManagedObjectState.PHYSICAL_TYPE);
  }

  private HashSet<ManagedObject> createMapObjects(final int num) {
    return createRandomObjects(num, ManagedObjectState.MAP_TYPE);
  }

  private HashSet<ManagedObject> createCDSMObjects(final int num) {
    return createRandomObjects(num, ManagedObjectState.CONCURRENT_DISTRIBUTED_SERVER_MAP_TYPE);
  }

  private HashSet<ManagedObject> createRandomObjects(final int num, final byte stateType) {
    final Random r = new Random(System.currentTimeMillis());
    final HashSet<ManagedObject> objects = new HashSet<ManagedObject>();
    for (int i = 0; i < num; i++) {
      final long id = (long) r.nextInt(num * 100) + 1;
      final ManagedObject mo = new TestPersistentStateManagedObject(new ObjectID(id), new ArrayList<ObjectID>(),
                                                                    stateType);
      objects.add(mo);
    }
    return (objects);
  }

  private HashSet<ManagedObject> addToObjectStore(final HashSet<ManagedObject> objects,
                                                  final HashSet<ManagedObject> newObjects) {
    final HashSet<ManagedObject> duplicated = new HashSet<ManagedObject>();
    final ObjectIDSet evictableSet = this.objectStore.getAllEvictableObjectIDs();
    newObjects.removeAll(objects);
    for (final ManagedObject mo : newObjects) {
      if (!this.objectStore.containsObject(mo.getID()) && !evictableSet.contains(mo.getID())) {
        objects.add(mo);
        this.objectStore.addNewObject(mo);
      } else {
        duplicated.add(mo);
      }
    }
    newObjects.removeAll(duplicated);
    logger.info("Added " + newObjects.size() + " objects");
    return objects;
  }

  private SyncObjectIdSet getAllObjectIDs() {
    final SyncObjectIdSet rv = new SyncObjectIdSetImpl();
    rv.startPopulating();
    final Thread t = new Thread(this.oidManager.getObjectIDReader(rv), "ObjectIdReaderThread");
    t.setDaemon(true);
    t.start();
    try {
      t.join();
    } catch (final InterruptedException e) {
      throw new AssertionError(e);
    }
    return rv;
  }

  private SyncObjectIdSet getAllEvictableObjectIDs() {
    final SyncObjectIdSet rv = new SyncObjectIdSetImpl();
    rv.startPopulating();
    final Thread t = new Thread(this.oidManager.getEvictableObjectIDReader(rv), "EvictableObjectIdReaderThread");
    t.setDaemon(true);
    t.start();
    try {
      t.join();
    } catch (final InterruptedException e) {
      throw new AssertionError(e);
    }
    return rv;
  }

  private void verify(final Collection objects) {
    // verify an in-memory bit correspond to an object ID
    final HashSet originalIds = new HashSet();
    for (final Iterator i = objects.iterator(); i.hasNext();) {
      final ManagedObject mo = (ManagedObject) i.next();
      originalIds.add(mo.getID());
    }

    final Collection inMemoryIds = getAllObjectIDs();
    assertTrue("Wrong bits in memory were set", originalIds.containsAll(inMemoryIds));

    // verify on disk object IDs
    final ObjectIDSet idSet = this.managedObjectPersistor.snapshotObjectIDs();
    assertTrue("Wrong object IDs on disk", idSet.containsAll(inMemoryIds));
    assertTrue("Wrong object IDs on disk", inMemoryIds.containsAll(idSet));
  }

  private void verifyObjectIDSet(final Collection objectIDSet, final Collection managedObjectSet) {
    assertEquals("Size is different", objectIDSet.size(), managedObjectSet.size());
    for (final Iterator i = managedObjectSet.iterator(); i.hasNext();) {
      final ManagedObject mo = (ManagedObject) i.next();
      assertTrue("Missing " + mo.getID(), objectIDSet.contains((mo.getID())));
    }
  }

  // wait for background retrieving persistent data
  private void waitForBackgroupTasks() {
    this.objectStore.getAllObjectIDs();
    this.objectStore.getAllEvictableObjectIDs();
    this.objectStore.getAllMapTypeObjectIDs();
  }

  public void testEvictableObjectsStep1() throws Exception {
    waitForBackgroupTasks();

    // publish CDSM data
    final HashSet<ManagedObject> newObjects = createCDSMObjects(15050);
    final HashSet<ManagedObject> objects = addToObjectStore(new HashSet<ManagedObject>(), newObjects);
    PersistenceTransaction ptx = this.persistenceTransactionProvider.newTransaction();
    try {
      this.managedObjectPersistor.saveAllObjects(ptx, objects);
    } finally {
      ptx.commit();
    }

    // consume add-logs into disk store
    runCheckpointToCompressedStorage();

    // delete half
    final int total = objects.size();
    final SortedSet<ObjectID> toDelete = new TreeSet<ObjectID>();
    int count = 0;
    for (final Iterator i = objects.iterator(); (count < total / 2) && i.hasNext();) {
      final ManagedObject mo = (ManagedObject) i.next();
      toDelete.add(mo.getID());
      i.remove();
      ++count;
    }

    this.testSleepycatCollectionsPersistor.setCounter(0);
    ptx = this.persistenceTransactionProvider.newTransaction();
    try {
      this.managedObjectPersistor.deleteAllObjects(toDelete);
      this.managedObjectPersistor.removeAllObjectIDs(toDelete);
    } finally {
      ptx.commit();
    }
    assertEquals(toDelete.size(), this.testSleepycatCollectionsPersistor.getCounter());

    // consume delete-logs to disk store
    runCheckpointToCompressedStorage();

    getAllObjectIDs();
    verify(objects);

    final Collection evictableSet = this.managedObjectPersistor.snapshotEvictableObjectIDs();
    verifyObjectIDSet(evictableSet, objects);

    // save for next test
    globalAllObjects = objects;
  }

  public void testEvictableObjectsStep2() throws Exception {
    waitForBackgroupTasks();

    final HashSet<ManagedObject> objects = globalAllObjects;
    globalEvictableObjects = new HashSet<ManagedObject>(objects);

    // after restarted, evictable set shall be the same
    Collection evictableSet = this.managedObjectPersistor.snapshotEvictableObjectIDs();
    verifyObjectIDSet(evictableSet, objects);
    verify(objects);

    // add more objects
    final HashSet<ManagedObject> newObjects2 = createMapObjects(1344);
    addToObjectStore(objects, newObjects2);
    final HashSet<ManagedObject> newObjects = createPhyscalObjects(1234);
    addToObjectStore(objects, newObjects);
    final HashSet<ManagedObject> newEvitcable = createCDSMObjects(700);
    addToObjectStore(objects, newEvitcable);
    globalEvictableObjects.addAll(newEvitcable);
    newObjects.addAll(newObjects2);
    newObjects.addAll(newEvitcable);
    final PersistenceTransaction ptx = this.persistenceTransactionProvider.newTransaction();
    try {
      this.managedObjectPersistor.saveAllObjects(ptx, newObjects);
    } finally {
      ptx.commit();
    }

    // consume add-logs into disk store
    runCheckpointToCompressedStorage();
    objects.addAll(newObjects);
    verify(objects);

    evictableSet = this.managedObjectPersistor.snapshotEvictableObjectIDs();
    verifyObjectIDSet(evictableSet, globalEvictableObjects);

  }

  public void testEvictableObjectsStep3() throws Exception {
    waitForBackgroupTasks();

    final HashSet<ManagedObject> objects = globalAllObjects;

    // after restarted, evictable set shall be the same
    final Collection evictableSet = this.managedObjectPersistor.snapshotEvictableObjectIDs();
    verifyObjectIDSet(evictableSet, globalEvictableObjects);
    verify(objects);

    // delete all
    final SortedSet<ObjectID> toDelete = new TreeSet<ObjectID>();
    for (final Iterator i = objects.iterator(); i.hasNext();) {
      final ManagedObject mo = (ManagedObject) i.next();
      toDelete.add(mo.getID());
      i.remove();
    }

    // this.testSleepycatCollectionsPersistor.setCounter(0);
    final PersistenceTransaction ptx = this.persistenceTransactionProvider.newTransaction();
    try {
      this.managedObjectPersistor.deleteAllObjects(toDelete);
      this.managedObjectPersistor.removeAllObjectIDs(toDelete);
    } finally {
      ptx.commit();
    }
    // assertEquals(toDelete.size(), this.testSleepycatCollectionsPersistor.getCounter());

  }

  public void testEvictableObjectsStep4() throws Exception {
    waitForBackgroupTasks();

    final Collection objects = getAllObjectIDs();
    assertEquals(0, objects.size());

    final Collection evictableSet = getAllEvictableObjectIDs();
    assertEquals(0, evictableSet.size());
  }

  private void runCheckpointToCompressedStorage() {
    this.oidManager.flushToCompressedStorage(new StoppedFlag(), Integer.MAX_VALUE);
  }

  private static class TestSleepycatCollectionsPersistor extends TCCollectionsPersistor {
    private int counter;

    public TestSleepycatCollectionsPersistor(final TCLogger logger, final TCMapsDatabase mapsDatabase,
                                             final PersistableCollectionFactory sleepycatCollectionFactory,
                                             final TCCollectionsSerializer serializer) {
      super(logger, mapsDatabase, sleepycatCollectionFactory, serializer);
    }

    @Override
    public long deleteAllCollections(final PersistenceTransactionProvider ptp, final SortedSet<ObjectID> mapIds,
                                     final SortedSet<ObjectID> mapObjectIds) {
      this.counter += mapIds.size();
      return this.counter;
    }

    public void setCounter(final int value) {
      this.counter = value;
    }

    public int getCounter() {
      return this.counter;
    }
  }

  private class TestPersistentStateManagedObject extends TestManagedObject {

    private final ManagedObjectState state;

    public TestPersistentStateManagedObject(final ObjectID id, final ArrayList<ObjectID> references,
                                            final byte stateType) {
      super(id, references);
      this.state = new TestManagedObjectState(stateType);
    }

    @Override
    public boolean isNew() {
      return true;
    }

    @Override
    public ManagedObjectState getManagedObjectState() {
      return this.state;
    }

    @Override
    public boolean equals(final Object other) {
      if (!(other instanceof TestPersistentStateManagedObject)) { return false; }
      final TestPersistentStateManagedObject o = (TestPersistentStateManagedObject) other;
      return getID().toLong() == o.getID().toLong();
    }

    @Override
    public int hashCode() {
      return (int) getID().toLong();
    }
  }

  private class TestManagedObjectState extends AbstractManagedObjectState {
    private final byte type;

    public TestManagedObjectState(final byte type) {
      this.type = type;
    }

    @Override
    protected boolean basicEquals(final AbstractManagedObjectState o) {
      return false;
    }

    public void addObjectReferencesTo(final ManagedObjectTraverser traverser) {
      return;
    }

    public void apply(final ObjectID objectID, final DNACursor cursor, final ApplyTransactionInfo includeIDs) {
      return;
    }

    public ManagedObjectFacade createFacade(final ObjectID objectID, final String className, final int limit) {
      return null;
    }

    public void dehydrate(final ObjectID objectID, final DNAWriter writer, final DNAType dnaType) {
      return;
    }

    public String getClassName() {
      return null;
    }

    public String getLoaderDescription() {
      return null;
    }

    public Set getObjectReferences() {
      return null;
    }

    public byte getType() {
      return this.type;
    }

    public void writeTo(final ObjectOutput o) {
      return;
    }

    @Override
    public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result + getOuterType().hashCode();
      result = prime * result + type;
      return result;
    }

    private ManagedObjectPersistorEvictableTest getOuterType() {
      return ManagedObjectPersistorEvictableTest.this;
    }

  }

}
