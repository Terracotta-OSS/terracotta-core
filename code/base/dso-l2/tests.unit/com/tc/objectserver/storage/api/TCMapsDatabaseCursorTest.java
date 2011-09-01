/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.storage.api;

import org.apache.commons.io.FileUtils;

import com.tc.object.config.schema.L2DSOConfig;
import com.tc.objectserver.persistence.db.BatchedTransactionImpl;
import com.tc.objectserver.persistence.db.TCCollectionsSerializerImpl;
import com.tc.test.TCTestCase;
import com.tc.util.Assert;
import com.tc.util.Conversion;

import java.io.File;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.Random;

public class TCMapsDatabaseCursorTest extends TCTestCase {
  private final Random                   random = new Random();
  private File                           dbHome;
  private DBEnvironment                  dbenv;
  private PersistenceTransactionProvider ptp;

  private TCMapsDatabase                 database;

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    File dataPath = getTempDirectory();

    dbHome = new File(dataPath.getAbsolutePath(), L2DSOConfig.OBJECTDB_DIRNAME);
    dbHome.mkdir();

    dbenv = DBFactory.getInstance().createEnvironment(true, dbHome, null, false);
    dbenv.open();

    ptp = dbenv.getPersistenceTransactionProvider();
    database = dbenv.getMapsDatabase();
  }

  public void testBasicCursor() throws Exception {
    TCCollectionsSerializerImpl serializer = new TCCollectionsSerializerImpl();
    long objectId1 = 1;
    byte[] key1 = getRandomlyFilledByteArray(objectId1);
    byte[] value1 = getRandomlyFilledByteArray(objectId1);

    long objectId2 = 2;
    byte[] key2 = getRandomlyFilledByteArray(objectId2);
    byte[] value2 = getRandomlyFilledByteArray(objectId2);

    PersistenceTransaction tx = ptp.newTransaction();
    database.insert(tx, objectId1, key1, value1, serializer);
    database.insert(tx, objectId2, key2, value2, serializer);
    tx.commit();

    Assert.assertEquals(2, database.count(ptp.newTransaction()));

    tx = ptp.newTransaction();
    HashMap<byte[], byte[]> map = new HashMap<byte[], byte[]>();
    database.loadMap(tx, objectId1, map, serializer);
    tx.commit();
    int count = 0;
    for (Entry<byte[], byte[]> entry : map.entrySet()) {
      Assert.assertTrue(Arrays.equals(key1, entry.getKey()));
      Assert.assertTrue(Arrays.equals(value1, entry.getValue()));
      count++;
    }

    Assert.assertEquals(1, count);
  }

  public void testCursorDelete() throws Exception {
    TCCollectionsSerializerImpl serializer = new TCCollectionsSerializerImpl();

    long objectId1 = 1;
    byte[] key1 = getRandomlyFilledByteArray(objectId1);
    byte[] value1 = getRandomlyFilledByteArray(objectId1);

    long objectId2 = 2;
    byte[] key2 = getRandomlyFilledByteArray(objectId2);
    byte[] value2 = getRandomlyFilledByteArray(objectId2);

    PersistenceTransaction tx = ptp.newTransaction();
    database.insert(tx, objectId1, key1, value1, serializer);
    database.insert(tx, objectId2, key2, value2, serializer);
    tx.commit();

    Assert.assertEquals(2, database.count(ptp.newTransaction()));

    tx = ptp.newTransaction();
    HashMap<byte[], byte[]> map = new HashMap<byte[], byte[]>();
    database.loadMap(tx, objectId1, map, serializer);
    for (Entry<byte[], byte[]> entry : map.entrySet()) {
      Assert.assertTrue(Arrays.equals(key1, entry.getKey()));
      Assert.assertTrue(Arrays.equals(value1, entry.getValue()));
    }
    tx.commit();

    tx = ptp.newTransaction();
    BatchedTransactionImpl bt = new BatchedTransactionImpl(ptp, 1);
    bt.startBatchedTransaction();
    database.deleteCollectionBatched(objectId1, bt);
    long countDeleted = bt.completeBatchedTransaction();

    Assert.assertEquals(1, database.count(ptp.newTransaction()));
    Assert.assertEquals(1, countDeleted);

    tx = ptp.newTransaction();
    map = new HashMap<byte[], byte[]>();
    database.loadMap(tx, objectId2, map, serializer);
    for (Entry<byte[], byte[]> entry : map.entrySet()) {
      Assert.assertTrue(Arrays.equals(key2, entry.getKey()));
      Assert.assertTrue(Arrays.equals(value2, entry.getValue()));
    }
    tx.commit();
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
