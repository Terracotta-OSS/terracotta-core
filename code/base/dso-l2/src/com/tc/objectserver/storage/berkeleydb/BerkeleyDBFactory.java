/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.storage.berkeleydb;

import com.tc.objectserver.storage.api.DBEnvironment;
import com.tc.objectserver.storage.api.DBFactory;

import java.io.File;
import java.io.IOException;
import java.util.Properties;

public class BerkeleyDBFactory implements DBFactory {
  private final Properties properties;

  public BerkeleyDBFactory(final Properties properties) {
    this.properties = properties;
  }

  public DBEnvironment createEnvironment(boolean paranoid, File envHome) throws IOException {
    return new BerkeleyDBEnvironment(paranoid, envHome, properties);
  }

}
