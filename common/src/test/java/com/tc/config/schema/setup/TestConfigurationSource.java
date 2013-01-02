/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.config.schema.setup;

import com.tc.config.schema.setup.sources.ConfigurationSource;
import com.terracottatech.config.TcConfigDocument;

import java.io.File;
import java.io.InputStream;

public class TestConfigurationSource implements ConfigurationSource {

  @Override
  public File directoryLoadedFrom() {
    return new File(System.getProperty("user.dir"));
  }

  @Override
  public InputStream getInputStream(long maxTimeoutMillis) {
    TcConfigDocument tcConfigDocument = TcConfigDocument.Factory.newInstance();
    tcConfigDocument.addNewTcConfig();
    return tcConfigDocument.newInputStream();
  }

  @Override
  public boolean isTrusted() {
    return true;
  }

}
