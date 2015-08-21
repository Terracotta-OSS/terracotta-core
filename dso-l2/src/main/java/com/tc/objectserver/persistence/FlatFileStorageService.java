package com.tc.objectserver.persistence;

import org.terracotta.entity.Service;
import org.terracotta.entity.ServiceConfiguration;
import org.terracotta.persistence.IPersistentStorage;
import org.terracotta.persistence.KeyValueStorage;

import java.nio.file.Path;

public class FlatFileStorageService implements Service<IPersistentStorage> {
  private final FlatFilePersistentStorage storage;
  
  public FlatFileStorageService(Path path) {
    this.storage = new FlatFilePersistentStorage(path.toString());
  }

  @Override
  public void initialize(ServiceConfiguration<? extends IPersistentStorage> configuration) {
    throw new UnsupportedOperationException("sub-services should already be initialized");
  }

  @Override
  public FlatFilePersistentStorage get() {
    return this.storage;
  }

  @Override
  public void destroy() {
    this.storage.close();
  }
}
