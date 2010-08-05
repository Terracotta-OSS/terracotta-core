/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.storage.api;

import org.apache.commons.io.FileUtils;

import com.tc.object.config.schema.NewL2DSOConfig;
import com.tc.objectserver.persistence.db.TCDatabaseException;
import com.tc.objectserver.storage.api.DBEnvironment;
import com.tc.objectserver.storage.api.PersistenceTransaction;
import com.tc.objectserver.storage.api.PersistenceTransactionProvider;
import com.tc.objectserver.storage.api.TCDatabaseEntry;
import com.tc.objectserver.storage.api.TCMapsDatabase;
import com.tc.objectserver.storage.api.TCMapsDatabaseCursor;
import com.tc.objectserver.storage.api.TCDatabaseReturnConstants.Status;
import com.tc.test.TCTestCase;
import com.tc.util.Assert;
import com.tc.util.Conversion;

import java.io.File;
import java.util.Arrays;
import java.util.Properties;
import java.util.Random;

public class TCMapsDatabaseTest extends TCTestCase {
  private final Random                   random = new Random();
  private File                           dbHome;
  private DBEnvironment                  dbenv;
  private PersistenceTransactionProvider ptp;

  private TCMapsDatabase                 database;

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    File dataPath = getTempDirectory();

    dbHome = new File(dataPath.getAbsolutePath(), NewL2DSOConfig.OBJECTDB_DIRNAME);
    dbHome.mkdir();

    dbenv = new DBFactoryForDBUnitTests().createEnvironment(true, dbHome, new Properties());
    dbenv.open();

    ptp = dbenv.getPersistenceTransactionProvider();
    database = dbenv.getMapsDatabase();
  }

  public void testPutDelete() {
    long objectId = 1;

    byte[] key = getRandomlyFilledByteArray(objectId);
    byte[] value = getRandomlyFilledByteArray(objectId);

    PersistenceTransaction tx = ptp.newTransaction();
    Status status = database.put(objectId, key, value, tx);
    tx.commit();

    Assert.assertEquals(Status.SUCCESS, status);

    tx = ptp.newTransaction();
    TCMapsDatabaseCursor cursor = database.openCursor(tx, objectId);
    int count = 0;
    while (cursor.hasNext()) {
      TCDatabaseEntry<byte[], byte[]> entry = cursor.next();
      Assert.assertTrue(Arrays.equals(key, entry.getKey()));
      Assert.assertTrue(Arrays.equals(value, entry.getValue()));
      count++;
    }
    cursor.close();
    tx.commit();

    Assert.assertEquals(1, count);

    tx = ptp.newTransaction();
    status = database.delete(objectId, key, tx);
    tx.commit();

    Assert.assertEquals(Status.SUCCESS, status);

    Assert.assertEquals(0, database.count());

    count = 0;
    tx = ptp.newTransaction();
    cursor = database.openCursor(tx, objectId);
    while (cursor.hasNext()) {
      count++;
    }
    cursor.close();
    tx.commit();
    Assert.assertEquals(0, count);
  }

  public void testDeleteCollections() throws TCDatabaseException {
    long objectId1 = 1;
    byte[] key1 = getRandomlyFilledByteArray(objectId1);
    byte[] value1 = getRandomlyFilledByteArray(objectId1);

    long objectId2 = 2;
    byte[] key2 = getRandomlyFilledByteArray(objectId2);
    byte[] value2 = getRandomlyFilledByteArray(objectId2);

    PersistenceTransaction tx = ptp.newTransaction();
    database.put(objectId1, key1, value1, tx);
    database.put(objectId2, key2, value2, tx);
    tx.commit();

    Assert.assertEquals(2, database.count());

    tx = ptp.newTransaction();
    database.deleteCollection(objectId1, tx);
    tx.commit();

    Assert.assertEquals(1, database.count());

    tx = ptp.newTransaction();
    TCMapsDatabaseCursor cursor = database.openCursor(tx, objectId2);
    int count = 0;
    while (cursor.hasNext()) {
      TCDatabaseEntry<byte[], byte[]> entry = cursor.next();
      Assert.assertTrue(Arrays.equals(key2, entry.getKey()));
      Assert.assertTrue(Arrays.equals(value2, entry.getValue()));
      count++;
    }
    cursor.close();
    tx.commit();

    Assert.assertEquals(1, count);
  }

  private byte[] getRandomlyFilledByteArray(long objectId) {
    byte[] array = new byte[108];
    random.nextBytes(array);

    byte[] temp = Conversion.long2Bytes(objectId);
    for (int i = 0; i < temp.length; i++) {
      array[i] = temp[i];
    }
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
