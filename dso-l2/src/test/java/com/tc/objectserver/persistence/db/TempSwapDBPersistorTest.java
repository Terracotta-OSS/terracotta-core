/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.persistence.db;

import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.objectserver.persistence.inmemory.InMemoryClientStatePersistor;
import com.tc.objectserver.persistence.inmemory.InMemoryPersistentMapStore;
import com.tc.objectserver.persistence.inmemory.InMemorySequenceProvider;
import com.tc.objectserver.persistence.inmemory.NullPersistenceTransactionProvider;
import com.tc.objectserver.persistence.inmemory.NullTransactionPersistor;
import com.tc.objectserver.storage.berkeleydb.BerkeleyDBEnvironment;
import com.tc.test.TCTestCase;
import com.tc.util.Assert;

import java.io.File;

public class TempSwapDBPersistorTest extends TCTestCase {

  public void testBasic() throws Exception {
    TCLogger logger = TCLogging.getLogger(getClass());
    BerkeleyDBEnvironment dbEnv = newDBEnvironment(false);
    SerializationAdapterFactory saf = new CustomSerializationAdapterFactory();

    TempSwapDBPersistorImpl persistor = new TempSwapDBPersistorImpl(logger, dbEnv, saf);

    Assert.assertTrue(persistor.getPersistenceTransactionProvider() instanceof NullPersistenceTransactionProvider);
    Assert.assertTrue(persistor.getClientStatePersistor() instanceof InMemoryClientStatePersistor);
    Assert.assertTrue(persistor.getPersistentStateStore() instanceof InMemoryPersistentMapStore);
    Assert.assertTrue(persistor.getTransactionPersistor() instanceof NullTransactionPersistor);
    Assert.assertTrue(persistor.getGlobalTransactionIDSequence() instanceof InMemorySequenceProvider);

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

}
