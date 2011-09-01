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
import java.util.Random;

public class TCLongToBytesDatabaseTest extends TCTestCase {
  private final Random                   random = new Random();
  private File                           dbHome;
  private DBEnvironment                  dbenv;
  private PersistenceTransactionProvider ptp;

  private TCLongToBytesDatabase          database;

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    File dataPath = getTempDirectory();

    dbHome = new File(dataPath.getAbsolutePath(), L2DSOConfig.OBJECTDB_DIRNAME);
    dbHome.mkdir();

    dbenv = DBFactory.getInstance().createEnvironment(true, dbHome, null, false);
    dbenv.open();

    ptp = dbenv.getPersistenceTransactionProvider();
    database = dbenv.getObjectDatabase();
  }

  public void testInsertUpdateGet() {
    long objectId1 = 1;
    byte[] value1 = getRandomlyFilledByteArray();
    byte[] value2 = getRandomlyFilledByteArray();

    PersistenceTransaction tx = ptp.newTransaction();
    Status status = database.insert(objectId1, value1, tx);
    tx.commit();
    Assert.assertEquals(Status.SUCCESS, status);

    tx = ptp.newTransaction();
    byte[] valueFetched = database.get(objectId1, tx);
    tx.commit();
    Assert.assertTrue(Arrays.equals(value1, valueFetched));

    tx = ptp.newTransaction();
    status = database.update(objectId1, value2, tx);
    tx.commit();

    Assert.assertEquals(Status.SUCCESS, status);

    tx = ptp.newTransaction();
    valueFetched = database.get(objectId1, tx);
    tx.commit();
    Assert.assertTrue(Arrays.equals(value2, valueFetched));
  }

  public void testDelete() {
    long objectId1 = 1;
    byte[] value1 = getRandomlyFilledByteArray();

    PersistenceTransaction tx = ptp.newTransaction();
    Status status = database.insert(objectId1, value1, tx);
    tx.commit();
    Assert.assertEquals(Status.SUCCESS, status);

    tx = ptp.newTransaction();
    status = database.delete(objectId1, tx);
    tx.commit();
    Assert.assertEquals(Status.SUCCESS, status);

    tx = ptp.newTransaction();
    status = database.delete(objectId1, tx);
    tx.commit();
    Assert.assertEquals(Status.NOT_FOUND, status);
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
