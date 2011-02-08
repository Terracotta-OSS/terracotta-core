/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.storage.api;

import org.apache.commons.io.FileUtils;

import com.tc.object.ObjectID;
import com.tc.object.config.schema.L2DSOConfig;
import com.tc.objectserver.storage.api.TCDatabaseReturnConstants.Status;
import com.tc.test.TCTestCase;
import com.tc.util.Assert;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Random;
import java.util.Set;
import java.util.Map.Entry;

public class TCRootDatabaseTest extends TCTestCase {
  private final Random                   random = new Random();
  private File                           dbHome;
  private DBEnvironment                  dbenv;
  private PersistenceTransactionProvider ptp;

  private TCRootDatabase                 database;

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    File dataPath = getTempDirectory();

    dbHome = new File(dataPath.getAbsolutePath(), L2DSOConfig.OBJECTDB_DIRNAME);
    dbHome.mkdir();

    dbenv = new DBFactoryForDBUnitTests(new Properties()).createEnvironment(true, dbHome, null, false);
    dbenv.open();

    ptp = dbenv.getPersistenceTransactionProvider();
    database = dbenv.getRootDatabase();
  }

  public void testGetPut() {
    long objectId = 1;
    byte[] key = getRandomlyFilledByteArray();

    PersistenceTransaction tx = ptp.newTransaction();
    Status status = database.put(key, objectId, tx);
    tx.commit();
    Assert.assertEquals(Status.SUCCESS, status);

    tx = ptp.newTransaction();
    long objectIdFetched = database.getIdFromName(key, tx);
    tx.commit();

    Assert.assertEquals(objectId, objectIdFetched);
  }

  public void testGetsMethods() {
    byte[][] keys = new byte[5][];
    for (int i = 0; i < keys.length; i++) {
      keys[i] = getRandomlyFilledByteArray();
    }

    for (int i = 0; i < keys.length; i++) {
      PersistenceTransaction tx = ptp.newTransaction();
      Status status = database.put(keys[i], i, tx);
      tx.commit();
      Assert.assertEquals(Status.SUCCESS, status);
    }

    PersistenceTransaction tx = ptp.newTransaction();
    Set<ObjectID> rootIds = database.getRootIds(tx);
    Assert.assertEquals(keys.length, rootIds.size());
    for (int i = 0; i < keys.length; i++) {
      Assert.assertTrue(rootIds.contains(new ObjectID(i)));
    }

    tx = ptp.newTransaction();
    Map<byte[], Long> rootNamesToIds = database.getRootNamesToId(tx);
    Assert.assertEquals(keys.length, rootNamesToIds.size());
    for (Entry<byte[], Long> entry : rootNamesToIds.entrySet()) {
      boolean found = false;
      for (int i = 0; i < keys.length; i++) {
        if (Arrays.equals(entry.getKey(), keys[i])) {
          Assert.assertEquals(i, entry.getValue().intValue());
          found = true;
        }
      }
      Assert.assertTrue(found);
    }

    tx = ptp.newTransaction();
    List<byte[]> rootNames = database.getRootNames(tx);
    Assert.assertEquals(keys.length, rootNames.size());
    for (byte[] byteValue : rootNames) {
      boolean found = false;
      for (byte[] key : keys) {
        if (Arrays.equals(byteValue, key)) {
          found = true;
        }
      }
      Assert.assertTrue(found);
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
