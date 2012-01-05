/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.storage.api;

import com.tc.objectserver.persistence.db.TCDatabaseException;
import com.tc.objectserver.storage.api.TCDatabaseReturnConstants.Status;
import com.tc.util.Assert;

import java.util.Arrays;
import java.util.Random;

public class TCBytesToBytesDatabaseTest extends AbstractDatabaseTest {
  private final Random           random = new Random();

  private TCBytesToBytesDatabase database;

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    database = getDbenv().getObjectOidStoreDatabase();
  }

  public void testPutGet() {
    byte[] key = getRandomlyFilledByteArray();
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

  public void testPutNoOverwrite() {
    byte[] key = getRandomlyFilledByteArray();
    byte[] value1 = getRandomlyFilledByteArray();

    PersistenceTransaction tx = newTransaction();
    Status status = database.putNoOverwrite(tx, key, value1);
    tx.commit();

    Assert.assertEquals(Status.SUCCESS, status);

    byte[] value2 = getRandomlyFilledByteArray();
    tx = newTransaction();
    status = database.putNoOverwrite(tx, key, value2);
    tx.commit();

    Assert.assertEquals(Status.NOT_SUCCESS, status);

    tx = newTransaction();
    byte[] valueReturned = database.get(key, tx);
    tx.commit();

    Assert.assertTrue(Arrays.equals(value1, valueReturned));
  }

  public void testDelete() {
    byte[] key = getRandomlyFilledByteArray();
    byte[] value = getRandomlyFilledByteArray();

    PersistenceTransaction tx = newTransaction();
    Status status = database.put(key, value, tx);
    tx.commit();

    Assert.assertEquals(Status.SUCCESS, status);

    tx = newTransaction();
    byte[] valueReturned = database.get(key, tx);
    tx.commit();

    Assert.assertTrue(Arrays.equals(value, valueReturned));

    tx = newTransaction();
    status = database.delete(key, tx);
    tx.commit();

    Assert.assertEquals(Status.SUCCESS, status);

    tx = newTransaction();
    valueReturned = database.get(key, tx);
    tx.commit();

    Assert.assertNull(valueReturned);
  }

  public void testCursorWithConcurrentRead() {
    byte[][] keys = { getRandomlyFilledByteArray(), getRandomlyFilledByteArray(), getRandomlyFilledByteArray(),
        getRandomlyFilledByteArray() };
    byte[][] values = { getRandomlyFilledByteArray(), getRandomlyFilledByteArray(), getRandomlyFilledByteArray(),
        getRandomlyFilledByteArray() };

    byte[] key1 = getRandomlyFilledByteArray();
    byte[] value1 = getRandomlyFilledByteArray();

    TCBytesToBytesDatabase bytesToBytesDatabase = null;
    try {
      bytesToBytesDatabase = getDbenv().getObjectOidStoreDatabase();
    } catch (TCDatabaseException e) {
      throw new AssertionError(e);
    }

    PersistenceTransaction tx = newTransaction();
    for (int i = 0; i < keys.length; i++) {
      byte[] key = keys[i];
      byte[] value = values[i];
      database.put(key, value, tx);
    }
    bytesToBytesDatabase.put(key1, value1, tx);
    tx.commit();

    tx = newTransaction();
    TCDatabaseCursor<byte[], byte[]> cursor = database.openCursorUpdatable(tx);

    while (cursor.hasNext()) {
      cursor.next();
      // get
      bytesToBytesDatabase.get(key1, tx);
      cursor.delete();
    }
    cursor.close();
    tx.commit();

    tx = newTransaction();
    cursor = database.openCursorUpdatable(tx);
    Assert.assertFalse(cursor.hasNext());
    cursor.close();
    tx.commit();
  }

  private byte[] getRandomlyFilledByteArray() {
    byte[] array = new byte[100];
    random.nextBytes(array);
    return array;
  }
}
