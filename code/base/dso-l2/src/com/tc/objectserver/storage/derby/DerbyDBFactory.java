/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.storage.derby;

import com.tc.config.schema.setup.L2ConfigurationSetupManager;
import com.tc.management.beans.object.DerbyServerDBBackup;
import com.tc.management.beans.object.ServerDBBackupMBean;
import com.tc.objectserver.storage.api.DBEnvironment;
import com.tc.objectserver.storage.api.DBFactory;
import com.tc.properties.TCProperties;
import com.tc.stats.counter.sampled.SampledCounter;

import java.io.File;
import java.io.IOException;
import java.util.Properties;

import javax.management.NotCompliantMBeanException;

public class DerbyDBFactory implements DBFactory {
  private final Properties properties;

  public DerbyDBFactory(final TCProperties l2Properties) {
    this.properties = l2Properties.getPropertiesFor("derbydb").addAllPropertiesTo(new Properties());
  }

  public DBEnvironment createEnvironment(boolean paranoid, File envHome, SampledCounter l2FaultFromDisk,
                                         boolean offheapEnabled) throws IOException {
    return new DerbyDBEnvironment(paranoid, envHome, properties, l2FaultFromDisk, offheapEnabled);
  }

  public ServerDBBackupMBean getServerDBBackupMBean(L2ConfigurationSetupManager configurationSetupManager)
      throws NotCompliantMBeanException {
    return new DerbyServerDBBackup(configurationSetupManager);
  }
}
