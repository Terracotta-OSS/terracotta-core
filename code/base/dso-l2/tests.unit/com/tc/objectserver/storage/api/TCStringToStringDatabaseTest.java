/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.storage.api;

import org.apache.commons.io.FileUtils;

import com.tc.object.config.schema.L2DSOConfig;
import com.tc.objectserver.storage.api.TCDatabaseReturnConstants.Status;
import com.tc.test.TCTestCase;
import com.tc.util.Assert;

import java.io.File;
import java.util.Properties;

public class TCStringToStringDatabaseTest extends TCTestCase {
  private File                           dbHome;
  private DBEnvironment                  dbenv;
  private PersistenceTransactionProvider ptp;

  private TCStringToStringDatabase       database;

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    File dataPath = getTempDirectory();

    dbHome = new File(dataPath.getAbsolutePath(), L2DSOConfig.OBJECTDB_DIRNAME);
    dbHome.mkdir();

    dbenv = new DBFactoryForDBUnitTests(new Properties()).createEnvironment(true, dbHome, null, false);
    dbenv.open();

    ptp = dbenv.getPersistenceTransactionProvider();
    database = dbenv.getClusterStateStoreDatabase();
  }

  public void testGetPut() {
    String key = "String-key";
    String value = "String-value";

    PersistenceTransaction tx = ptp.newTransaction();
    Status status = database.put(key, value, tx);
    tx.commit();
    Assert.assertEquals(Status.SUCCESS, status);

    tx = ptp.newTransaction();
    TCDatabaseEntry<String, String> entry = new TCDatabaseEntry<String, String>();
    status = database.get(entry.setKey(key), tx);
    tx.commit();

    Assert.assertEquals(Status.SUCCESS, status);
    Assert.assertEquals(value, entry.getValue());
  }

  public void testDelete() {
    String key = "String-key";
    String value = "String-value";

    PersistenceTransaction tx = ptp.newTransaction();
    Status status = database.put(key, value, tx);
    tx.commit();
    Assert.assertEquals(Status.SUCCESS, status);

    tx = ptp.newTransaction();
    TCDatabaseEntry<String, String> entry = new TCDatabaseEntry<String, String>();
    status = database.get(entry.setKey(key), tx);
    tx.commit();

    Assert.assertEquals(Status.SUCCESS, status);
    Assert.assertEquals(value, entry.getValue());

    tx = ptp.newTransaction();
    status = database.delete(key, tx);
    tx.commit();
    Assert.assertEquals(Status.SUCCESS, status);

    tx = ptp.newTransaction();
    status = database.delete(key, tx);
    tx.commit();
    Assert.assertEquals(Status.NOT_FOUND, status);
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
