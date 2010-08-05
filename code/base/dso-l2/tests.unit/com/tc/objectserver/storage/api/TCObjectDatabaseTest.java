/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.storage.api;

import org.apache.commons.io.FileUtils;

import com.tc.object.ObjectID;
import com.tc.object.config.schema.NewL2DSOConfig;
import com.tc.objectserver.storage.api.DBEnvironment;
import com.tc.objectserver.storage.api.PersistenceTransaction;
import com.tc.objectserver.storage.api.PersistenceTransactionProvider;
import com.tc.objectserver.storage.api.TCObjectDatabase;
import com.tc.objectserver.storage.api.TCDatabaseReturnConstants.Status;
import com.tc.test.TCTestCase;
import com.tc.util.Assert;

import java.io.File;
import java.util.Arrays;
import java.util.Properties;
import java.util.Random;
import java.util.Set;

public class TCObjectDatabaseTest extends TCTestCase {
  private final Random                   random = new Random();
  private File                           dbHome;
  private DBEnvironment                  dbenv;
  private PersistenceTransactionProvider ptp;

  private TCObjectDatabase               database;

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    File dataPath = getTempDirectory();

    dbHome = new File(dataPath.getAbsolutePath(), NewL2DSOConfig.OBJECTDB_DIRNAME);
    dbHome.mkdir();

    dbenv = new DBFactoryForDBUnitTests().createEnvironment(true, dbHome, new Properties());
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

    value1 = getRandomlyFilledByteArray();
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

  public void testGetAllObjectIDs() {
    long[] objectIds = new long[1000];
    for (int i = 0; i < objectIds.length; i++) {
      objectIds[i] = i;
    }
    byte[] value = getRandomlyFilledByteArray();

    for (int i = 0; i < objectIds.length; i++) {
      PersistenceTransaction tx = ptp.newTransaction();
      Status status = database.insert(objectIds[i], value, tx);
      tx.commit();
      Assert.assertEquals(Status.SUCCESS, status);
    }

    PersistenceTransaction tx = ptp.newTransaction();
    Set<ObjectID> objectIDsFeteched = database.getAllObjectIds(tx);

    Assert.assertEquals(objectIds.length, objectIDsFeteched.size());

    for (int i = 0; i < objectIds.length; i++) {
      Assert.assertTrue(objectIDsFeteched.contains(new ObjectID(i)));
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
