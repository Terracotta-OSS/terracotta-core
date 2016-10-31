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
package org.terracotta.passthrough;

import org.terracotta.entity.BasicServiceConfiguration;
import org.terracotta.entity.PlatformConfiguration;
import org.terracotta.entity.ServiceProvider;
import org.terracotta.entity.ServiceConfiguration;
import org.terracotta.entity.ServiceProviderCleanupException;
import org.terracotta.persistence.IPersistentStorage;
import org.terracotta.persistence.IPlatformPersistence;

import java.util.Collection;
import java.util.Set;
import java.util.HashSet;

import org.terracotta.entity.ServiceProviderConfiguration;


/**
 * NOTE:  This is largely based on EmulatedStorageServiceProvider (but substantially simplified), from terracotta-core.
 * XXX: Can be removed once IPersistentStorage is removed.
 */
public class PassthroughEmulatedStorageServiceProvider implements ServiceProvider {
  private RegistryLookup registryLookup;

  @Override
  public boolean initialize(ServiceProviderConfiguration configuration, PlatformConfiguration platformConfiguration) {
    // Currently, this provider is created directly so there is no chance of seeing any other kind of provider.
    // In the future, this may change.
    Assert.assertTrue(configuration instanceof Configuration);
    Configuration emulatedConfiguration = (Configuration)configuration;
    this.registryLookup = emulatedConfiguration.registryLookup;
    Assert.assertTrue(null != this.registryLookup);
    return true;
  }

  @Override
  public <T> T getService(long consumerID, ServiceConfiguration<T> configuration) {
    IPlatformPersistence underlyingAbstraction = this.registryLookup.getRegistryForConsumerID(consumerID).getService(new BasicServiceConfiguration<IPlatformPersistence>(IPlatformPersistence.class));
    PassthroughEmulatedPersistentStorage emulated = new PassthroughEmulatedPersistentStorage(underlyingAbstraction);
    return configuration.getServiceType().cast(emulated);
  }

  @Override
  public Collection<Class<?>> getProvidedServiceTypes() {
    // Using Collections.singleton here complains about trying to unify between
    // different containers of different class
    // bindings so doing it manually satisfies the compiler (seems to work in
    // Java8 but not Java6).
    Set<Class<?>> set = new HashSet<Class<?>>();
    set.add(IPersistentStorage.class);
    return set;
  }

  @Override
  public void clear() throws ServiceProviderCleanupException {
    // Do nothing - we assume that the underlying service implementation will do this for us.
  }


  public static class Configuration implements ServiceProviderConfiguration {
    public final RegistryLookup registryLookup;
    
    public Configuration(RegistryLookup registryLookup) {
      this.registryLookup = registryLookup;
    }
    
    @Override
    public Class<? extends ServiceProvider> getServiceProviderType() {
      return PassthroughEmulatedStorageServiceProvider.class;
    }
  }


  public interface RegistryLookup {
    public PassthroughServiceRegistry getRegistryForConsumerID(long consumerID);
  }
}
