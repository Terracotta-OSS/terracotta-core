/*
 * All content copyright (c) 2014 Terracotta, Inc., except as may otherwise
 * be noted in a separate copyright notice. All rights reserved.
 */
package com.tc.objectserver.persistence;

import java.io.File;
import org.terracotta.entity.ServiceProvider;
import org.terracotta.entity.ServiceProviderConfiguration;

/**
 *
 */
public class FlatFileStorageProviderConfiguration implements ServiceProviderConfiguration {
  
  private final File basedir;

  public FlatFileStorageProviderConfiguration(File basedir) {
    this.basedir = basedir;
  }

  public File getBasedir() {
    return basedir;
  }

  @Override
  public Class<? extends ServiceProvider> getServiceProviderType() {
    return FlatFileStorageServiceProvider.class;
  }
  
}
