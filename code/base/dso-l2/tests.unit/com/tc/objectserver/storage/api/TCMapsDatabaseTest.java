/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.storage.api;

import org.apache.commons.io.FileUtils;

import com.tc.object.config.schema.L2DSOConfig;
import com.tc.objectserver.persistence.db.TCCollectionsSerializer;
import com.tc.objectserver.persistence.db.TCCollectionsSerializerImpl;
import com.tc.test.TCTestCase;
import com.tc.util.Assert;
import com.tc.util.Conversion;

import java.io.File;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Properties;
import java.util.Random;
import java.util.Map.Entry;

public class TCMapsDatabaseTest extends TCTestCase {
  private final Random                   random = new Random();
  private File                           dbHome;
  private DBEnvironment                  dbenv;
  private PersistenceTransactionProvider ptp;

  private TCMapsDatabase                 database;

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    final File dataPath = getTempDirectory();

    this.dbHome = new File(dataPath.getAbsolutePath(), L2DSOConfig.OBJECTDB_DIRNAME);
    this.dbHome.mkdir();

    this.dbenv = new DBFactoryForDBUnitTests(new Properties()).createEnvironment(true, this.dbHome, null, false);
    this.dbenv.open();

    this.ptp = this.dbenv.getPersistenceTransactionProvider();
    this.database = this.dbenv.getMapsDatabase();
  }

  public void testPutDelete() throws Exception {
    long objectId = 1;
    TCCollectionsSerializer serializer = new TCCollectionsSerializerImpl();

    byte[] key = getRandomlyFilledByteArray(objectId);
    byte[] value = getRandomlyFilledByteArray(objectId);

    PersistenceTransaction tx = ptp.newTransaction();
    int written = database.insert(tx, objectId, key, value, serializer);
    tx.commit();

    Assert.assertTrue(written > 0);

    tx = ptp.newTransaction();
    HashMap<byte[], byte[]> map = new HashMap<byte[], byte[]>();
    database.loadMap(tx, objectId, map, serializer);
    int count = 0;
    for (Entry<byte[], byte[]> entry : map.entrySet()) {
      Assert.assertTrue(Arrays.equals(key, entry.getKey()));
      Assert.assertTrue(Arrays.equals(value, entry.getValue()));
      count++;
    }
    tx.commit();

    Assert.assertEquals(1, count);

    tx = ptp.newTransaction();
    written = database.delete(tx, objectId, key, serializer);
    tx.commit();

    Assert.assertTrue(written > 0);

    Assert.assertEquals(0, database.count(ptp.newTransaction()));
  }

  public void testDeleteCollections() throws Exception {
    TCCollectionsSerializer serializer = new TCCollectionsSerializerImpl();

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
    database.deleteCollection(objectId1, tx);
    tx.commit();

    Assert.assertEquals(1, database.count(ptp.newTransaction()));

    tx = ptp.newTransaction();
    HashMap<byte[], byte[]> map = new HashMap<byte[], byte[]>();
    database.loadMap(tx, objectId2, map, serializer);
    int count = 0;
    for (Entry<byte[], byte[]> entry : map.entrySet()) {
      Assert.assertTrue(Arrays.equals(key2, entry.getKey()));
      Assert.assertTrue(Arrays.equals(value2, entry.getValue()));
      count++;
    }
    tx.commit();

    Assert.assertEquals(1, count);
  }

  private byte[] getRandomlyFilledByteArray(final long objectId) {
    final byte[] array = new byte[108];
    this.random.nextBytes(array);

    final byte[] temp = Conversion.long2Bytes(objectId);
    for (int i = 0; i < temp.length; i++) {
      array[i] = temp[i];
    }
    return array;
  }

  public void testDelete() throws Exception {
    long objectId1 = 1;
    long objectId2 = 2;
    TCCollectionsSerializer serializer = new TCCollectionsSerializerImpl();

    String key = "key";
    String value = "value";

    PersistenceTransaction tx = ptp.newTransaction();
    database.insert(tx, objectId1, key, value, serializer);
    database.insert(tx, objectId2, key, value, serializer);
    tx.commit();

    tx = ptp.newTransaction();
    database.delete(tx, objectId1, key, serializer);
    tx.commit();

    Assert.assertEquals(1, database.count(ptp.newTransaction()));
  }

  @Override
  protected void tearDown() throws Exception {
    super.tearDown();
    try {
      this.dbenv.close();
      FileUtils.cleanDirectory(this.dbHome);
    } catch (final Exception e) {
      e.printStackTrace();
    }
  }
}
