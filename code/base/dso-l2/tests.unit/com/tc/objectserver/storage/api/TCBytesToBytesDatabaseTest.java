/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.storage.api;

import org.apache.commons.io.FileUtils;

import com.tc.object.config.schema.NewL2DSOConfig;
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

    dbHome = new File(dataPath.getAbsolutePath(), NewL2DSOConfig.OBJECTDB_DIRNAME);
    dbHome.mkdir();

    dbenv = new DBFactoryForDBUnitTests(new Properties()).createEnvironment(true, dbHome);
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

  private byte[] getRandomlyFilledByteArray() {
    byte[] array = new byte[100];
    random.nextBytes(array);
    return array;
  }

  @Override
  protected void tearDown() throws Exception {
    super.tearDown();
    dbenv.close();
    FileUtils.cleanDirectory(dbHome);
  }
}
