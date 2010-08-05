/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.storage.api;

import com.tc.config.schema.setup.L2TVSConfigurationSetupManager;
import com.tc.management.beans.object.ServerDBBackupMBean;
import com.tc.objectserver.storage.api.DBEnvironment;
import com.tc.objectserver.storage.api.DBFactory;
import com.tc.objectserver.storage.berkeleydb.BerkeleyDBFactory;
import com.tc.statistics.retrieval.actions.SRAForDB;

import java.io.File;
import java.io.IOException;
import java.util.Properties;

public class DBFactoryForDBUnitTests implements DBFactory {
  // Just change DB factory here and run tests
  private final DBFactory dbFactory = new BerkeleyDBFactory();

  public DBEnvironment createEnvironment(boolean paranoid, File envHome, Properties properties) throws IOException {
    return dbFactory.createEnvironment(paranoid, envHome, properties);
  }

  public SRAForDB createSRAForDB(DBEnvironment dbenv) {
    return null;
  }

  public ServerDBBackupMBean getServerDBBackupMBean(L2TVSConfigurationSetupManager configurationSetupManager) {
    return null;
  }
}
