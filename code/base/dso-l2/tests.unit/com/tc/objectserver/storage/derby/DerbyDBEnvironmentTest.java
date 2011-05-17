/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.storage.derby;

import com.tc.objectserver.storage.api.DBEnvironment;
import com.tc.properties.TCPropertiesConsts;
import com.tc.stats.counter.sampled.SampledCounter;
import com.tc.test.TCTestCase;

import java.io.File;
import java.io.FileInputStream;
import java.sql.Connection;
import java.util.Properties;

public class DerbyDBEnvironmentTest extends TCTestCase {

  public void testTableExists() throws Exception {
    File dataDir = new File(getTempDirectory(), "testReopen");
    dataDir.mkdirs();
    DerbyDBEnvironment derbyEnv = new DerbyDBEnvironment(true, dataDir);
    assertTrue(derbyEnv.open());

    Connection connection = derbyEnv.createConnection();
    assertTrue(DerbyDBEnvironment.tableExists(connection, DBEnvironment.OBJECT_DB_NAME));
    assertFalse(DerbyDBEnvironment.tableExists(connection, DBEnvironment.OBJECT_DB_NAME + "nope"));
    assertFalse(DerbyDBEnvironment.tableExists(connection, "nope" + DBEnvironment.OBJECT_DB_NAME));

    connection.close();
    derbyEnv.close();
  }

  public void testHeapUsage() throws Exception {
    File dataDir = new File(getTempDirectory(), "testHeapUsage");
    dataDir.mkdirs();
    Properties props = new Properties();
    props.setProperty(TCPropertiesConsts.DERBY_MAXMEMORYPERCENT, "25");
    DerbyDBEnvironment derbyEnv = new DerbyDBEnvironment(false, dataDir, props, SampledCounter.NULL_SAMPLED_COUNTER,
                                                         false);
    derbyEnv.open();

    Properties derbyProps = new Properties();
    derbyProps.load(new FileInputStream(new File(dataDir, "derby.properties")));
    assertTrue(derbyProps.containsKey(TCPropertiesConsts.DERBY_STORAGE_PAGECACHESIZE));
    assertTrue(derbyProps.containsKey(TCPropertiesConsts.DERBY_LOG_BUFFER_SIZE));
    assertTrue(derbyProps.containsKey(TCPropertiesConsts.DERBY_STORAGE_PAGESIZE));
    // TODO: Maybe check the calculation too? A bit tricky without the page size overhead constants though.
    derbyEnv.close();
  }

  public void testLogDevice() throws Exception {
    File dataDir = new File(getTempDirectory(), "dbDataDir");
    File logDir = new File(getTempDirectory(), "dbLogDir");
    dataDir.mkdir();
    logDir.mkdir();
    Properties props = new Properties();
    props.setProperty(TCPropertiesConsts.DERBY_LOG_DEVICE, logDir.getAbsolutePath());
    DerbyDBEnvironment derbyEnv = new DerbyDBEnvironment(false, dataDir, props, SampledCounter.NULL_SAMPLED_COUNTER,
                                                         false);
    derbyEnv.open();
    assertTrue(new File(logDir, "log").exists());
    derbyEnv.close();
  }

}
