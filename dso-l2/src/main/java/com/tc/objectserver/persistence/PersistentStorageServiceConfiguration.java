package com.tc.objectserver.persistence;

import org.terracotta.entity.ServiceConfiguration;

import org.terracotta.persistence.IPersistentStorage;
import org.terracotta.persistence.KeyValueStorage;
/**
 * The empty configuration for a persistent storage service (there are no configurable implementations of this interface).
 */
public class PersistentStorageServiceConfiguration implements ServiceConfiguration<IPersistentStorage> {
  @Override
  public Class<IPersistentStorage> getServiceType() {
    return IPersistentStorage.class;
  }
}
