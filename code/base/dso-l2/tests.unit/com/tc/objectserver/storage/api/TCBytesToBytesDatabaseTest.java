/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.storage.api;

import org.apache.commons.io.FileUtils;

import com.tc.object.config.schema.L2DSOConfig;
import com.tc.objectserver.persistence.db.TCDatabaseException;
import com.tc.objectserver.storage.api.TCDatabaseReturnConstants.Status;
import com.tc.test.TCTestCase;
import com.tc.util.Assert;

import java.io.File;
import java.util.Arrays;
import java.util.Properties;
import java.util.Random;

public class TCBytesToBytesDatabaseTest extends TCTestCase {
  private final Random                   random = new Random();
  private File                           dbHome;
  private DBEnvironment                  dbenv;
  private PersistenceTransactionProvider ptp;

  private TCBytesToBytesDatabase         database;

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    File dataPath = getTempDirectory();

    dbHome = new File(dataPath.getAbsolutePath(), L2DSOConfig.OBJECTDB_DIRNAME);
    dbHome.mkdir();

    dbenv = new DBFactoryForDBUnitTests(new Properties()).createEnvironment(true, dbHome, null, false);
    dbenv.open();

    ptp = dbenv.getPersistenceTransactionProvider();
    database = dbenv.getObjectOidStoreDatabase();
  }

  public void testPutGet() {
    byte[] key = getRandomlyFilledByteArray();
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

  public void testPutNoOverwrite() {
    byte[] key = getRandomlyFilledByteArray();
    byte[] value1 = getRandomlyFilledByteArray();

    PersistenceTransaction tx = ptp.newTransaction();
    Status status = database.putNoOverwrite(tx, key, value1);
    tx.commit();

    Assert.assertEquals(Status.SUCCESS, status);

    byte[] value2 = getRandomlyFilledByteArray();
    tx = ptp.newTransaction();
    status = database.putNoOverwrite(tx, key, value2);
    tx.commit();

    Assert.assertEquals(Status.NOT_SUCCESS, status);

    tx = ptp.newTransaction();
    byte[] valueReturned = database.get(key, tx);
    tx.commit();

    Assert.assertTrue(Arrays.equals(value1, valueReturned));
  }

  public void testDelete() {
    byte[] key = getRandomlyFilledByteArray();
    byte[] value = getRandomlyFilledByteArray();

    PersistenceTransaction tx = ptp.newTransaction();
    Status status = database.put(key, value, tx);
    tx.commit();

    Assert.assertEquals(Status.SUCCESS, status);

    tx = ptp.newTransaction();
    byte[] valueReturned = database.get(key, tx);
    tx.commit();

    Assert.assertTrue(Arrays.equals(value, valueReturned));

    tx = ptp.newTransaction();
    status = database.delete(key, tx);
    tx.commit();

    Assert.assertEquals(Status.SUCCESS, status);

    tx = ptp.newTransaction();
    valueReturned = database.get(key, tx);
    tx.commit();

    Assert.assertNull(valueReturned);
  }

  public void testCursorWithConcurrentRead() {
    byte[][] keys = { getRandomlyFilledByteArray(), getRandomlyFilledByteArray(), getRandomlyFilledByteArray(),
        getRandomlyFilledByteArray() };
    byte[][] values = { getRandomlyFilledByteArray(), getRandomlyFilledByteArray(), getRandomlyFilledByteArray(),
        getRandomlyFilledByteArray() };

    byte[] key1 = getRandomlyFilledByteArray();
    byte[] value1 = getRandomlyFilledByteArray();

    TCBytesToBytesDatabase bytesToBytesDatabase = null;
    try {
      bytesToBytesDatabase = dbenv.getObjectOidStoreDatabase();
    } catch (TCDatabaseException e) {
      throw new AssertionError(e);
    }

    PersistenceTransaction tx = ptp.newTransaction();
    for (int i = 0; i < keys.length; i++) {
      byte[] key = keys[i];
      byte[] value = values[i];
      database.put(key, value, tx);
    }
    bytesToBytesDatabase.put(key1, value1, tx);
    tx.commit();

    tx = ptp.newTransaction();
    TCDatabaseCursor<byte[], byte[]> cursor = database.openCursorUpdatable(tx);
    int count = 0;
    while (cursor.hasNext()) {
      cursor.next();
      // get
      bytesToBytesDatabase.get(key1, tx);
      cursor.delete();
      count++;
    }
    cursor.close();
    tx.commit();

    tx = ptp.newTransaction();
    cursor = database.openCursorUpdatable(tx);
    Assert.assertFalse(cursor.hasNext());
    cursor.close();
    tx.commit();
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
