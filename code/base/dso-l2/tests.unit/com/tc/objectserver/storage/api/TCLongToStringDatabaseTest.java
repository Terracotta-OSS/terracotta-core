/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.storage.api;

import org.apache.commons.io.FileUtils;

import com.tc.object.config.schema.L2DSOConfig;
import com.tc.objectserver.storage.api.TCDatabaseReturnConstants.Status;
import com.tc.test.TCTestCase;
import com.tc.util.Assert;

import gnu.trove.TLongObjectHashMap;

import java.io.File;
import java.util.Properties;

public class TCLongToStringDatabaseTest extends TCTestCase {
  private File                           dbHome;
  private DBEnvironment                  dbenv;
  private PersistenceTransactionProvider ptp;

  private TCLongToStringDatabase         database;

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    File dataPath = getTempDirectory();

    dbHome = new File(dataPath.getAbsolutePath(), L2DSOConfig.OBJECTDB_DIRNAME);
    dbHome.mkdir();

    dbenv = new DBFactoryForDBUnitTests(new Properties()).createEnvironment(true, dbHome, null, false);
    dbenv.open();

    ptp = dbenv.getPersistenceTransactionProvider();
    database = dbenv.getStringIndexDatabase();
  }

  public void testPutGetAll() {
    long[] keys = new long[1000];
    String[] values = new String[keys.length];
    for (int i = 0; i < 1000; i++) {
      keys[i] = i;
      values[i] = String.valueOf(i);
    }

    for (int i = 0; i < keys.length; i++) {
      PersistenceTransaction tx = ptp.newTransaction();
      Status status = database.insert(keys[i], values[i], tx);
      tx.commit();

      Assert.assertEquals(Status.SUCCESS, status);
    }

    TLongObjectHashMap map = new TLongObjectHashMap();
    PersistenceTransaction tx = ptp.newTransaction();
    map = database.loadMappingsInto(map, tx);

    Assert.assertEquals(keys.length, map.size());

    for (int i = 0; i < keys.length; i++) {
      String str = (String) map.get(keys[i]);
      Assert.assertEquals(values[i], str);
    }
  }

  @Override
  protected void tearDown() throws Exception {
    super.tearDown();
    try {
      dbenv.close();
      FileUtils.cleanDirectory(dbHome);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
}
