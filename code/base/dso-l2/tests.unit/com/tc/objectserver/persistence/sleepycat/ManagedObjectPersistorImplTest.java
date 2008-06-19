/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.persistence.sleepycat;

import com.sleepycat.je.CursorConfig;
import com.tc.async.impl.MockSink;
import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.object.ObjectID;
import com.tc.objectserver.core.api.ManagedObject;
import com.tc.objectserver.core.impl.TestManagedObject;
import com.tc.objectserver.impl.PersistentManagedObjectStore;
import com.tc.objectserver.persistence.api.PersistenceTransaction;
import com.tc.objectserver.persistence.api.PersistenceTransactionProvider;
import com.tc.objectserver.persistence.impl.TestMutableSequence;
import com.tc.objectserver.persistence.sleepycat.FastObjectIDManagerImpl.OidBitsArrayMap;
import com.tc.properties.TCProperties;
import com.tc.properties.TCPropertiesImpl;
import com.tc.properties.TCPropertiesConsts;
import com.tc.test.TCTestCase;
import com.tc.util.SyncObjectIdSet;

import java.io.File;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Random;
import java.util.SortedSet;
import java.util.TreeSet;

public class ManagedObjectPersistorImplTest extends TCTestCase {
  private static final TCLogger          logger = TCLogging.getTestingLogger(ManagedObjectPersistorImplTest.class);
  private ManagedObjectPersistorImpl     managedObjectPersistor;
  private PersistentManagedObjectStore   objectStore;
  private PersistenceTransactionProvider persistenceTransactionProvider;
  private DBEnvironment                  env;
  private FastObjectIDManagerImpl        oidManager;

  public ManagedObjectPersistorImplTest() {
    //
  }

  protected void setUp() throws Exception {
    super.setUp();
    // test only with Oid fastLoad enabled
    TCProperties tcProps = TCPropertiesImpl.getProperties();
    tcProps.setProperty(TCPropertiesConsts.L2_OBJECTMANAGER_LOADOBJECTID_FASTLOAD, "true");
    assertTrue(TCPropertiesImpl.getProperties().getBoolean(TCPropertiesConsts.L2_OBJECTMANAGER_LOADOBJECTID_FASTLOAD));
    boolean paranoid = true;
    env = newDBEnvironment(paranoid);
    env.open();
    CursorConfig dbCursorConfig = new CursorConfig();
    persistenceTransactionProvider = new SleepycatPersistenceTransactionProvider(env.getEnvironment());
    CursorConfig rootDBCursorConfig = new CursorConfig();
    SleepycatCollectionFactory sleepycatCollectionFactory = new SleepycatCollectionFactory();
    SleepycatCollectionsPersistor sleepycatCollectionsPersistor = new SleepycatCollectionsPersistor(logger, env
        .getMapsDatabase(), sleepycatCollectionFactory);
    managedObjectPersistor = new ManagedObjectPersistorImpl(logger, env.getClassCatalogWrapper().getClassCatalog(),
                                                            new SleepycatSerializationAdapterFactory(), env
                                                                .getObjectDatabase(), env.getOidDatabase(), env
                                                                .getOidLogDatabase(), env.getOidLogSequeneceDB(),
                                                            dbCursorConfig, new TestMutableSequence(), env
                                                                .getRootDatabase(), rootDBCursorConfig,
                                                            persistenceTransactionProvider,
                                                            sleepycatCollectionsPersistor, env.isParanoidMode());
    objectStore = new PersistentManagedObjectStore(managedObjectPersistor, new MockSink());
    oidManager = (FastObjectIDManagerImpl) managedObjectPersistor.getOibjectIDManager();
  }

  protected void tearDown() throws Exception {
    oidManager.stopCheckpointRunner();
    env.close();
    super.tearDown();
  }

  protected boolean cleanTempDir() {
    return false;
  }

  private DBEnvironment newDBEnvironment(boolean paranoid) throws Exception {
    File dbHome;
    int count = 0;
    do {
      dbHome = new File(this.getTempDirectory(), getClass().getName() + "db" + (++count));
    } while (dbHome.exists());
    dbHome.mkdir();
    assertTrue(dbHome.exists());
    assertTrue(dbHome.isDirectory());
    System.out.println("DB Home: " + dbHome);
    return new DBEnvironment(paranoid, dbHome);
  }

