/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.storage.derby;

import com.tc.objectserver.persistence.db.TCDatabaseException;
import com.tc.properties.TCPropertiesConsts;
import com.tc.test.TCTestCase;

import java.io.File;
import java.io.IOException;
import java.util.Properties;

public class DerbyDBEnvironmentTest extends TCTestCase {

  public void testLogDevice() throws IOException, TCDatabaseException {
    File dataDir = new File(getTempDirectory(), "dbDataDir");
    File logDir = new File(getTempDirectory(), "dbLogDir");
    dataDir.mkdir();
    logDir.mkdir();
    Properties props = new Properties();
    props.setProperty(TCPropertiesConsts.DERBY_LOG_DEVICE, logDir.getAbsolutePath());
    DerbyDBEnvironment derbyEnv = new DerbyDBEnvironment(false, dataDir, props, null);
    derbyEnv.createDatabase();
    assertTrue(new File(logDir, "log").exists());
  }

}
