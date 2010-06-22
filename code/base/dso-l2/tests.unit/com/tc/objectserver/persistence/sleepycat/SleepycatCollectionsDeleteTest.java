/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
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
import java.util.SortedSet;
import java.util.TreeSet;

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

    Random rand = new Random();
    SortedSet<ObjectID> deleteIds = new TreeSet<ObjectID>();
    int totalEntries = 0;
    for(int i = 0; i < 10; i++){
      final ObjectID id = new ObjectID(i);
      final MapManagedObjectState state = (MapManagedObjectState) ManagedObjectStateFactory.getInstance()
      .createState(id, ObjectID.NULL_ID, "java.util.HashMap", "System.loader", new TestDNACursor());
      final SleepycatPersistableMap sMap = (SleepycatPersistableMap) state.getPersistentCollection();
      int entries = rand.nextInt(200000);
      System.out.println("XXX added entries:" + entries);
      addToMap(sMap, entries);
      Assert.assertEquals(totalEntries, this.env.getMapsDatabase().count());
      totalEntries += entries;
      PersistenceTransaction tx = this.ptp.newTransaction();
      this.collectionsPersistor.saveCollections(tx, state);
      tx.commit();
      Assert.assertEquals(totalEntries, this.env.getMapsDatabase().count());
      deleteIds.add(id);
    }

    System.out.println("XXX total entries: " + totalEntries);
    long start = System.currentTimeMillis();
    int objectsDeleted = this.collectionsPersistor.deleteAllCollections(ptp, deleteIds);
    System.out.println("time taken to delete " + (System.currentTimeMillis() - start) + "ms");
    Assert.assertEquals(totalEntries, objectsDeleted);
    Assert.assertEquals(0, this.env.getMapsDatabase().count());

  }

  private void addToMap(final Map map, final int numOfEntries) {
    for (int i = 50; i < numOfEntries + 50; i++) {
      map.put(new ObjectID(i), new Integer(i));
    }
  }

}
