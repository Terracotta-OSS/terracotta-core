/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.storage.api;

import com.tc.object.config.schema.L2DSOConfig;
import com.tc.test.TCTestCase;

import java.io.File;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class TCTransactionStoreDatabaseTest extends TCTestCase {
  private final Random                   random = new Random();
  private File                           dbHome;
  private DBEnvironment                  dbenv;
  private PersistenceTransactionProvider ptp;

  private TCTransactionStoreDatabase     database;

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    File dataPath = getTempDirectory();

    dbHome = new File(dataPath.getAbsolutePath(), L2DSOConfig.OBJECTDB_DIRNAME);
    dbHome.mkdir();

    dbenv = DBFactory.getInstance().createEnvironment(true, dbHome, null, false);
    dbenv.open();

    ptp = dbenv.getPersistenceTransactionProvider();
    database = dbenv.getTransactionDatabase();
  }

  public void testCursor() throws Exception {
    Map<Long, byte[]> objects = new HashMap<Long, byte[]>();
    PersistenceTransaction tx = ptp.newTransaction();
    for (long i = 0; i < 100; i++) {
      objects.put(i, getRandomlyFilledByteArray());
      database.insert(i, objects.get(i), tx);
    }
    tx.commit();

    tx = ptp.newTransaction();
    TCDatabaseCursor<Long, byte[]> cursor = database.openCursor(tx);
    while (cursor.hasNext()) {
      TCDatabaseEntry<Long, byte[]> entry = cursor.next();
      byte[] value = objects.remove(entry.getKey());
      assertNotNull(value);
      assertTrue(Arrays.equals(value, entry.getValue()));
    }
    cursor.close();
    tx.commit();

    assertTrue(objects.isEmpty());

  }

  private byte[] getRandomlyFilledByteArray() {
    byte[] array = new byte[100];
    random.nextBytes(array);
    return array;
  }
}
