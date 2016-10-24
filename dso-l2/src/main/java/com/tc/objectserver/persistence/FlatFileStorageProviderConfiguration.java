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

import com.tc.util.Assert;


public class FlatFileStorageProviderConfiguration implements ServiceProviderConfiguration {
  private final File basedir;
  private final boolean shouldBlockOnLock;

  public FlatFileStorageProviderConfiguration(File basedir, boolean shouldBlockOnLock) {
    Assert.assertNotNull(basedir);
    Assert.assertTrue(basedir.isDirectory());
    this.basedir = basedir;
    this.shouldBlockOnLock = shouldBlockOnLock;
  }

  public File getBasedir() {
    return this.basedir;
  }

  public boolean shouldBlockOnLock() {
    return this.shouldBlockOnLock;
  }

  @Override
  public Class<? extends ServiceProvider> getServiceProviderType() {
    return FlatFileStorageServiceProvider.class;
  }
}
