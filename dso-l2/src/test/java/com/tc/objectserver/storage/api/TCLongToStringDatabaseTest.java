/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.storage.api;

import com.tc.objectserver.storage.api.TCDatabaseReturnConstants.Status;
import com.tc.util.Assert;

import gnu.trove.TLongObjectHashMap;

public class TCLongToStringDatabaseTest extends AbstractDatabaseTest {
  private TCLongToStringDatabase database;

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    database = getDbenv().getStringIndexDatabase();
  }

  public void testPutGetAll() {
    long[] keys = new long[1000];
    String[] values = new String[keys.length];
    for (int i = 0; i < 1000; i++) {
      keys[i] = i;
      values[i] = String.valueOf(i);
    }

    for (int i = 0; i < keys.length; i++) {
      PersistenceTransaction tx = newTransaction();
      Status status = database.insert(keys[i], values[i], tx);
      tx.commit();

      Assert.assertEquals(Status.SUCCESS, status);
    }

    TLongObjectHashMap map = new TLongObjectHashMap();
    PersistenceTransaction tx = newTransaction();
    map = database.loadMappingsInto(map, tx);

    Assert.assertEquals(keys.length, map.size());

    for (int i = 0; i < keys.length; i++) {
      String str = (String) map.get(keys[i]);
      Assert.assertEquals(values[i], str);
    }
  }
}
