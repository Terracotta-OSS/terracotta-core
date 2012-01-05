/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.storage.api;

import com.tc.object.ObjectID;
import com.tc.objectserver.storage.api.TCDatabaseReturnConstants.Status;
import com.tc.util.Assert;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Set;

public class TCRootDatabaseTest extends AbstractDatabaseTest {
  private final Random   random = new Random();
  private TCRootDatabase database;

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    database = getDbenv().getRootDatabase();
  }

  public void testGetPut() {
    long objectId = 1;
    byte[] key = getRandomlyFilledByteArray();

    PersistenceTransaction tx = newTransaction();
    Status status = database.put(key, objectId, tx);
    tx.commit();
    Assert.assertEquals(Status.SUCCESS, status);

    tx = newTransaction();
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
      PersistenceTransaction tx = newTransaction();
      Status status = database.put(keys[i], i, tx);
      tx.commit();
      Assert.assertEquals(Status.SUCCESS, status);
    }

    PersistenceTransaction tx = newTransaction();
    Set<ObjectID> rootIds = database.getRootIds(tx);
    Assert.assertEquals(keys.length, rootIds.size());
    for (int i = 0; i < keys.length; i++) {
      Assert.assertTrue(rootIds.contains(new ObjectID(i)));
    }

    tx = newTransaction();
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

    tx = newTransaction();
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
}
