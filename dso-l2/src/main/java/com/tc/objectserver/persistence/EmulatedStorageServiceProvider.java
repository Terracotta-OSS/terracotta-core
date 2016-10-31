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

import org.terracotta.entity.BasicServiceConfiguration;
import org.terracotta.entity.PlatformConfiguration;
import org.terracotta.entity.ServiceProvider;
import org.terracotta.entity.ServiceConfiguration;
import org.terracotta.entity.ServiceProviderCleanupException;
import org.terracotta.persistence.IPersistentStorage;
import org.terracotta.persistence.IPlatformPersistence;

import com.tc.io.TCFile;
import com.tc.io.TCFileImpl;
import com.tc.io.TCRandomFileAccessImpl;
import com.tc.logging.CustomerLogging;
import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.services.TerracottaServiceProviderRegistry;
import com.tc.util.Assert;
import com.tc.util.NonBlockingStartupLock;
import com.tc.util.startuplock.FileNotCreatedException;
import com.tc.util.startuplock.LocationNotCreatedException;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import java.util.HashSet;

import org.terracotta.entity.ServiceProviderConfiguration;


/**
 * Implements an entire IPersistentStorage data store on top of an underlying IPlatformPersistence implementation.
 * NOTE:  This is just temporary until IPersistentStorage can be fully removed.
 */
public class EmulatedStorageServiceProvider implements ServiceProvider {
  private TerracottaServiceProviderRegistry registry;

  @Override
  public boolean initialize(ServiceProviderConfiguration configuration, PlatformConfiguration platformConfiguration) {
    // Currently, this provider is created directly so there is no chance of seeing any other kind of provider.
    // In the future, this may change.
    Assert.assertTrue(configuration instanceof EmulatedStorageConfiguration);
    EmulatedStorageConfiguration emulatedConfiguration = (EmulatedStorageConfiguration)configuration;
    this.registry = emulatedConfiguration.registry;
    Assert.assertNotNull(this.registry);
    return true;
  }

  @Override
  public <T> T getService(long consumerID, ServiceConfiguration<T> configuration) {
    IPlatformPersistence underlyingAbstraction = this.registry.subRegistry(consumerID).getService(new BasicServiceConfiguration<>(IPlatformPersistence.class));
    EmulatedPersistentStorage emulated = new EmulatedPersistentStorage(underlyingAbstraction);
    return configuration.getServiceType().cast(emulated);
  }

  @Override
  public Collection<Class<?>> getProvidedServiceTypes() {
    return Collections.singleton(IPersistentStorage.class);
  }

  @Override
  public void clear() throws ServiceProviderCleanupException {
    // Do nothing - we assume that the underlying service implementation will do this for us.
  }


  public static class EmulatedStorageConfiguration implements ServiceProviderConfiguration {
    public final TerracottaServiceProviderRegistry registry;
    
    public EmulatedStorageConfiguration(TerracottaServiceProviderRegistry registry) {
      this.registry = registry;
    }
    
    @Override
    public Class<? extends ServiceProvider> getServiceProviderType() {
      return EmulatedStorageServiceProvider.class;
    }
  }
}
