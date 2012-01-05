/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.storage.api;

import com.tc.objectserver.storage.api.TCDatabaseReturnConstants.Status;
import com.tc.util.Assert;

import java.util.Arrays;
import java.util.Random;

public class TCLongToBytesDatabaseTest extends AbstractDatabaseTest {
  private final Random          random = new Random();

  private TCLongToBytesDatabase database;

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    database = getDbenv().getObjectDatabase();
  }

  public void testInsertUpdateGet() {
    long objectId1 = 1;
    byte[] value1 = getRandomlyFilledByteArray();
    byte[] value2 = getRandomlyFilledByteArray();

    PersistenceTransaction tx = newTransaction();
    Status status = database.insert(objectId1, value1, tx);
    tx.commit();
    Assert.assertEquals(Status.SUCCESS, status);

    tx = newTransaction();
    byte[] valueFetched = database.get(objectId1, tx);
    tx.commit();
    Assert.assertTrue(Arrays.equals(value1, valueFetched));

    tx = newTransaction();
    status = database.update(objectId1, value2, tx);
    tx.commit();

    Assert.assertEquals(Status.SUCCESS, status);

    tx = newTransaction();
    valueFetched = database.get(objectId1, tx);
    tx.commit();
    Assert.assertTrue(Arrays.equals(value2, valueFetched));
  }

  public void testDelete() {
    long objectId1 = 1;
    byte[] value1 = getRandomlyFilledByteArray();

    PersistenceTransaction tx = newTransaction();
    Status status = database.insert(objectId1, value1, tx);
    tx.commit();
    Assert.assertEquals(Status.SUCCESS, status);

    tx = newTransaction();
    status = database.delete(objectId1, tx);
    tx.commit();
    Assert.assertEquals(Status.SUCCESS, status);

    tx = newTransaction();
    status = database.delete(objectId1, tx);
    tx.commit();
    Assert.assertEquals(Status.NOT_FOUND, status);
  }

  private byte[] getRandomlyFilledByteArray() {
    byte[] array = new byte[100];
    random.nextBytes(array);
    return array;
  }
}
