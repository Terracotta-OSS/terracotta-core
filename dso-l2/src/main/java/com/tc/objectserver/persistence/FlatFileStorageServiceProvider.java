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

import org.terracotta.entity.ServiceProvider;
import org.terracotta.entity.ServiceConfiguration;
import org.terracotta.entity.ServiceProviderCleanupException;
import org.terracotta.persistence.IPersistentStorage;

import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.util.Assert;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import java.util.HashSet;

import org.terracotta.entity.ServiceProviderConfiguration;


/**
 * This service provides a very simple key-value storage persistence system.  It allows key-value data to be serialized to
 * a file in the working directory using Java serialization.
 * 
 * The initial use was to test/support platform restart without depending on CoreStorage.
 */
public class FlatFileStorageServiceProvider implements ServiceProvider {
  private static final TCLogger logger = TCLogging.getLogger(FlatFileStorageServiceProvider.class);
  private boolean shouldPersistAcrossRestarts;
  private Path directory;
  private final Set<Long> consumers = new HashSet<>();

  @Override
  public boolean initialize(ServiceProviderConfiguration configuration) {
    // Currently, this provider is created directly so there is no chance of seeing any other kind of provider.
    // In the future, this may change.
    Assert.assertTrue(configuration instanceof FlatFileStorageProviderConfiguration);
    FlatFileStorageProviderConfiguration flatFileConfiguration = (FlatFileStorageProviderConfiguration)configuration;
    this.shouldPersistAcrossRestarts = flatFileConfiguration.shouldPersistAcrossRestarts();
    File targetDirectory = flatFileConfiguration.getBasedir();
    if (null != targetDirectory) {
      this.directory = targetDirectory.toPath();
    } else {
      this.directory = Paths.get(".").toAbsolutePath().normalize();
    }
    logger.info("Initialized flat file storage to: " + this.directory);
    return true;
  }

  @Override
  public <T> T getService(long consumerID, ServiceConfiguration<T> configuration) {
    consumers.add(consumerID);
    String filename = "consumer_" + consumerID + ".dat";
    File file = this.directory.resolve(filename).toFile();
    // If this is being configured as non-restartable, we want to delete the file before anyone tries to use it.
    // However, this means that a given instance can be closed and re-opened, within the same run, without issue.
    // TODO: fix this - if this method called more than once by same entity, will end up removing the data
    if (!this.shouldPersistAcrossRestarts) {
      file.delete();
    }
    FlatFilePersistentStorage storage = new FlatFilePersistentStorage(file);
    return configuration.getServiceType().cast(storage);
  }

  @Override
  public Collection<Class<?>> getProvidedServiceTypes() {
    return Collections.singleton(IPersistentStorage.class);
  }

  @Override
  public void close() {
    
  }

  @Override
  public void clear() throws ServiceProviderCleanupException {
    // check that either there are no consumers or platform is the only consumer
    Assert.assertTrue((consumers.size() == 0) || (consumers.size() == 1 && consumers.iterator().next() == 0));

    final String CONSUMER_FILE_PAT = "consumer_[0-9]+.dat";

    // remove data files
    for(File file : directory.toFile().listFiles()) {
      if(file.getName().matches(CONSUMER_FILE_PAT) && !file.delete()) {
        throw new ServiceProviderCleanupException("FlatFileStorageServiceProvider clear failed - can't delete " + file.getAbsolutePath());
      }
    }
  }
  
}
