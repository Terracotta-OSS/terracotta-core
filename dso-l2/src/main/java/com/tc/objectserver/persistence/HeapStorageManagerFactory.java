package com.tc.objectserver.persistence;

import org.terracotta.corestorage.ImmutableKeyValueStorageConfig;
import org.terracotta.corestorage.KeyValueStorageConfig;
import org.terracotta.corestorage.StorageManager;
import org.terracotta.corestorage.TransformerLookup;
import org.terracotta.corestorage.heap.HeapStorageManager;

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
  public StorageManager createStorageManager(final Map<String, KeyValueStorageConfig<?, ?>> configMap,
                                             final TransformerLookup transformerLookup) {
    return new HeapStorageManager(configMap);
  }

  @Override
  public <K, V> KeyValueStorageConfig<K, V> wrapObjectDBConfig(final KeyValueStorageConfig<K, V> baseConfig) {
    return baseConfig;
  }

  @Override
  public <K, V> KeyValueStorageConfig<K, V> wrapMapConfig(final KeyValueStorageConfig<K, V> baseConfig) {
    return baseConfig;
  }

  @Override
  public <K, V> KeyValueStorageConfig<K, V> wrapObjectDBConfig(final ImmutableKeyValueStorageConfig.Builder<K, V> builder) {
    return builder.build();
  }

  @Override
  public <K, V> KeyValueStorageConfig<K, V> wrapMapConfig(final ImmutableKeyValueStorageConfig.Builder<K, V> builder) {
    return builder.build();
  }
}
