package org.terracotta.passthrough;

import org.terracotta.entity.PlatformConfiguration;
import org.terracotta.entity.ServiceConfiguration;
import org.terracotta.entity.ServiceProvider;
import org.terracotta.entity.ServiceProviderConfiguration;
import org.terracotta.entity.ServiceProviderCleanupException;
import org.terracotta.entity.StateDumper;
import org.terracotta.persistence.IPlatformPersistence;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;


/**
 * NOTE: This is loosely a clone of the NullPlatformStorageServiceProvider class in terracotta-core with some unused
 *  functionality stripped out.
 */
public class PassthroughNullPlatformStorageServiceProvider implements ServiceProvider {
  private final Map<Long, PassthroughNullPlatformPersistentStorage> providers = new HashMap<Long, PassthroughNullPlatformPersistentStorage>();

  @Override
  public boolean initialize(ServiceProviderConfiguration serviceProviderConfiguration, PlatformConfiguration platformConfiguration) {
    return true;
  }

  @Override
  public synchronized <T> T getService(long consumerID, ServiceConfiguration<T> serviceConfiguration) {
    PassthroughNullPlatformPersistentStorage storage = this.providers.get(consumerID);
    if (null == storage) {
      storage = new PassthroughNullPlatformPersistentStorage();
      this.providers.put(consumerID, storage);
    }
    return serviceConfiguration.getServiceType().cast(storage);
  }

  @Override
  public Collection<Class<?>> getProvidedServiceTypes() {
    // Using Collections.singleton here complains about trying to unify between
    // different containers of different class
    // bindings so doing it manually satisfies the compiler (seems to work in
    // Java8 but not Java6).
    Set<Class<?>> set = new HashSet<Class<?>>();
    set.add(IPlatformPersistence.class);
    return set;
  }

  public synchronized void close() throws IOException {
    providers.clear();
  }

  @Override
  public synchronized void prepareForSynchronization() throws ServiceProviderCleanupException {
    providers.clear();
  }

  @Override
  public void dumpStateTo(StateDumper stateDumper) {

  }
}
