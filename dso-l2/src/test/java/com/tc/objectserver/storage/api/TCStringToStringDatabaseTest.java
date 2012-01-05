/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.storage.api;

import com.tc.objectserver.storage.api.TCDatabaseReturnConstants.Status;
import com.tc.util.Assert;

public class TCStringToStringDatabaseTest extends AbstractDatabaseTest {
  private TCStringToStringDatabase database;

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    database = getDbenv().getClusterStateStoreDatabase();
  }

  public void testGetPut() {
    String key = "String-key";
    String value = "String-value";

    PersistenceTransaction tx = newTransaction();
    Status status = database.put(key, value, tx);
    tx.commit();
    Assert.assertEquals(Status.SUCCESS, status);

    tx = newTransaction();
    TCDatabaseEntry<String, String> entry = new TCDatabaseEntry<String, String>();
    status = database.get(entry.setKey(key), tx);
    tx.commit();

    Assert.assertEquals(Status.SUCCESS, status);
    Assert.assertEquals(value, entry.getValue());
  }

  public void testDelete() {
    String key = "String-key";
    String value = "String-value";

    PersistenceTransaction tx = newTransaction();
    Status status = database.put(key, value, tx);
    tx.commit();
    Assert.assertEquals(Status.SUCCESS, status);

    tx = newTransaction();
    TCDatabaseEntry<String, String> entry = new TCDatabaseEntry<String, String>();
    status = database.get(entry.setKey(key), tx);
    tx.commit();

    Assert.assertEquals(Status.SUCCESS, status);
    Assert.assertEquals(value, entry.getValue());

    tx = newTransaction();
    status = database.delete(key, tx);
    tx.commit();
    Assert.assertEquals(Status.SUCCESS, status);

    tx = newTransaction();
    status = database.delete(key, tx);
    tx.commit();
    Assert.assertEquals(Status.NOT_FOUND, status);
  }
}
