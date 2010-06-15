/*
 * All content copyright (c) 20032008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.
 * All rights reserved.
 */
package com.tc.objectserver.persistence.sleepycat;

import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.object.ObjectID;
import com.tc.object.TestDNACursor;
import com.tc.objectserver.managedobject.ManagedObjectStateFactory;
import com.tc.objectserver.managedobject.MapManagedObjectState;
import com.tc.objectserver.managedobject.NullManagedObjectChangeListenerProvider;
import com.tc.objectserver.persistence.api.PersistenceTransaction;
import com.tc.objectserver.persistence.api.PersistenceTransactionProvider;
import com.tc.test.TCTestCase;
import com.tc.util.Assert;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.Random;

public class SleepycatCollectionsDeleteTest extends TCTestCase {

  private SleepycatPersistor             persistor;
  private PersistenceTransactionProvider ptp;
  private DBEnvironment                  env;
  private SleepycatCollectionsPersistor  collectionsPersistor;
  private static int                     dbHomeCounter = 0;
  private static File                    tempDirectory;

  @Override
  public void setUp() throws Exception {

    if (this.env != null) {
      this.env.close();
    }
    final File dbHome = newDBHome();
    final TCLogger logger = TCLogging.getLogger(getClass());
    final CustomSerializationAdapterFactory saf = new CustomSerializationAdapterFactory();
    this.env = new DBEnvironment(true, dbHome);
    this.persistor = new SleepycatPersistor(logger, this.env, saf);
    this.ptp = this.persistor.getPersistenceTransactionProvider();
    this.collectionsPersistor = this.persistor.getCollectionsPersistor();
  }

  // XXX:: Check SleepycatSerializationTest if you want know why its done like this or ask Orion.
  private File newDBHome() throws IOException {
    File file;
    if (tempDirectory == null) {
      tempDirectory = getTempDirectory();
    }
    ++dbHomeCounter;
    for (file = new File(tempDirectory, "db" + dbHomeCounter); file.exists(); ++dbHomeCounter) {
      //
    }
    assertFalse(file.exists());
    System.err.println("DB Home = " + file);
    return file;
  }

  @Override
  public void tearDown() throws Exception {
    this.persistor = null;
    this.ptp = null;
    this.env = null;
  }
  
  public void testDeleteMap() throws Exception {
    ManagedObjectStateFactory.createInstance(new NullManagedObjectChangeListenerProvider(), this.persistor);
    final ObjectID id1 = new ObjectID(1);
    final MapManagedObjectState state1 = (MapManagedObjectState) ManagedObjectStateFactory.getInstance()
        .createState(id1, ObjectID.NULL_ID, "java.util.HashMap", "System.loader", new TestDNACursor());
    final SleepycatPersistableMap sMap1 = (SleepycatPersistableMap) state1.getPersistentCollection();

    Random rand = new Random();
    final int map1Entries = rand.nextInt(200000);
    System.out.println("XXX map 1 entries: " + map1Entries);
    addToMap(sMap1, map1Entries);
    Assert.assertEquals(0, this.env.getMapsDatabase().count());
    PersistenceTransaction tx = this.ptp.newTransaction();
    this.collectionsPersistor.saveCollections(tx, state1);
    tx.commit();
    Assert.assertEquals(map1Entries, this.env.getMapsDatabase().count());
    
    final ObjectID id2 = new ObjectID(100);
    final MapManagedObjectState state2 = (MapManagedObjectState) ManagedObjectStateFactory.getInstance()
    .createState(id2, ObjectID.NULL_ID, "java.util.HashMap", "System.loader", new TestDNACursor());
    final SleepycatPersistableMap sMap2 = (SleepycatPersistableMap) state2.getPersistentCollection();
    
    final int map2Entries = rand.nextInt(200000);
    System.out.println("XXX map 2 entries: " + map2Entries);
    addToMap(sMap2, map2Entries);
    tx = this.ptp.newTransaction();
    this.collectionsPersistor.saveCollections(tx, state2);
    tx.commit();
    Assert.assertEquals(map1Entries + map2Entries, this.env.getMapsDatabase().count());
    
    int objectsDeleted = this.collectionsPersistor.deleteCollection(this.ptp, id1);
    Assert.assertEquals(map1Entries, objectsDeleted);
    Assert.assertEquals(map2Entries, this.env.getMapsDatabase().count());

    objectsDeleted = this.collectionsPersistor.deleteCollection(this.ptp, id2);
    Assert.assertEquals(map2Entries, objectsDeleted);
    Assert.assertEquals(0, this.env.getMapsDatabase().count());
  }

  private void addToMap(final Map map, final int numOfEntries) {
    for (int i = 50; i < numOfEntries + 50; i++) {
      map.put(new ObjectID(i), new Integer(i));
    }
  }

}
