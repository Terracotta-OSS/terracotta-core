package com.tc.objectserver.persistence;

import org.terracotta.corestorage.ImmutableKeyValueStorageConfig;
import org.terracotta.corestorage.KeyValueStorageConfig;
import org.terracotta.corestorage.StorageManager;
import org.terracotta.corestorage.TransformerLookup;

import java.io.IOException;
import java.util.Map;

/**
 * @author tim
 */
public interface StorageManagerFactory {
  StorageManager createStorageManager(Map<String, KeyValueStorageConfig<?, ?>> configMap, TransformerLookup transformerLookup) throws IOException;

  <K, V> KeyValueStorageConfig<K, V> wrapObjectDBConfig(KeyValueStorageConfig<K, V> baseConfig);

  <K, V> KeyValueStorageConfig<K, V> wrapObjectDBConfig(ImmutableKeyValueStorageConfig.Builder<K, V> builder);

  <K, V> KeyValueStorageConfig<K, V> wrapMapConfig(KeyValueStorageConfig<K, V> baseConfig);

  <K, V> KeyValueStorageConfig<K, V> wrapMapConfig(ImmutableKeyValueStorageConfig.Builder<K, V> builder);
}
