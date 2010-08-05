/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.storage.berkeleydb;

import com.tc.config.schema.setup.L2TVSConfigurationSetupManager;
import com.tc.management.beans.object.ServerDBBackup;
import com.tc.management.beans.object.ServerDBBackupMBean;
import com.tc.objectserver.storage.api.DBEnvironment;
import com.tc.objectserver.storage.api.DBFactory;
import com.tc.statistics.retrieval.actions.SRAForDB;

import java.io.File;
import java.io.IOException;
import java.util.Properties;

import javax.management.NotCompliantMBeanException;

public class BerkeleyDBFactory implements DBFactory {
  public DBEnvironment createEnvironment(boolean paranoid, File envHome, Properties properties) throws IOException {
    return new BerkeleyDBEnvironment(paranoid, envHome, properties);
  }

  public ServerDBBackupMBean getServerDBBackupMBean(L2TVSConfigurationSetupManager configurationSetupManager)
      throws NotCompliantMBeanException {
    return new ServerDBBackup(configurationSetupManager);
  }

  public SRAForDB createSRAForDB(DBEnvironment dbenv) {
    return new SRAForDB(dbenv);
  }
}
