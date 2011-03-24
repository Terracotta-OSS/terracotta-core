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

public class ManagedObjectPersistorImplTest extends TCTestCase {
  private static final TCLogger             logger = TCLogging.getTestingLogger(ManagedObjectPersistorImplTest.class);
  private ManagedObjectPersistorImpl        managedObjectPersistor;
  private PersistentManagedObjectStore      objectStore;
  private PersistenceTransactionProvider    persistenceTransactionProvider;
  private TestSleepycatCollectionsPersistor testSleepycatCollectionsPersistor;
  private BerkeleyDBEnvironment             env;
  private FastObjectIDManagerImpl           oidManager;

  public ManagedObjectPersistorImplTest() {
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
    File dbHome;
    int count = 0;
    do {
      dbHome = new File(getTempDirectory(), getClass().getName() + "db" + (++count));
    } while (dbHome.exists());
    dbHome.mkdir();
    assertTrue(dbHome.exists());
    assertTrue(dbHome.isDirectory());
    System.out.println("DB Home: " + dbHome);
    return new BerkeleyDBEnvironment(paranoid, dbHome);
  }

  private Collection createRandomObjects(final int num, final boolean withPersistentCollectionState) {
    final Random r = new Random();
    final HashSet objects = new HashSet(num);
    final HashSet ids = new HashSet(num);
    for (int i = 0; i < num; i++) {
      final long id = (long) r.nextInt(num * 10) + 1;
      if (ids.add(Long.valueOf(id))) {
        final ManagedObject mo = new TestPersistentStateManagedObject(new ObjectID(id), new ArrayList<ObjectID>(),
                                                                      withPersistentCollectionState);
        objects.add(mo);
        this.objectStore.addNewObject(mo);
      }
    }
    logger.info("Test with " + objects.size() + " objects");
    return (objects);
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

  private SyncObjectIdSet getAllMapsObjectIDs() {
    final SyncObjectIdSet rv = new SyncObjectIdSetImpl();
    rv.startPopulating();
    final Thread t = new Thread(this.oidManager.getMapsObjectIDReader(rv), "ObjectIdReaderThread");
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

  private void verifyState(final Collection oidSet, final Collection objects) {
    for (final Iterator i = objects.iterator(); i.hasNext();) {
      final ManagedObject mo = (ManagedObject) i.next();
      assertTrue("PersistentCollectionMap missing " + mo.getID(), oidSet.contains((mo.getID())));
    }
  }

  public void testOidBitsArraySave() throws Exception {
    // wait for background retrieving persistent data
    this.objectStore.getAllObjectIDs();

    // publish data
    final Collection objects = createRandomObjects(15050, false);
    final PersistenceTransaction ptx = this.persistenceTransactionProvider.newTransaction();
    try {
      this.managedObjectPersistor.saveAllObjects(ptx, objects);
    } finally {
      ptx.commit();
    }

    runCheckpointToCompressedStorage();

    final Collection oidSet = getAllObjectIDs();

    // verify object IDs is in memory
    for (final Iterator i = objects.iterator(); i.hasNext();) {
      final ManagedObject mo = (ManagedObject) i.next();
      assertTrue("Object:" + mo.getID() + " missed in memory! ", oidSet.contains(mo.getID()));
    }

    verify(objects);
  }

  public void testOidBitsArrayDeleteHalf() throws Exception {
    // wait for background retrieving persistent data
    this.objectStore.getAllObjectIDs();

    // publish data
    final Collection objects = createRandomObjects(15050, false);
    PersistenceTransaction ptx = this.persistenceTransactionProvider.newTransaction();
    try {
      this.managedObjectPersistor.saveAllObjects(ptx, objects);
    } finally {
      ptx.commit();
    }

    runCheckpointToCompressedStorage();

    final int total = objects.size();
    final SortedSet<ObjectID> toDelete = new TreeSet<ObjectID>();
    final int count = 0;
    for (final Iterator i = objects.iterator(); (count < total / 2) && i.hasNext();) {
      final ManagedObject mo = (ManagedObject) i.next();
      toDelete.add(mo.getID());
      i.remove();
    }

    ptx = this.persistenceTransactionProvider.newTransaction();
    try {
      this.managedObjectPersistor.removeAllObjectIDs(toDelete);
      this.managedObjectPersistor.deleteAllObjects(toDelete);
    } finally {
      ptx.commit();
    }

    runCheckpointToCompressedStorage();

    getAllObjectIDs();
    verify(objects);
  }

  public void testCreateAndDeleteInSameBatch() throws Exception {
    // wait for background retrieving persistent data
    this.objectStore.getAllObjectIDs();

    // publish data
    final Collection objects = createRandomObjects(20, false);
    PersistenceTransaction ptx = this.persistenceTransactionProvider.newTransaction();
    try {
      this.managedObjectPersistor.saveAllObjects(ptx, objects);
    } finally {
      ptx.commit();
    }

    final TreeSet<ObjectID> objectIds = new TreeSet<ObjectID>();
    for (final Iterator i = objects.iterator(); i.hasNext();) {
      final ManagedObject mo = (ManagedObject) i.next();
      objectIds.add(mo.getID());
    }
    ptx = this.persistenceTransactionProvider.newTransaction();
    try {
      this.managedObjectPersistor.removeAllObjectIDs(objectIds);
      this.managedObjectPersistor.deleteAllObjects(objectIds);
    } finally {
      ptx.commit();
    }

    try {
      runCheckpointToCompressedStorage();
    } catch (final Exception e) {
      throw new RuntimeException(e);
    }

    objects.clear();
    verify(objects);

  }

  public void testOidBitsArrayDeleteAll() throws Exception {
    // wait for background retrieving persistent data
    this.objectStore.getAllObjectIDs();

    // publish data
    final Collection objects = createRandomObjects(15050, false);
    PersistenceTransaction ptx = this.persistenceTransactionProvider.newTransaction();
    try {
      this.managedObjectPersistor.saveAllObjects(ptx, objects);
    } finally {
      ptx.commit();
    }

    runCheckpointToCompressedStorage();

    final TreeSet<ObjectID> objectIds = new TreeSet<ObjectID>();
    for (final Iterator i = objects.iterator(); i.hasNext();) {
      final ManagedObject mo = (ManagedObject) i.next();
      objectIds.add(mo.getID());
    }
    ptx = this.persistenceTransactionProvider.newTransaction();
    try {
      this.managedObjectPersistor.removeAllObjectIDs(objectIds);
      this.managedObjectPersistor.deleteAllObjects(objectIds);
    } finally {
      ptx.commit();
    }

    runCheckpointToCompressedStorage();

    objects.clear();
    verify(objects);
  }

  public void testStateOidBitsArraySave() throws Exception {
    // wait for background retrieving persistent data
    this.objectStore.getAllObjectIDs();

    // publish data with persistentCollectionMap
    final Collection objects = createRandomObjects(15050, true);
    final PersistenceTransaction ptx = this.persistenceTransactionProvider.newTransaction();
    try {
      this.managedObjectPersistor.saveAllObjects(ptx, objects);
    } finally {
      ptx.commit();
    }

    runCheckpointToCompressedStorage();

    Collection oidSet = getAllObjectIDs();
    // verify object IDs is in memory
    for (final Iterator i = objects.iterator(); i.hasNext();) {
      final ManagedObject mo = (ManagedObject) i.next();
      assertTrue("Object:" + mo.getID() + " missed in memory! ", oidSet.contains(mo.getID()));
    }
    verify(objects);

    oidSet = getAllMapsObjectIDs();
    verifyState(oidSet, objects);
  }

  public void testStateOidBitsArrayDeleteHalf() throws Exception {
    // wait for background retrieving persistent data
    this.objectStore.getAllObjectIDs();

    // publish data with persistentCollectionMap
    final Collection objects = createRandomObjects(15050, true);
    PersistenceTransaction ptx = this.persistenceTransactionProvider.newTransaction();
    try {
      this.managedObjectPersistor.saveAllObjects(ptx, objects);
    } finally {
      ptx.commit();
    }

    runCheckpointToCompressedStorage();

    final int total = objects.size();
    final SortedSet<ObjectID> toDelete = new TreeSet<ObjectID>();
    final int count = 0;
    for (final Iterator i = objects.iterator(); (count < total / 2) && i.hasNext();) {
      final ManagedObject mo = (ManagedObject) i.next();
      toDelete.add(mo.getID());
      i.remove();
    }
    this.testSleepycatCollectionsPersistor.setCounter(0);
    ptx = this.persistenceTransactionProvider.newTransaction();
    try {
      this.managedObjectPersistor.removeAllObjectIDs(toDelete);
      this.managedObjectPersistor.deleteAllObjects(toDelete);
    } finally {
      ptx.commit();
    }
    assertEquals(1, this.testSleepycatCollectionsPersistor.getCounter());

    runCheckpointToCompressedStorage();

    getAllObjectIDs();
    verify(objects);

    final Collection oidSet = getAllMapsObjectIDs();
    verifyState(oidSet, objects);
  }

  public void testStateOidBitsArrayDeleteAll() throws Exception {
    // wait for background retrieving persistent data
    this.objectStore.getAllObjectIDs();

    // publish data
    final Collection objects = createRandomObjects(15050, true);
    PersistenceTransaction ptx = this.persistenceTransactionProvider.newTransaction();
    try {
      this.managedObjectPersistor.saveAllObjects(ptx, objects);
    } finally {
      ptx.commit();
    }

    runCheckpointToCompressedStorage();

    final TreeSet<ObjectID> objectIds = new TreeSet<ObjectID>();
    for (final Iterator i = objects.iterator(); i.hasNext();) {
      final ManagedObject mo = (ManagedObject) i.next();
      objectIds.add(mo.getID());
    }
    this.testSleepycatCollectionsPersistor.setCounter(0);
    ptx = this.persistenceTransactionProvider.newTransaction();
    try {
      this.managedObjectPersistor.removeAllObjectIDs(objectIds);
      this.managedObjectPersistor.deleteAllObjects(objectIds);
    } finally {
      ptx.commit();
    }
    assertEquals(1, this.testSleepycatCollectionsPersistor.getCounter());

    runCheckpointToCompressedStorage();

    objects.clear();
    verify(objects);

    final Collection oidSet = getAllMapsObjectIDs();
    verifyState(oidSet, objects);
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
      ++this.counter;
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
                                            final boolean isPersistentCollectionMap) {
      super(id, references);
      final byte type = (isPersistentCollectionMap) ? ManagedObjectState.MAP_TYPE : ManagedObjectState.PHYSICAL_TYPE;
      this.state = new TestManagedObjectState(type);
    }

    @Override
    public boolean isNew() {
      return true;
    }

    @Override
    public ManagedObjectState getManagedObjectState() {
      return this.state;
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

    private ManagedObjectPersistorImplTest getOuterType() {
      return ManagedObjectPersistorImplTest.this;
    }

  }

}
