/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.storage.api;

import com.tc.config.schema.setup.L2TVSConfigurationSetupManager;
import com.tc.management.beans.object.ServerDBBackupMBean;
import com.tc.objectserver.storage.berkeleydb.BerkeleyDBFactory;

import java.io.File;
import java.io.IOException;
import java.util.Properties;

public class DBFactoryForDBUnitTests implements DBFactory {
  // Just change DB factory here and run tests

  private final DBFactory dbFactory;

  public DBFactoryForDBUnitTests(final Properties prop) {
    dbFactory = new BerkeleyDBFactory(prop);
  }

  public DBEnvironment createEnvironment(boolean paranoid, File envHome) throws IOException {
    return dbFactory.createEnvironment(paranoid, envHome);
  }

  public ServerDBBackupMBean getServerDBBackupMBean(L2TVSConfigurationSetupManager configurationSetupManager) {
    return null;
  }
}
