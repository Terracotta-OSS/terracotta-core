/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.impl;

import com.sleepycat.je.CursorConfig;
import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.object.ObjectID;
import com.tc.objectserver.core.api.ManagedObject;
import com.tc.objectserver.core.impl.TestManagedObject;
import com.tc.objectserver.persistence.api.PersistenceTransaction;
import com.tc.objectserver.persistence.api.PersistenceTransactionProvider;
import com.tc.objectserver.persistence.impl.TestMutableSequence;
import com.tc.objectserver.persistence.impl.TestPersistenceTransactionProvider;
import com.tc.objectserver.persistence.sleepycat.DBEnvironment;
import com.tc.objectserver.persistence.sleepycat.ManagedObjectPersistorImpl;
import com.tc.objectserver.persistence.sleepycat.SleepycatCollectionFactory;
import com.tc.objectserver.persistence.sleepycat.SleepycatCollectionsPersistor;
import com.tc.objectserver.persistence.sleepycat.SleepycatSerializationAdapterFactory;
import com.tc.test.TCTestCase;
import com.tc.util.SyncObjectIdSet;

import java.io.File;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Random;

public class ManagedObjectPersistorImplTest extends TCTestCase {
  private static final TCLogger        logger = TCLogging.getTestingLogger(ManagedObjectPersistorImplTest.class);
  private ManagedObjectPersistorImpl   managedObjectPersistor;
  private Map                          map;
  private TestManagedObjectPersistor   persistor;
  private PersistentManagedObjectStore objectStore;
  PersistenceTransactionProvider persistenceTransactionProvider;
  DBEnvironment env;
  
  protected void setUp() throws Exception {
    super.setUp();
    boolean paranoid = false;
    env = newDBEnvironment(paranoid);
    env.open();
    CursorConfig dbCursorConfig = new CursorConfig();
    persistenceTransactionProvider = new TestPersistenceTransactionProvider();
    CursorConfig rootDBCursorConfig = new CursorConfig();
    SleepycatCollectionFactory sleepycatCollectionFactory = new SleepycatCollectionFactory();
    SleepycatCollectionsPersistor sleepycatCollectionsPersistor = new SleepycatCollectionsPersistor(logger, env.getMapsDatabase(),
                                                                           sleepycatCollectionFactory);
    managedObjectPersistor = new ManagedObjectPersistorImpl(logger,
                                                            env.getClassCatalogWrapper().getClassCatalog(),
                                                            new SleepycatSerializationAdapterFactory(),
                                                            env.getObjectDatabase(),
                                                            env.getOidDatabase(),
                                                            dbCursorConfig,
                                                            new TestMutableSequence(),
                                                            env.getRootDatabase(),
                                                            rootDBCursorConfig,
                                                            persistenceTransactionProvider,
                                                            sleepycatCollectionsPersistor
                                                            );
    map = new HashMap();
    persistor = new TestManagedObjectPersistor(map);
    objectStore = new PersistentManagedObjectStore(managedObjectPersistor);
  }
  
  protected void tearDown() throws Exception {
    env.close();
    super.tearDown();
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
    DBEnvironment env = new DBEnvironment(paranoid, dbHome);
    return env;
  }

  private Collection createRandomObjects(int num) {
    Random r = new Random();
    HashSet objects = new HashSet(num);
    HashSet ids = new HashSet(num);
    for (int i = 0; i < num; i++) {
      long id = (long)r.nextInt(num * 10) + 1;
      if (ids.add(new Long(id))) {
        ManagedObject mo = new TestManagedObject(new ObjectID(id), new ObjectID[] {});
        objects.add(mo);
      }   
    }
    logger.info("Test with "+ objects.size()+ " objects");
    return (objects);
  }
  
  private void verify(Collection objects) {
    // verify a in-memory bit crosspond to an object ID
    HashSet originalIds = new HashSet();
    for(Iterator i = objects.iterator(); i.hasNext(); ) {
      ManagedObject mo = (ManagedObject) i.next();
      originalIds.add(mo.getID());
    }
    Collection inMemoryIds = managedObjectPersistor.bitsArrayMapToObjectID();
    assertTrue("Wrong bits in memory were set",
               originalIds.containsAll(inMemoryIds));
    
    // verify on disk object IDs
    // clear in memory arrays then read in from persistor
    managedObjectPersistor.resetBitsArrayMap();    
    SyncObjectIdSet idSet = managedObjectPersistor.getAllObjectIDs();
    idSet.snapshot(); // blocked while reading from disk
    Collection diskIds = managedObjectPersistor.bitsArrayMapToObjectID();
    assertTrue("Wrong object IDs on disk",
               diskIds.equals(inMemoryIds));

  }
  
  public void testOidBitsArraySave() throws Exception {
    // publish data
    Collection objects = createRandomObjects(15050);
    PersistenceTransaction ptx = persistenceTransactionProvider.newTransaction();
    managedObjectPersistor.saveAllObjects(ptx, objects);
    ptx.commit();
    
    //verify object IDs is in memory
    for(Iterator i = objects.iterator(); i.hasNext(); ) {
      ManagedObject mo = (ManagedObject) i.next();
      assertTrue("Object:"+mo.getID()+" missed in memory! ", 
                 managedObjectPersistor.inMemoryContains(mo.getID()));
    }
    
    verify(objects);
   }
  
  public void testOidBitsArrayDeleteHalf() throws Exception {
    // publish data
    Collection objects = createRandomObjects(15050);
    PersistenceTransaction ptx = persistenceTransactionProvider.newTransaction();
    managedObjectPersistor.saveAllObjects(ptx, objects);
    ptx.commit();

    int total = objects.size();
    HashSet toDelete = new HashSet();
    int count = 0;
    for(Iterator i = objects.iterator();(count < total/2) && i.hasNext();) {
      ManagedObject mo = (ManagedObject)i.next();
      toDelete.add(mo.getID());
      i.remove();
    }
    ptx = persistenceTransactionProvider.newTransaction();
    managedObjectPersistor.deleteAllObjectsByID(ptx, toDelete);
    ptx.commit();
    
    verify(objects);
  }
  
  public void testOidBitsArrayDeleteAll() throws Exception {
    // publish data
    Collection objects = createRandomObjects(15050);
    PersistenceTransaction ptx = persistenceTransactionProvider.newTransaction();
    managedObjectPersistor.saveAllObjects(ptx, objects);
    ptx.commit();
    
    HashSet objectIds = new HashSet();
    for(Iterator i = objects.iterator(); i.hasNext(); ) {
      ManagedObject mo = (ManagedObject) i.next();
      objectIds.add(mo.getID());
    }
    ptx = persistenceTransactionProvider.newTransaction();
    managedObjectPersistor.deleteAllObjectsByID(ptx, objectIds);
    ptx.commit();
    
    objects.clear();
    verify(objects);
  }

 }
