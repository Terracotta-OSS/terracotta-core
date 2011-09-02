/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.storage.api;

import com.tc.config.schema.setup.L2ConfigurationSetupManager;
import com.tc.management.beans.object.ServerDBBackupMBean;
import com.tc.properties.TCProperties;
import com.tc.properties.TCPropertiesConsts;
import com.tc.properties.TCPropertiesImpl;
import com.tc.stats.counter.sampled.SampledCounter;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;

import javax.management.NotCompliantMBeanException;

/**
 * This class is responsible for creating db and other classes specific to a particular db.
 */
public abstract class DBFactory {
  private static final DBFactory INSTANCE = getDBFactory();

  private static DBFactory getDBFactory() {
    String factoryName = TCPropertiesImpl.getProperties().getProperty(TCPropertiesConsts.L2_DB_FACTORY_NAME);
    try {
      Class dbClass = Class.forName(factoryName);
      Constructor<DBFactory> constructor = dbClass.getConstructor(TCProperties.class);
      return constructor.newInstance(TCPropertiesImpl.getProperties().getPropertiesFor("l2"));
    } catch (ClassNotFoundException cnfe) {
      throw new RuntimeException("Could not find DBFactory subclass: " + factoryName);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  public static DBFactory getInstance() {
    return INSTANCE;
  }

  public abstract DBEnvironment createEnvironment(boolean paranoid, File envHome, SampledCounter l2FaultFromDisk,
                                                  boolean offheapEnabled) throws IOException;

  public abstract DBEnvironment createEnvironment(boolean paranoid, File envHome) throws IOException;

  public abstract ServerDBBackupMBean getServerDBBackupMBean(final L2ConfigurationSetupManager configurationSetupManager)
      throws NotCompliantMBeanException;
}
