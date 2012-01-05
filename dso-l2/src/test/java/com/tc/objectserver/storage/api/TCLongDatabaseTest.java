/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.storage.api;

import com.tc.objectserver.storage.api.TCDatabaseReturnConstants.Status;
import com.tc.util.Assert;

import java.util.Set;
import java.util.TreeSet;

public class TCLongDatabaseTest extends AbstractDatabaseTest {
  private TCLongDatabase database;

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    database = getDbenv().getClientStateDatabase();
  }

  public void testPutGetAll() {
    long[] keys = new long[1000];
    for (int i = 0; i < 1000; i++) {
      keys[i] = i;
    }

    for (long key : keys) {
      PersistenceTransaction tx = newTransaction();
      Status status = database.insert(key, tx);
      tx.commit();

      Assert.assertEquals(Status.SUCCESS, status);
    }

    PersistenceTransaction tx = newTransaction();
    Set<Long> keysTemp = database.getAllKeys(tx);
    TreeSet<Long> keysFetched = new TreeSet<Long>();
    keysFetched.addAll(keysTemp);

    Assert.assertEquals(keys.length, keysFetched.size());

    int counter = 0;
    for (Long key : keysFetched) {
      Assert.assertTrue(keys[counter] == key);
      counter++;
    }
  }

  public void testContains() {
    long[] keys = new long[1000];
    for (int i = 0; i < 1000; i++) {
      keys[i] = i;
    }

    for (long key : keys) {
      PersistenceTransaction tx = newTransaction();
      Status status = database.insert(key, tx);
      tx.commit();

      Assert.assertEquals(Status.SUCCESS, status);
    }

    for (long key : keys) {
      PersistenceTransaction tx = newTransaction();
      Assert.assertTrue(database.contains(key, tx));
      tx.commit();
    }
  }

  public void testDelete() {
    long[] keys = new long[1000];
    for (int i = 0; i < 1000; i++) {
      keys[i] = i;
    }

    for (long key : keys) {
      PersistenceTransaction tx = newTransaction();
      Status status = database.insert(key, tx);
      tx.commit();

      Assert.assertEquals(Status.SUCCESS, status);
    }

    PersistenceTransaction tx = newTransaction();
    Set<Long> keysTemp = database.getAllKeys(tx);
    Assert.assertEquals(keys.length, keysTemp.size());

    for (long key : keys) {
      tx = newTransaction();
      Status status = database.delete(key, tx);
      tx.commit();

      Assert.assertEquals(Status.SUCCESS, status);
    }

    tx = newTransaction();
    keysTemp = database.getAllKeys(tx);
    Assert.assertEquals(0, keysTemp.size());
  }
}
