/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.storage.api;

import com.tc.objectserver.persistence.db.DBException;
import com.tc.objectserver.persistence.db.TCCollectionsSerializer;
import com.tc.objectserver.persistence.db.TCCollectionsSerializerImpl;
import com.tc.objectserver.storage.berkeleydb.BerkeleyDBTCMapsDatabase;
import com.tc.util.Assert;
import com.tc.util.Conversion;

import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Set;

public class TCMapsDatabaseTest extends AbstractDatabaseTest {
  private static final TCCollectionsSerializer serializer = new TCCollectionsSerializerImpl();
  private final Random                         random     = new Random();

  private TCMapsDatabase                       database;

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    this.database = getDbenv().getMapsDatabase();
  }

  public void testBigPutDelete() throws Exception {
    testPutDelete(64000);
  }

  public void testSmallPutDelete() throws Exception {
    testPutDelete(100);
  }

  private void testPutDelete(int size) throws Exception {
    long objectId = 1;

    byte[] key = getRandomlyFilledByteArray(objectId, size);
    byte[] value = getRandomlyFilledByteArray(objectId, size);

    PersistenceTransaction tx = newTransaction();
    int written = database.insert(tx, objectId, key, value, serializer);
    tx.commit();

    Assert.assertTrue(written > 0);

    tx = newTransaction();
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

    tx = newTransaction();
    written = database.delete(tx, objectId, key, serializer);
    tx.commit();

    Assert.assertTrue(written > 0);

    tx = newTransaction();
    Assert.assertEquals(0, database.count(tx));
    tx.commit();
  }

  public void testBigInsert() throws Exception {
    testInsert(64000);
  }

  public void testSmallInsert() throws Exception {
    testInsert(100);
  }

  private void testInsert(int size) throws Exception {
    long mapId = 1;

    Map<String, String> reference = new HashMap<String, String>();
    PersistenceTransaction tx = newTransaction();
    for (int i = 0; i < 10; i++) {
      String key = getRandomString(mapId, size);
      String value = getRandomString(mapId, size);
      reference.put(key, value);
      database.insert(tx, mapId, key, value, serializer);
    }
    tx.commit();

    Map<String, String> actual = new HashMap<String, String>();
    tx = newTransaction();
    database.loadMap(tx, mapId, actual, serializer);
    tx.commit();

    assertEquals(reference, actual);
  }

  public void testInsertBigDuplicate() throws Exception {
    long mapId = 1;
    String key = getRandomString(mapId, 64000);
    String value = getRandomString(mapId, 64000);

    PersistenceTransaction tx = newTransaction();
    database.insert(tx, mapId, key, value, serializer);
    tx.commit();

    tx = newTransaction();
    try {
      database.insert(tx, mapId, key, value, serializer);
      // BDB doesn't detect duplicate inserts, both update and insert just map to put, so we don't fail in the case of
      // bdb.
      assertTrue(database instanceof BerkeleyDBTCMapsDatabase);
    } catch (DBException e) {
      assertEquals("Duplicate key insert into map " + mapId, e.getMessage());
    }
    tx.commit();
  }

  public void testBigUpdate() throws Exception {
    testUpdate(64000);
  }

  public void testSmallUpdate() throws Exception {
    testUpdate(100);
  }

  private void testUpdate(int size) throws Exception {
    long mapId = 1;

    Map<String, String> reference = new HashMap<String, String>();
    PersistenceTransaction tx = newTransaction();
    for (int i = 0; i < 10; i++) {
      String key = getRandomString(mapId, size);
      String value = getRandomString(mapId, size);
      reference.put(key, value);
      database.insert(tx, mapId, key, value, serializer);
    }
    tx.commit();

    tx = newTransaction();
    for (String referenceKey : reference.keySet()) {
      String newValue = getRandomString(mapId, size);
      reference.put(referenceKey, newValue);
      database.update(tx, mapId, referenceKey, newValue, serializer);
    }
    tx.commit();

    Map<String, String> actual = new HashMap<String, String>();
    tx = newTransaction();
    database.loadMap(tx, mapId, actual, serializer);
    tx.commit();

    assertEquals(reference, actual);
  }

  public void testBigDeleteCollections() throws Exception {
    testDeleteCollections(64000);
  }

  public void testSmallDeleteCollections() throws Exception {
    testDeleteCollections(100);
  }

  private void testDeleteCollections(int size) throws Exception {

    long objectId1 = 1;
    byte[] key1 = getRandomlyFilledByteArray(objectId1, size);
    byte[] value1 = getRandomlyFilledByteArray(objectId1, size);

    long objectId2 = 2;
    byte[] key2 = getRandomlyFilledByteArray(objectId2, size);
    byte[] value2 = getRandomlyFilledByteArray(objectId2, size);

    PersistenceTransaction tx = newTransaction();
    database.insert(tx, objectId1, key1, value1, serializer);
    database.insert(tx, objectId2, key2, value2, serializer);
    tx.commit();

    tx = newTransaction();
    Assert.assertEquals(2, database.count(tx));
    tx.commit();

    tx = newTransaction();
    database.deleteCollection(objectId1, tx);
    tx.commit();

    tx = newTransaction();
    Assert.assertEquals(1, database.count(tx));
    tx.commit();

    tx = newTransaction();
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

  public void testBigDelete() throws Exception {
    testDelete(64000);
  }

  public void testSmallDelete() throws Exception {
    testDelete(100);
  }

  private void testDelete(int size) throws Exception {
    long objectId1 = 1;
    long objectId2 = 2;

    String key = getRandomString(objectId1, size);
    String value = getRandomString(objectId1, size);

    PersistenceTransaction tx = newTransaction();
    database.insert(tx, objectId1, key, value, serializer);
    database.insert(tx, objectId2, key, value, serializer);
    tx.commit();

    tx = newTransaction();
    database.delete(tx, objectId1, key, serializer);
    tx.commit();

    tx = newTransaction();
    Assert.assertEquals(1, database.count(tx));
    tx.commit();
  }

  public void testHashCollisions() throws Exception {
    // Only really applicable to DerbyTCMapsDatabase
    // The following keys should all hash to 1922
    Map<String, String> referenceMap = new HashMap<String, String>();
    referenceMap.put(new String(frontPadKey(new byte[] { 2, 0, 0 }, 20000)), getRandomString(1, 200));
    referenceMap.put(new String(frontPadKey(new byte[] { 1, 31, 0 }, 20000)), getRandomString(1, 200));
    referenceMap.put(new String(frontPadKey(new byte[] { 0, 62, 0 }, 20000)), getRandomString(1, 200));
    referenceMap.put(new String(frontPadKey(new byte[] { 1, 30, 31 }, 20000)), getRandomString(1, 200));
    referenceMap.put(new String(frontPadKey(new byte[] { 0, 61, 31 }, 20000)), getRandomString(1, 200));
    referenceMap.put(new String(frontPadKey(new byte[] { 1, 29, 62 }, 20000)), getRandomString(1, 200));
    referenceMap.put(new String(frontPadKey(new byte[] { 0, 60, 62 }, 20000)), getRandomString(1, 200));
    referenceMap.put(new String(frontPadKey(new byte[] { 1, 28, 93 }, 20000)), getRandomString(1, 200));
    referenceMap.put(new String(frontPadKey(new byte[] { 0, 59, 93 }, 20000)), getRandomString(1, 200));
    referenceMap.put(new String(frontPadKey(new byte[] { 1, 27, 124 }, 20000)), getRandomString(1, 200));
    referenceMap.put(new String(frontPadKey(new byte[] { 0, 58, 124 }, 20000)), getRandomString(1, 200));

    PersistenceTransaction tx = newTransaction();
    for (Entry<String, String> entry : referenceMap.entrySet()) {
      database.insert(tx, 1, entry.getKey().getBytes(), entry.getValue(), serializer);
    }
    tx.commit();

    tx = newTransaction();
    Map<byte[], String> actualMap = new HashMap<byte[], String>();
    database.loadMap(tx, 1, actualMap, serializer);
    assertEquals(11, actualMap.size());
    assertEquals(11, database.count(tx));
    tx.commit();

    for (Entry<byte[], String> entry : actualMap.entrySet()) {
      assertEquals(referenceMap.get(new String(entry.getKey())), entry.getValue());
    }

    tx = newTransaction();
    for (String key : referenceMap.keySet()) {
      String updatedValue = getRandomString(1, 200);
      referenceMap.put(key, updatedValue);
      assertTrue(database.update(tx, 1, key.getBytes(), updatedValue, serializer) > 0);
    }
    assertEquals(11, database.count(tx));
    tx.commit();

    tx = newTransaction();
    Map<byte[], String> updatedActualMap = new HashMap<byte[], String>();
    database.loadMap(tx, 1, updatedActualMap, serializer);
    assertEquals(11, updatedActualMap.size());
    tx.commit();

    for (Entry<byte[], String> entry : updatedActualMap.entrySet()) {
      assertEquals(referenceMap.get(new String(entry.getKey())), entry.getValue());
    }

    Set<String> keys = new HashSet<String>(referenceMap.keySet());
    int count = 11;
    for (String key : keys) {
      count--;
      Map<byte[], String> emptyingMap = new HashMap<byte[], String>();
      tx = newTransaction();
      database.delete(tx, 1, key.getBytes(), serializer);
      tx.commit();

      tx = newTransaction();
      database.loadMap(tx, 1, emptyingMap, serializer);
      assertEquals(count, database.count(tx));
      tx.commit();
    }
  }

  public void testSmallBigKeyTransition() throws Exception {
    long mapId = 1;
    Map<String, String> referenceMap = new HashMap<String, String>();
    // Transition is 15992
    for (int i = 15990; i < 15995; i++) {
      referenceMap.put(getRandomString(1, i), getRandomString(1, 100));
    }
    PersistenceTransaction tx = newTransaction();
    for (Entry<String, String> entry : referenceMap.entrySet()) {
      database.insert(tx, mapId, entry.getKey(), entry.getValue(), serializer);
    }
    tx.commit();

    Map<String, String> actualMap = new HashMap<String, String>();
    tx = newTransaction();
    database.loadMap(tx, mapId, actualMap, serializer);
    tx.commit();

    assertEquals(referenceMap, actualMap);

    tx = newTransaction();
    for (String key : referenceMap.keySet()) {
      String newValue = getRandomString(mapId, 200);
      referenceMap.put(key, newValue);
      database.update(tx, mapId, key, newValue, serializer);
    }
    tx.commit();

    tx = newTransaction();
    actualMap.clear();
    database.loadMap(tx, mapId, actualMap, serializer);
    tx.commit();

    assertEquals(referenceMap, actualMap);

    Iterator<String> refernceKeyIterator = referenceMap.keySet().iterator();
    while (refernceKeyIterator.hasNext()) {
      String key = refernceKeyIterator.next();
      refernceKeyIterator.remove();
      tx = newTransaction();
      database.delete(tx, mapId, key, serializer);
      tx.commit();

      actualMap.clear();
      tx = newTransaction();
      database.loadMap(tx, mapId, actualMap, serializer);
      tx.commit();

      assertEquals(referenceMap, actualMap);
    }
  }

  private byte[] frontPadKey(byte[] key, int padding) {
    byte[] paddedKey = new byte[key.length + padding];
    System.arraycopy(key, 0, paddedKey, paddedKey.length - key.length, key.length);
    return paddedKey;
  }

  private byte[] getRandomlyFilledByteArray(final long objectId, int arraySize) {
    final byte[] array = new byte[arraySize];
    this.random.nextBytes(array);

    final byte[] temp = Conversion.long2Bytes(objectId);
    for (int i = 0; i < temp.length; i++) {
      array[i] = temp[i];
    }
    if (arraySize < 60000) return array;
    for (int i = 0; i < 60000; i++) {
      array[i] = 0;
    }
    return array;
  }

  private String getRandomString(final long objectId, int arraySize) throws UnsupportedEncodingException {
    return new String(getRandomlyFilledByteArray(objectId, arraySize), "US-ASCII");
  }
}
