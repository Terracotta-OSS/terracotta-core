package com.tc.objectserver.persistence;

import org.terracotta.corestorage.KeyValueStorageConfig;
import org.terracotta.corestorage.StorageManager;
import org.terracotta.corestorage.TransformerLookup;
import org.terracotta.corestorage.heap.HeapStorageManager;

import java.io.IOException;
import java.util.Map;

/**
 * @author tim
 */
public class HeapStorageManagerFactory implements StorageManagerFactory {

  public static final HeapStorageManagerFactory INSTANCE = new HeapStorageManagerFactory();

  private HeapStorageManagerFactory() {
    // Use the singleton instance
  }

  @Override
  public StorageManager createStorageManager(final Map<String, KeyValueStorageConfig<?, ?>> configMap, final TransformerLookup transformerLookup) throws IOException {
    return new HeapStorageManager(configMap);
  }

  @Override
  public StorageManager createMetadataStorageManager(final Map<String, KeyValueStorageConfig<?, ?>> configMap) throws IOException {
    return new HeapStorageManager(configMap);
  }
}
