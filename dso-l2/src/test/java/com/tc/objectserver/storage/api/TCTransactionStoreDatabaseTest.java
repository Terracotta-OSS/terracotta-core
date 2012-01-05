/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.storage.api;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class TCTransactionStoreDatabaseTest extends AbstractDatabaseTest {
  private final Random               random = new Random();
  private TCTransactionStoreDatabase database;

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    database = getDbenv().getTransactionDatabase();
  }

  public void testCursor() throws Exception {
    Map<Long, byte[]> objects = new HashMap<Long, byte[]>();
    PersistenceTransaction tx = newTransaction();
    for (long i = 0; i < 100; i++) {
      objects.put(i, getRandomlyFilledByteArray());
      database.insert(i, objects.get(i), tx);
    }
    tx.commit();

    tx = newTransaction();
    TCDatabaseCursor<Long, byte[]> cursor = database.openCursor(tx);
    while (cursor.hasNext()) {
      TCDatabaseEntry<Long, byte[]> entry = cursor.next();
      byte[] value = objects.remove(entry.getKey());
      assertNotNull(value);
      assertTrue(Arrays.equals(value, entry.getValue()));
    }
    cursor.close();
    tx.commit();

    assertTrue(objects.isEmpty());

  }

  private byte[] getRandomlyFilledByteArray() {
    byte[] array = new byte[100];
    random.nextBytes(array);
    return array;
  }
}
