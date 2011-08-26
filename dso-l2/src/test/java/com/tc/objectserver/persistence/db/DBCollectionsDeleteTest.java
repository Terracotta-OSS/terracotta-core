/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.persistence.db;

import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.object.ObjectID;
import com.tc.object.TestDNACursor;
import com.tc.objectserver.managedobject.ManagedObjectStateFactory;
import com.tc.objectserver.managedobject.MapManagedObjectState;
import com.tc.objectserver.managedobject.NullManagedObjectChangeListenerProvider;
import com.tc.objectserver.storage.api.DBEnvironment;
import com.tc.objectserver.storage.api.PersistenceTransaction;
import com.tc.objectserver.storage.api.PersistenceTransactionProvider;
import com.tc.objectserver.storage.berkeleydb.BerkeleyDBEnvironment;
import com.tc.properties.TCPropertiesConsts;
import com.tc.properties.TCPropertiesImpl;
import com.tc.test.TCTestCase;
import com.tc.util.Assert;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.Random;
import java.util.SortedSet;
import java.util.TreeSet;

public class DBCollectionsDeleteTest extends TCTestCase {

  private DBPersistorImpl                persistor;
  private PersistenceTransactionProvider ptp;
  private DBEnvironment                  env;
  private TCCollectionsPersistor         collectionsPersistor;
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
    this.env = new BerkeleyDBEnvironment(true, dbHome);
    this.persistor = new DBPersistorImpl(logger, this.env, saf);
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
    for (int i = 0; i < 10; i++) {
      final ObjectID id = new ObjectID(i);
      final MapManagedObjectState state = (MapManagedObjectState) ManagedObjectStateFactory.getInstance()
          .createState(id, ObjectID.NULL_ID, "java.util.HashMap", "System.loader", new TestDNACursor());
      final TCPersistableMap sMap = (TCPersistableMap) state.getPersistentCollection();
      int entries = 0;
      if (i == 0) {
        entries = TCPropertiesImpl.getProperties().getInt(TCPropertiesConsts.L2_OBJECTMANAGER_DELETEBATCHSIZE) - 1;
      } else if (i == 1) {
        entries = TCPropertiesImpl.getProperties().getInt(TCPropertiesConsts.L2_OBJECTMANAGER_DELETEBATCHSIZE) + 100;
      } else {
        entries = rand.nextInt(200000);
      }
      System.out.println("XXX added entries:" + entries);
      addToMap(sMap, entries);
      Assert.assertEquals(totalEntries, this.env.getMapsDatabase().count(null));
      totalEntries += entries;
      PersistenceTransaction tx = this.ptp.newTransaction();
      this.collectionsPersistor.saveCollections(tx, state);
      tx.commit();
      Assert.assertEquals(totalEntries, this.env.getMapsDatabase().count(null));
      deleteIds.add(id);
    }

    System.out.println("XXX total entries: " + totalEntries);
    long start = System.currentTimeMillis();
    long objectsDeleted = this.collectionsPersistor.deleteAllCollections(ptp, deleteIds, deleteIds);
    System.out.println("time taken to delete " + (System.currentTimeMillis() - start) + "ms");
    Assert.assertEquals(totalEntries, objectsDeleted);
    Assert.assertEquals(0, this.env.getMapsDatabase().count(null));

  }

  private void addToMap(final Map map, final int numOfEntries) {
    for (int i = 50; i < numOfEntries + 50; i++) {
      map.put(new ObjectID(i), Integer.valueOf(i));
    }
  }

}
