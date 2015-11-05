/*
 *
 *  The contents of this file are subject to the Terracotta Public License Version
 *  2.0 (the "License"); You may not use this file except in compliance with the
 *  License. You may obtain a copy of the License at
 *
 *  http://terracotta.org/legal/terracotta-public-license.
 *
 *  Software distributed under the License is distributed on an "AS IS" basis,
 *  WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 *  the specific language governing rights and limitations under the License.
 *
 *  The Covered Software is Terracotta Core.
 *
 *  The Initial Developer of the Covered Software is
 *  Terracotta, Inc., a Software AG company
 *
 */
package com.tc.objectserver.persistence;

import java.io.File;
import org.terracotta.entity.ServiceProvider;
import org.terracotta.entity.ServiceProviderConfiguration;


public class FlatFileStorageProviderConfiguration implements ServiceProviderConfiguration {
  private final File basedir;
  private final boolean shouldPersistAcrossRestarts;

  public FlatFileStorageProviderConfiguration(File basedir, boolean shouldPersistAcrossRestarts) {
    this.basedir = basedir;
    this.shouldPersistAcrossRestarts = shouldPersistAcrossRestarts;
  }

  public File getBasedir() {
    return this.basedir;
  }

  public boolean shouldPersistAcrossRestarts() {
    return this.shouldPersistAcrossRestarts;
  }

  @Override
  public Class<? extends ServiceProvider> getServiceProviderType() {
    return FlatFileStorageServiceProvider.class;
  }
}
