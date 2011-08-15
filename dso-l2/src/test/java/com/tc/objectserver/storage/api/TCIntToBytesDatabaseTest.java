/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.storage.api;

import org.apache.commons.io.FileUtils;

import com.tc.object.config.schema.L2DSOConfig;
import com.tc.objectserver.storage.api.TCDatabaseReturnConstants.Status;
import com.tc.test.TCTestCase;
import com.tc.util.Assert;

import java.io.File;
import java.util.Arrays;
import java.util.Map;
import java.util.Properties;
import java.util.Random;
import java.util.Map.Entry;

public class TCIntToBytesDatabaseTest extends TCTestCase {
  private final Random                   random = new Random();
  private File                           dbHome;
  private DBEnvironment                  dbenv;
  private PersistenceTransactionProvider ptp;

  private TCIntToBytesDatabase           database;

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    File dataPath = getTempDirectory();

    dbHome = new File(dataPath.getAbsolutePath(), L2DSOConfig.OBJECTDB_DIRNAME);
    dbHome.mkdir();

    dbenv = new DBFactoryForDBUnitTests(new Properties()).createEnvironment(true, dbHome, null, false);
    dbenv.open();

    ptp = dbenv.getPersistenceTransactionProvider();
    database = dbenv.getClassDatabase();
  }

  public void testPutGet() {
    int key = random.nextInt(100);
    byte[] value = getRandomlyFilledByteArray();

    PersistenceTransaction tx = ptp.newTransaction();
    Status status = database.put(key, value, tx);
    tx.commit();

    Assert.assertEquals(Status.SUCCESS, status);

    tx = ptp.newTransaction();
    byte[] valueReturned = database.get(key, tx);
    tx.commit();

    Assert.assertTrue(Arrays.equals(value, valueReturned));
  }

  public void testGetAll() {
    int[] keys = { 0, 1, 2, 3, 4, 5 };
    byte[][] values = new byte[keys.length][100];

    for (int i = 0; i < keys.length; i++) {
      values[i] = getRandomlyFilledByteArray();
    }

    for (int i = 0; i < keys.length; i++) {
      PersistenceTransaction tx = ptp.newTransaction();
      Status status = database.put(keys[i], values[i], tx);
      tx.commit();

      Assert.assertEquals(Status.SUCCESS, status);
    }

    PersistenceTransaction tx = ptp.newTransaction();
    Map<Integer, byte[]> keyValuesFetched = database.getAll(tx);

    Assert.assertEquals(keys.length, keyValuesFetched.size());

    for (Entry<Integer, byte[]> entry : keyValuesFetched.entrySet()) {
      int index = entry.getKey().intValue();
      Assert.assertTrue(index < keys.length && index > -1);
      Assert.assertTrue(Arrays.equals(values[index], entry.getValue()));
    }
  }

  private byte[] getRandomlyFilledByteArray() {
    byte[] array = new byte[100];
    random.nextBytes(array);
    return array;
  }

  @Override
  protected void tearDown() throws Exception {
    super.tearDown();
    try {
      dbenv.close();
      FileUtils.cleanDirectory(dbHome);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
}