  private Collection createRandomObjects(int num) {
    Random r = new Random();
    HashSet objects = new HashSet(num);
    HashSet ids = new HashSet(num);
    for (int i = 0; i < num; i++) {
      long id = (long) r.nextInt(num * 10) + 1;
      if (ids.add(new Long(id))) {
        ManagedObject mo = new TestManagedObject(new ObjectID(id), new ObjectID[] {}) {
          public boolean isNew() {
            return true;
          }
        };
        objects.add(mo);
      }
    }
    logger.info("Test with " + objects.size() + " objects");
    return (objects);
  }

  private void verify(Collection objects) {
    // verify an in-memory bit crosspond to an object ID
    HashSet originalIds = new HashSet();
    for (Iterator i = objects.iterator(); i.hasNext();) {
      ManagedObject mo = (ManagedObject) i.next();
      originalIds.add(mo.getID());
    }
    Collection inMemoryIds = oidManager.bitsArrayMapToObjectID();
    assertTrue("Wrong bits in memory were set", originalIds.containsAll(inMemoryIds));

    // verify on disk object IDs
    SyncObjectIdSet idSet = managedObjectPersistor.getAllObjectIDs();
    idSet.snapshot(); // blocked while reading from disk
    assertTrue("Wrong object IDs on disk", idSet.containsAll(inMemoryIds));
    assertTrue("Wrong object IDs on disk", inMemoryIds.containsAll(idSet));

  }

  public void testOidBitsArraySave() throws Exception {
    // wait for background retrieving persistent data
    objectStore.getAllObjectIDs();

    // publish data
    Collection objects = createRandomObjects(15050);
    PersistenceTransaction ptx = persistenceTransactionProvider.newTransaction();
    managedObjectPersistor.saveAllObjects(ptx, objects);
    ptx.commit();

    oidManager.runCheckpoint();

    OidBitsArrayMap oidMap = oidManager.loadBitsArrayFromDisk();
    // verify object IDs is in memory
    for (Iterator i = objects.iterator(); i.hasNext();) {
      ManagedObject mo = (ManagedObject) i.next();
      assertTrue("Object:" + mo.getID() + " missed in memory! ", oidMap.contains(mo.getID()));
    }

    verify(objects);
  }

  public void testOidBitsArrayDeleteHalf() throws Exception {
    // wait for background retrieving persistent data
    objectStore.getAllObjectIDs();

    // publish data
    Collection objects = createRandomObjects(15050);
    PersistenceTransaction ptx = persistenceTransactionProvider.newTransaction();
    managedObjectPersistor.saveAllObjects(ptx, objects);
    ptx.commit();

    oidManager.runCheckpoint();

    int total = objects.size();
    SortedSet<ObjectID> toDelete = new TreeSet<ObjectID>();
    int count = 0;
    for (Iterator i = objects.iterator(); (count < total / 2) && i.hasNext();) {
      ManagedObject mo = (ManagedObject) i.next();
      toDelete.add(mo.getID());
      i.remove();
    }
    ptx = persistenceTransactionProvider.newTransaction();
    managedObjectPersistor.deleteAllObjectsByID(ptx, toDelete);
    ptx.commit();

    oidManager.runCheckpoint();

    oidManager.loadBitsArrayFromDisk();
    verify(objects);
  }

  public void testOidBitsArrayDeleteAll() throws Exception {
    // wait for background retrieving persistent data
    objectStore.getAllObjectIDs();

    // publish data
    Collection objects = createRandomObjects(15050);
    PersistenceTransaction ptx = persistenceTransactionProvider.newTransaction();
    managedObjectPersistor.saveAllObjects(ptx, objects);
    ptx.commit();

    oidManager.runCheckpoint();

    TreeSet<ObjectID> objectIds = new TreeSet<ObjectID>();
    for (Iterator i = objects.iterator(); i.hasNext();) {
      ManagedObject mo = (ManagedObject) i.next();
      objectIds.add(mo.getID());
    }
    ptx = persistenceTransactionProvider.newTransaction();
    managedObjectPersistor.deleteAllObjectsByID(ptx, objectIds);
    ptx.commit();

    oidManager.runCheckpoint();

    objects.clear();
    verify(objects);
  }

}
