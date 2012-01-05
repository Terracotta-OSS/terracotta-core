/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.storage.api;

import com.tc.objectserver.storage.api.TCDatabaseReturnConstants.Status;
import com.tc.util.Assert;

import java.util.Arrays;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;

public class TCIntToBytesDatabaseTest extends AbstractDatabaseTest {
  private final Random         random = new Random();

  private TCIntToBytesDatabase database;

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    database = getDbenv().getClassDatabase();
  }

  public void testPutGet() {
    int key = random.nextInt(100);
    byte[] value = getRandomlyFilledByteArray();

    PersistenceTransaction tx = newTransaction();
    Status status = database.put(key, value, tx);
    tx.commit();

    Assert.assertEquals(Status.SUCCESS, status);

    tx = newTransaction();
    byte[] valueReturned = database.get(key, tx);
    tx.commit();

    Assert.assertTrue(Arrays.equals(value, valueReturned));
  }

  public void testGetAll() {
    int[] keys = { 0, 1, 2, 3, 4, 5 };
    byte[][] values = new byte[keys.length][100];

    for (int i = 0; i < keys.length; i++) {
      values[i] = getRandomlyFilledByteArray();
    }

    for (int i = 0; i < keys.length; i++) {
      PersistenceTransaction tx = newTransaction();
      Status status = database.put(keys[i], values[i], tx);
      tx.commit();

      Assert.assertEquals(Status.SUCCESS, status);
    }

    PersistenceTransaction tx = newTransaction();
    Map<Integer, byte[]> keyValuesFetched = database.getAll(tx);

    Assert.assertEquals(keys.length, keyValuesFetched.size());

    for (Entry<Integer, byte[]> entry : keyValuesFetched.entrySet()) {
      int index = entry.getKey().intValue();
      Assert.assertTrue(index < keys.length && index > -1);
      Assert.assertTrue(Arrays.equals(values[index], entry.getValue()));
    }
  }

  private byte[] getRandomlyFilledByteArray() {
    byte[] array = new byte[100];
    random.nextBytes(array);
    return array;
  }
}
